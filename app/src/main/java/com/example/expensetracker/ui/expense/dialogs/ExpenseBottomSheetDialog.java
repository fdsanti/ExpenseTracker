package com.example.expensetracker.ui.expense.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.expensetracker.R;
import com.example.expensetracker.model.Category;
import com.example.expensetracker.model.Expense;
import com.example.expensetracker.model.Member;
import com.example.expensetracker.ui.expense.ExpenseScreenController;
import com.example.expensetracker.ui.expense.ExpenseScreenState;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class ExpenseBottomSheetDialog extends BottomSheetDialogFragment {

    private ExpenseScreenState state;
    private ExpenseScreenController controller;
    private Expense expense; // null = create

    public static ExpenseBottomSheetDialog newCreateInstance(
            ExpenseScreenState state,
            ExpenseScreenController controller
    ) {
        ExpenseBottomSheetDialog dialog = new ExpenseBottomSheetDialog();
        dialog.state = state;
        dialog.controller = controller;
        dialog.expense = null;
        return dialog;
    }

    public static ExpenseBottomSheetDialog newEditInstance(
            ExpenseScreenState state,
            ExpenseScreenController controller,
            Expense expense
    ) {
        ExpenseBottomSheetDialog dialog = new ExpenseBottomSheetDialog();
        dialog.state = state;
        dialog.controller = controller;
        dialog.expense = expense;
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.bottom_sheet_expense, container, false);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        if (getContext() == null || state == null || controller == null) {
            dismissAllowingStateLoss();
            return;
        }

        boolean isEditMode = expense != null;
        android.widget.TextView txtExpenseSheetTitle = view.findViewById(R.id.txtExpenseSheetTitle);
        txtExpenseSheetTitle.setText(isEditMode ? "Editar gasto" : "Nuevo gasto");

        TextInputEditText txtNombre = view.findViewById(R.id.editText_NombreGasto);
        TextInputEditText txtFecha = view.findViewById(R.id.editText_FechaGasto);
        TextInputEditText txtMonto = view.findViewById(R.id.editText_Gasto);

        AutoCompleteTextView dropdownPersona = view.findViewById(R.id.dropdown_nombres);
        AutoCompleteTextView dropdownCategoria = view.findViewById(R.id.cat_dropdown);

        TextInputLayout inputLayoutWho = view.findViewById(R.id.inputLayout_Who);
        TextInputLayout inputLayoutNombre = view.findViewById(R.id.inputLayout_NombreGasto);
        TextInputLayout inputLayoutFecha = view.findViewById(R.id.inputLayout_FechaGasto);
        TextInputLayout inputLayoutCategoria = view.findViewById(R.id.menu_category);
        TextInputLayout inputLayoutMonto = view.findViewById(R.id.inputLayout_Gasto);

        final long[] selectedDate = {isEditMode ? expense.getDate() : todayAtLocalMidnight()};

        txtNombre.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                clearError(inputLayoutNombre);
            }
        });

        txtMonto.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                clearError(inputLayoutMonto);
            }
        });

        dropdownPersona.setOnItemClickListener((parent, itemView, position, id) -> {
            clearError(inputLayoutWho);
            expandBottomSheet();
        });
        dropdownPersona.setOnClickListener(v -> {
            clearError(inputLayoutWho);
            expandBottomSheet();
        });

        dropdownCategoria.setOnItemClickListener((parent, itemView, position, id) -> {
            clearError(inputLayoutCategoria);
            expandBottomSheet();
        });
        dropdownCategoria.setOnClickListener(v -> {
            clearError(inputLayoutCategoria);
            expandBottomSheet();
        });

        txtFecha.setOnClickListener(v -> {
            clearError(inputLayoutFecha);
            expandBottomSheet();

            MaterialDatePicker<Long> picker =
                    MaterialDatePicker.Builder.datePicker()
                            .setTitleText("Seleccionar fecha")
                            .setSelection(toUtc(selectedDate[0]))
                            .build();

            picker.addOnPositiveButtonClickListener(selection -> {
                selectedDate[0] = fromUtc(selection);
                txtFecha.setText(formatDate(selectedDate[0]));
                clearError(inputLayoutFecha);
            });

            picker.show(getParentFragmentManager(), "DATE_PICKER");
        });

        dropdownPersona.setAdapter(new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                mapMemberNames(state.members)
        ));

        List<Category> categories = state.categories;
        if (categories == null || categories.isEmpty()) {
            categories = getDefaultCategories();
        }

        dropdownCategoria.setAdapter(new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                mapCategoryNames(categories)
        ));

        if (isEditMode) {
            txtNombre.setText(expense.getDescription());
            txtMonto.setText(String.valueOf(expense.getAmount()));
            dropdownPersona.setText(resolveMemberName(expense.getPaidByMemberId(), state.members), false);
            dropdownCategoria.setText(resolveCategoryName(expense.getCategoryId(), categories), false);
        }

        txtFecha.setText(formatDate(selectedDate[0]));
        txtFecha.setFocusable(false);
        expandOnFocus(txtNombre, txtMonto, dropdownPersona, dropdownCategoria);
    }

    @Override
    public void onStart() {
        super.onStart();

        View view = getView();
        if (view == null) {
            return;
        }

        boolean isEditMode = expense != null;
        configureBottomSheetForKeyboard();

        TextInputEditText txtNombre = view.findViewById(R.id.editText_NombreGasto);
        TextInputEditText txtFecha = view.findViewById(R.id.editText_FechaGasto);
        TextInputEditText txtMonto = view.findViewById(R.id.editText_Gasto);

        AutoCompleteTextView dropdownPersona = view.findViewById(R.id.dropdown_nombres);
        AutoCompleteTextView dropdownCategoria = view.findViewById(R.id.cat_dropdown);

        TextInputLayout inputLayoutWho = view.findViewById(R.id.inputLayout_Who);
        TextInputLayout inputLayoutNombre = view.findViewById(R.id.inputLayout_NombreGasto);
        TextInputLayout inputLayoutFecha = view.findViewById(R.id.inputLayout_FechaGasto);
        TextInputLayout inputLayoutCategoria = view.findViewById(R.id.menu_category);
        TextInputLayout inputLayoutMonto = view.findViewById(R.id.inputLayout_Gasto);

        final long[] selectedDate = {isEditMode ? expense.getDate() : todayAtLocalMidnight()};

        txtFecha.setText(formatDate(selectedDate[0]));
        txtFecha.setOnClickListener(v -> {
            clearError(inputLayoutFecha);
            expandBottomSheet();

            MaterialDatePicker<Long> picker =
                    MaterialDatePicker.Builder.datePicker()
                            .setTitleText("Seleccionar fecha")
                            .setSelection(toUtc(selectedDate[0]))
                            .build();

            picker.addOnPositiveButtonClickListener(selection -> {
                selectedDate[0] = fromUtc(selection);
                txtFecha.setText(formatDate(selectedDate[0]));
                clearError(inputLayoutFecha);
            });

            picker.show(getParentFragmentManager(), "DATE_PICKER");
        });

        View btnSave = view.findViewById(R.id.btnCreateExpense);
        View btnDelete = view.findViewById(R.id.btnDeleteExpense);
        expandOnFocus(txtNombre, txtMonto, dropdownPersona, dropdownCategoria);

        if (btnSave != null) {
            if (btnSave instanceof android.widget.TextView) {
                ((android.widget.TextView) btnSave).setText(isEditMode ? "Guardar expense" : "Crear expense");
            }

            btnSave.setOnClickListener(v -> {
                String description = safe(txtNombre);
                String amountText = safe(txtMonto);
                String memberName = dropdownPersona.getText() != null
                        ? dropdownPersona.getText().toString().trim()
                        : "";
                String categoryName = dropdownCategoria.getText() != null
                        ? dropdownCategoria.getText().toString().trim()
                        : "";

                boolean hasError = false;

                clearError(inputLayoutWho);
                clearError(inputLayoutNombre);
                clearError(inputLayoutFecha);
                clearError(inputLayoutCategoria);
                clearError(inputLayoutMonto);

                if (description.isEmpty()) {
                    inputLayoutNombre.setError("Ingresá un nombre");
                    hasError = true;
                }

                if (amountText.isEmpty()) {
                    inputLayoutMonto.setError("Ingresá un monto");
                    hasError = true;
                }

                double amount = 0;
                if (!amountText.isEmpty()) {
                    try {
                        amount = Double.parseDouble(amountText);
                    } catch (Exception e) {
                        inputLayoutMonto.setError("Monto inválido");
                        hasError = true;
                    }
                }

                if (memberName.isEmpty()) {
                    inputLayoutWho.setError("Seleccioná una persona");
                    hasError = true;
                }

                if (categoryName.isEmpty()) {
                    inputLayoutCategoria.setError("Seleccioná una categoría");
                    hasError = true;
                }

                if (selectedDate[0] <= 0) {
                    inputLayoutFecha.setError("Seleccioná una fecha");
                    hasError = true;
                }

                if (hasError) {
                    return;
                }

                String memberId = findMemberId(memberName, state.members);

                List<Category> availableCategories = state.categories;
                if (availableCategories == null || availableCategories.isEmpty()) {
                    availableCategories = getDefaultCategories();
                }

                String categoryId = findCategoryId(categoryName, availableCategories);

                if (memberId == null) {
                    inputLayoutWho.setError("Persona inválida");
                    return;
                }

                if (categoryId == null) {
                    inputLayoutCategoria.setError("Categoría inválida");
                    return;
                }

                if (isEditMode) {
                    controller.updateExpense(
                            expense.getId(),
                            description,
                            amount,
                            memberId,
                            categoryId,
                            selectedDate[0]
                    );
                    showSnackbar("Los cambios se guardaron correctamente");
                } else {
                    controller.createExpense(
                            description,
                            amount,
                            memberId,
                            categoryId,
                            selectedDate[0]
                    );
                    showSnackbar("El gasto se agregó correctamente");
                }

                dismissAllowingStateLoss();
            });
        }

        if (btnDelete != null) {
            btnDelete.setVisibility(isEditMode ? View.VISIBLE : View.GONE);

            if (isEditMode) {
                btnDelete.setOnClickListener(v -> {
                    if (getContext() == null) {
                        return;
                    }

                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Eliminar expense")
                            .setMessage("¿Querés eliminar este gasto?")
                            .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                            .setPositiveButton("Eliminar", (dialog, which) -> {
                                controller.deleteExpense(expense.getId());
                                showSnackbar("El gasto se eliminó del tracker");
                                dismissAllowingStateLoss();
                            })
                            .show();
                });
            }
        }
    }

    private static List<String> mapMemberNames(List<Member> members) {
        List<String> list = new ArrayList<>();
        if (members == null) {
            return list;
        }

        for (Member m : members) {
            if (m != null) {
                list.add(m.getName());
            }
        }
        return list;
    }

    private static List<String> mapCategoryNames(List<Category> categories) {
        List<String> list = new ArrayList<>();
        if (categories == null) {
            return list;
        }

        for (Category c : categories) {
            if (c != null) {
                list.add(c.getName());
            }
        }
        return list;
    }

    private static String resolveMemberName(String id, List<Member> members) {
        if (members == null) {
            return "";
        }

        for (Member m : members) {
            if (m != null && m.getId().equals(id)) {
                return m.getName();
            }
        }
        return "";
    }

    private static String resolveCategoryName(String id, List<Category> categories) {
        if (categories == null) {
            return "";
        }

        for (Category c : categories) {
            if (c != null && c.getId().equals(id)) {
                return c.getName();
            }
        }
        return "";
    }

    private static String findMemberId(String name, List<Member> members) {
        if (members == null) {
            return null;
        }

        for (Member m : members) {
            if (m != null && m.getName().equals(name)) {
                return m.getId();
            }
        }
        return null;
    }

    private static String findCategoryId(String name, List<Category> categories) {
        if (categories == null) {
            return null;
        }

        for (Category c : categories) {
            if (c != null && c.getName().equals(name)) {
                return c.getId();
            }
        }
        return null;
    }

    private static String safe(TextInputEditText t) {
        return t.getText() != null ? t.getText().toString().trim() : "";
    }

    private static String formatDate(long ts) {
        return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                .format(new Date(ts));
    }

    private static long todayAtLocalMidnight() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private static long toUtc(long local) {
        Calendar l = Calendar.getInstance();
        l.setTimeInMillis(local);

        Calendar u = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        u.clear();
        u.set(
                l.get(Calendar.YEAR),
                l.get(Calendar.MONTH),
                l.get(Calendar.DAY_OF_MONTH),
                0,
                0,
                0
        );
        return u.getTimeInMillis();
    }

    private static long fromUtc(long utc) {
        Calendar u = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        u.setTimeInMillis(utc);

        Calendar l = Calendar.getInstance();
        l.clear();
        l.set(
                u.get(Calendar.YEAR),
                u.get(Calendar.MONTH),
                u.get(Calendar.DAY_OF_MONTH),
                0,
                0,
                0
        );
        return l.getTimeInMillis();
    }

    private static void clearError(TextInputLayout inputLayout) {
        inputLayout.setError(null);
        inputLayout.setErrorEnabled(false);
    }

    private void configureBottomSheetForKeyboard() {
        Dialog dialog = getDialog();
        if (dialog == null) {
            return;
        }

        Window window = dialog.getWindow();
        if (window != null) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        expandBottomSheet();
    }

    private void expandOnFocus(View... views) {
        if (views == null) {
            return;
        }

        for (View target : views) {
            if (target == null) {
                continue;
            }

            target.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    expandBottomSheet();
                }
            });
        }
    }

    private void expandBottomSheet() {
        Dialog dialog = getDialog();
        if (!(dialog instanceof BottomSheetDialog)) {
            return;
        }

        FrameLayout bottomSheet = ((BottomSheetDialog) dialog)
                .findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet == null) {
            return;
        }

        BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
        behavior.setSkipCollapsed(true);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    private void showSnackbar(String message) {
        if (getActivity() == null) {
            return;
        }

        View root = getActivity().findViewById(android.R.id.content);
        if (root == null) {
            return;
        }

        Snackbar.make(root, message, Snackbar.LENGTH_SHORT).show();
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    }

    private static List<Category> getDefaultCategories() {
        List<Category> list = new ArrayList<>();

        list.add(new Category("supermercado", "Supermercado", true, 0));
        list.add(new Category("delivery", "Delivery", true, 1));
        list.add(new Category("nafta_peajes", "Nafta / Peajes", true, 2));
        list.add(new Category("salidas", "Salidas", true, 3));
        list.add(new Category("servicios", "Servicios", true, 4));
        list.add(new Category("suscripciones", "Suscripciones", true, 5));
        list.add(new Category("auto", "Auto", true, 6));
        list.add(new Category("pago_casa", "Pago Casa", true, 7));
        list.add(new Category("compras", "Compras", true, 8));
        list.add(new Category("gatitas", "Gatitas", true, 9));
        list.add(new Category("otros", "Otros", true, 10));

        return list;
    }
}
