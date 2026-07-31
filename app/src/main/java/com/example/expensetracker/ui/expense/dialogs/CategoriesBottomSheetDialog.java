package com.example.expensetracker.ui.expense.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensetracker.R;
import com.example.expensetracker.data.DefaultCategories;
import com.example.expensetracker.data.TrackerRepository.RepositoryCallback;
import com.example.expensetracker.model.Category;
import com.example.expensetracker.ui.common.AppDialog;
import com.example.expensetracker.ui.common.AppSnackbar;
import com.example.expensetracker.ui.expense.ExpenseScreenController;
import com.example.expensetracker.ui.expense.ExpenseScreenState;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

public class CategoriesBottomSheetDialog extends BottomSheetDialogFragment {

    private ExpenseScreenState state;
    private ExpenseScreenController controller;
    private CategoryEditorAdapter adapter;
    private ItemTouchHelper itemTouchHelper;
    private View activeSnackbarView;
    private Runnable activeSnackbarDismissRunnable;

    public static CategoriesBottomSheetDialog newInstance(
            ExpenseScreenState state,
            ExpenseScreenController controller
    ) {
        CategoriesBottomSheetDialog dialog = new CategoriesBottomSheetDialog();
        dialog.state = state;
        dialog.controller = controller;
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.bottom_sheet_categories, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (state == null || controller == null || getContext() == null) {
            dismissAllowingStateLoss();
            return;
        }

        RecyclerView recyclerView = view.findViewById(R.id.categoriesRecycler);
        View btnAddCategory = view.findViewById(R.id.btnAddCategory);

        adapter = new CategoryEditorAdapter(getInitialCategories(), new CategoryEditorAdapter.Listener() {
            @Override
            public void onEditCategory(Category category) {
                showCategoryNameDialog(category);
            }

            @Override
            public void onDeleteCategory(Category category) {
                confirmDeleteCategory(category);
            }

            @Override
            public void onStartDrag(CategoryEditorAdapter.ViewHolder viewHolder) {
                itemTouchHelper.startDrag(viewHolder);
            }

            @Override
            public void onOrderChanged(List<Category> categories) {
                saveCategoryOrder(categories);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN,
                0
        ) {
            @Override
            public boolean onMove(
                    @NonNull RecyclerView recyclerView,
                    @NonNull RecyclerView.ViewHolder source,
                    @NonNull RecyclerView.ViewHolder target
            ) {
                return adapter.moveItem(source.getAdapterPosition(), target.getAdapterPosition());
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            }

            @Override
            public void clearView(
                    @NonNull RecyclerView recyclerView,
                    @NonNull RecyclerView.ViewHolder viewHolder
            ) {
                super.clearView(recyclerView, viewHolder);
                adapter.notifyOrderChanged();
            }

            @Override
            public boolean isLongPressDragEnabled() {
                return false;
            }
        });
        itemTouchHelper.attachToRecyclerView(recyclerView);

        btnAddCategory.setOnClickListener(v -> showCategoryNameDialog(null));
    }

    @Override
    public void onStart() {
        super.onStart();
        configureBottomSheet();
    }

    @Override
    public void onDestroyView() {
        clearActiveSnackbar();
        super.onDestroyView();
    }

    private List<Category> getInitialCategories() {
        if (state.categories == null || state.categories.isEmpty()) {
            return DefaultCategories.asList();
        }

        return new ArrayList<>(state.categories);
    }

    private void showCategoryNameDialog(@Nullable Category category) {
        AppDialog.showTextInput(
                requireContext(),
                category != null ? "Editar categor\u00eda" : "Agregar categor\u00eda",
                category != null ? category.getName() : "",
                "Nombre categor\u00eda",
                "Guardar",
                "Ingres\u00e1 un nombre",
                null,
                name -> {
                    if (category != null) {
                        updateCategoryName(category, name);
                    } else {
                        createCategory(name);
                    }
                }
        );
    }

    private void createCategory(String name) {
        controller.createCategory(name, new RepositoryCallback<Category>() {
            @Override
            public void onSuccess(Category result) {
                adapter.addCategory(result);
                showSnackbar("Categor\u00eda agregada");
            }

            @Override
            public void onError(Exception exception) {
                showToast("No se pudo agregar la categor\u00eda");
            }
        });
    }

    private void updateCategoryName(Category category, String name) {
        controller.updateCategoryName(category.getId(), name, new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                adapter.updateCategoryName(category.getId(), name);
                showSnackbar("Categor\u00eda actualizada");
            }

            @Override
            public void onError(Exception exception) {
                showToast("No se pudo editar la categor\u00eda");
            }
        });
    }

    private void confirmDeleteCategory(Category category) {
        if (DefaultCategories.OTHERS_ID.equals(category.getId())) {
            showToast("Otros no se puede eliminar");
            return;
        }

        AppDialog.showConfirmation(
                requireContext(),
                "Eliminar categor\u00eda",
                "Los gastos de esta categor\u00eda pasar\u00e1n a Otros.",
                "Eliminar",
                AppDialog.ActionStyle.DESTRUCTIVE,
                () -> deleteCategory(category)
        );
    }

    private void deleteCategory(Category category) {
        int remainingCount = Math.max(adapter.getCategories().size() - 1, 0);

        controller.deleteCategory(category.getId(), remainingCount, new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                adapter.removeCategory(category.getId());
                saveCategoryOrder(adapter.getCategories());
                showSnackbar("Categor\u00eda eliminada");
            }

            @Override
            public void onError(Exception exception) {
                showToast("No se pudo eliminar la categor\u00eda");
            }
        });
    }

    private void saveCategoryOrder(List<Category> categories) {
        controller.reorderCategories(categories, new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
            }

            @Override
            public void onError(Exception exception) {
                showToast("No se pudo guardar el orden");
            }
        });
    }

    private void configureBottomSheet() {
        Dialog dialog = getDialog();

        if (!(dialog instanceof BottomSheetDialog)) {
            return;
        }

        FrameLayout bottomSheet = ((BottomSheetDialog) dialog)
                .findViewById(com.google.android.material.R.id.design_bottom_sheet);

        if (bottomSheet == null) {
            return;
        }

        bottomSheet.setBackgroundColor(getAttrColor(R.attr.categoryEditorSheetBg));

        BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        behavior.setSkipCollapsed(true);

        ViewGroup.LayoutParams params = bottomSheet.getLayoutParams();
        params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        bottomSheet.setLayoutParams(params);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
    }

    private void showSnackbar(String message) {
        Context context = getContext();
        FrameLayout bottomSheet = getBottomSheetContainer();
        if (context == null || bottomSheet == null) {
            return;
        }

        clearActiveSnackbar();

        int elevation = dpToPx(context, 6);
        int bottomMargin = dpToPx(context, 24);
        int verticalOffset = dpToPx(context, 18);

        FrameLayout snackbarView = new FrameLayout(context);
        snackbarView.setBackgroundResource(R.drawable.bg_expense_snackbar);
        snackbarView.setPadding(0, 0, 0, 0);
        snackbarView.setElevation(elevation);

        snackbarView.addView(createSnackbarContent(context, message));

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        params.setMargins(0, 0, 0, bottomMargin);

        activeSnackbarView = snackbarView;
        bottomSheet.addView(snackbarView, params);
        snackbarView.bringToFront();
        snackbarView.setAlpha(0f);
        snackbarView.setTranslationY(verticalOffset);
        snackbarView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(180)
                .start();

        activeSnackbarDismissRunnable = () -> {
            if (activeSnackbarView == snackbarView && snackbarView.getParent() == bottomSheet) {
                snackbarView.animate()
                        .alpha(0f)
                        .translationY(verticalOffset)
                        .setDuration(160)
                        .withEndAction(() -> {
                            if (activeSnackbarView == snackbarView && snackbarView.getParent() == bottomSheet) {
                                bottomSheet.removeView(snackbarView);
                                activeSnackbarView = null;
                                activeSnackbarDismissRunnable = null;
                            }
                        })
                        .start();
            }
        };
        snackbarView.postDelayed(activeSnackbarDismissRunnable, 2200);
    }

    private View createSnackbarContent(Context context, String message) {
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.HORIZONTAL);
        content.setGravity(Gravity.CENTER_VERTICAL);
        content.setPadding(dpToPx(context, 18), dpToPx(context, 14), dpToPx(context, 20), dpToPx(context, 14));

        ImageView icon = new ImageView(context);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dpToPx(context, 20), dpToPx(context, 20));
        icon.setLayoutParams(iconParams);
        icon.setImageResource(R.drawable.checkcircle);
        icon.setColorFilter(getAttrColor(context, R.attr.expenseSnackbarContentColor));
        content.addView(icon);

        TextView text = new TextView(context);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        textParams.setMarginStart(dpToPx(context, 12));
        text.setLayoutParams(textParams);
        text.setIncludeFontPadding(false);
        text.setText(message);
        text.setTextColor(getAttrColor(context, R.attr.expenseSnackbarContentColor));
        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        text.setTypeface(ResourcesCompat.getFont(context, R.font.rajdhani_semibold));
        content.addView(text);

        return content;
    }

    private int getAttrColor(int attr) {
        Context context = requireContext();
        return getAttrColor(context, attr);
    }

    private int getAttrColor(Context context, int attr) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }

    @Nullable
    private FrameLayout getBottomSheetContainer() {
        Dialog dialog = getDialog();

        if (!(dialog instanceof BottomSheetDialog)) {
            return null;
        }

        return ((BottomSheetDialog) dialog)
                .findViewById(com.google.android.material.R.id.design_bottom_sheet);
    }

    private int dpToPx(Context context, int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }

    private void clearActiveSnackbar() {
        if (activeSnackbarView == null) {
            activeSnackbarDismissRunnable = null;
            return;
        }

        activeSnackbarView.animate().cancel();
        if (activeSnackbarDismissRunnable != null) {
            activeSnackbarView.removeCallbacks(activeSnackbarDismissRunnable);
        }

        ViewGroup parent = (ViewGroup) activeSnackbarView.getParent();
        if (parent != null) {
            parent.removeView(activeSnackbarView);
        }

        activeSnackbarView = null;
        activeSnackbarDismissRunnable = null;
    }

    private void showToast(String message) {
        Context context = getContext();
        if (context != null) {
            AppSnackbar.show(context, message);
        }
    }
}
