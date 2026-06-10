package com.example.expensetracker.ui.expense.components;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.FrameLayout;

import android.graphics.drawable.ColorDrawable;
import android.view.ViewGroup;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import com.example.expensetracker.Category;
import com.example.expensetracker.R;
import com.example.expensetracker.calculator.CategoryExpenseItem;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import android.graphics.Color;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;
import com.github.mikephil.charting.utils.MPPointF;
import android.view.Gravity;

import android.util.Log;

public class ContentCardView extends LinearLayout {

    public interface OnCategoryClickListener {
        void onCategoryClick(@NonNull String categoryId);
    }

    public interface OnExpenseClickListener {
        void onExpenseClick(@NonNull String expenseId);
    }

    public interface OnMemberFilterChangeListener {
        void onMemberFilterChanged(@Nullable String memberId);
    }

    public interface OnTypeFilterChangeListener {
        void onTypeFilterChanged(@Nullable String typeFilterValue);
    }

    public interface OnSortTypeChangeListener {
        void onSortTypeChanged(@NonNull String sortTypeValue);
    }

    private interface FilterSelectionCallback {
        void onSelected(@Nullable String value);
    }

    public enum Tab {
        CATEGORIES,
        EXPENSES
    }

    public static class FilterOptionUi {
        public final String id;
        public final String label;

        public FilterOptionUi(@Nullable String id, @NonNull String label) {
            this.id = id;
            this.label = label;
        }
    }

    public static class SortOptionUi {
        public final String value;
        public final String label;

        public SortOptionUi(@NonNull String value, @NonNull String label) {
            this.value = value;
            this.label = label;
        }
    }

    public static class Model {
        public final Tab selectedTab;
        public final List<CategoryItemUi> categories;
        public final List<ExpenseItemUi> expenses;
        public final boolean isEmpty;
        public final List<FilterOptionUi> memberFilterOptions;
        public final String selectedMemberFilterId;
        public final List<FilterOptionUi> typeFilterOptions;
        public final String selectedTypeFilterValue;
        public final List<SortOptionUi> sortOptions;
        public final String selectedSortTypeValue;

        public Model(
                @NonNull Tab selectedTab,
                @NonNull List<CategoryItemUi> categories,
                @NonNull List<ExpenseItemUi> expenses,
                boolean isEmpty,
                @NonNull List<FilterOptionUi> memberFilterOptions,
                @Nullable String selectedMemberFilterId,
                @NonNull List<FilterOptionUi> typeFilterOptions,
                @Nullable String selectedTypeFilterValue,
                @NonNull List<SortOptionUi> sortOptions,
                @Nullable String selectedSortTypeValue
        ) {
            this.selectedTab = selectedTab;
            this.categories = categories;
            this.expenses = expenses;
            this.isEmpty = isEmpty;
            this.memberFilterOptions = memberFilterOptions;
            this.selectedMemberFilterId = selectedMemberFilterId;
            this.typeFilterOptions = typeFilterOptions;
            this.selectedTypeFilterValue = selectedTypeFilterValue;
            this.sortOptions = sortOptions;
            this.selectedSortTypeValue = selectedSortTypeValue;
        }

        @NonNull
        public static Model empty() {
            return new Model(
                    Tab.CATEGORIES,
                    new ArrayList<>(),
                    new ArrayList<>(),
                    true,
                    new ArrayList<>(),
                    null,
                    new ArrayList<>(),
                    null,
                    new ArrayList<>(),
                    null
            );
        }
    }

    public static class CategoryItemUi {
        public final String categoryId;
        public final String categoryName;
        public final double amount;
        public final boolean expanded;
        public final List<CategoryExpenseItem> expenses;

        public CategoryItemUi(
                @NonNull String categoryId,
                @NonNull String categoryName,
                double amount,
                boolean expanded,
                @NonNull List<CategoryExpenseItem> expenses
        ) {
            this.categoryId = categoryId;
            this.categoryName = categoryName;
            this.amount = amount;
            this.expanded = expanded;
            this.expenses = expenses;
        }
    }

    public static class ExpenseItemUi {
        public final String id;
        public final String title;
        public final String date;
        public final String memberName;
        public final double amount;
        public final String categoryName;
        public final boolean individual;

        public ExpenseItemUi(
                @NonNull String id,
                @NonNull String title,
                @NonNull String date,
                @NonNull String memberName,
                double amount,
                @NonNull String categoryName,
                boolean individual
        ) {
            this.id = id;
            this.title = title;
            this.date = date;
            this.memberName = memberName;
            this.amount = amount;
            this.categoryName = categoryName;
            this.individual = individual;
        }
    }

    private TextView btnTabCategories;
    private TextView btnTabExpenses;
    private LinearLayout filtersContainer;
    private LinearLayout btnFilterUser;
    private LinearLayout btnFilterType;
    private TextView txtFilterUser;
    private TextView txtFilterType;
    private ImageView ivFilterUserChevron;
    private ImageView ivFilterTypeChevron;
    private ImageView btnSort;
    private TextView tvEmptyState;
    private PieChart pieChartCategories;
    private LinearLayout categoriesContainer;
    private LinearLayout expensesContainer;

    private OnCategoryClickListener onCategoryClickListener;
    private OnExpenseClickListener onExpenseClickListener;
    private OnMemberFilterChangeListener onMemberFilterChangeListener;
    private OnTypeFilterChangeListener onTypeFilterChangeListener;
    private OnSortTypeChangeListener onSortTypeChangeListener;

    private List<FilterOptionUi> currentMemberFilterOptions = new ArrayList<>();
    private List<FilterOptionUi> currentTypeFilterOptions = new ArrayList<>();
    private List<SortOptionUi> currentSortOptions = new ArrayList<>();
    private final Set<String> renderedExpandedCategoryIds = new HashSet<>();
    @Nullable
    private String lastPieChartSignature;
    private List<CategoryItemUi> lastPieChartItems = new ArrayList<>();
    @Nullable
    private ValueAnimator pieChartValueAnimator;
    @Nullable
    private String lastRenderedExpensesSignature;
    @Nullable
    private String pendingExpensePreloadSignature;
    @Nullable
    private String currentSelectedMemberFilterId;
    @Nullable
    private String currentSelectedTypeFilterValue;
    @Nullable
    private String currentSelectedSortTypeValue;

    public ContentCardView(Context context) {
        super(context);
        init(context);
    }

    public ContentCardView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ContentCardView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        inflate(context, R.layout.view_content_card, this);

        btnTabCategories = findViewById(R.id.btnTabCategories);
        btnTabExpenses = findViewById(R.id.btnTabExpenses);
        filtersContainer = findViewById(R.id.filtersContainer);
        btnFilterUser = findViewById(R.id.btnFilterUser);
        btnFilterType = findViewById(R.id.btnFilterType);
        txtFilterUser = findViewById(R.id.txtFilterUser);
        txtFilterType = findViewById(R.id.txtFilterType);
        ivFilterUserChevron = findViewById(R.id.ivFilterUserChevron);
        ivFilterTypeChevron = findViewById(R.id.ivFilterTypeChevron);
        btnSort = findViewById(R.id.btnSort);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        pieChartCategories = findViewById(R.id.pieChartCategories);
        setupPieChart();
        categoriesContainer = findViewById(R.id.categoriesContainer);
        expensesContainer = findViewById(R.id.expensesContainer);

        btnSort.setOnClickListener(v -> showSortMenu());
        btnFilterUser.setOnClickListener(v -> handleFilterClick(
                btnFilterUser,
                currentMemberFilterOptions,
                currentSelectedMemberFilterId,
                value -> {
                    if (onMemberFilterChangeListener != null) {
                        onMemberFilterChangeListener.onMemberFilterChanged(value);
                    }
                }
        ));
        btnFilterType.setOnClickListener(v -> handleFilterClick(
                btnFilterType,
                currentTypeFilterOptions,
                currentSelectedTypeFilterValue,
                value -> {
                    if (onTypeFilterChangeListener != null) {
                        onTypeFilterChangeListener.onTypeFilterChanged(value);
                    }
                }
        ));
    }

    public void setOnCategoriesClickListener(OnClickListener listener) {
        btnTabCategories.setOnClickListener(listener);
    }

    public void setOnExpensesClickListener(OnClickListener listener) {
        btnTabExpenses.setOnClickListener(listener);
    }

    public void setOnCategoryClickListener(@Nullable OnCategoryClickListener listener) {
        this.onCategoryClickListener = listener;
    }

    public void setOnExpenseClickListener(@Nullable OnExpenseClickListener listener) {
        this.onExpenseClickListener = listener;
    }

    public void setOnMemberFilterChangeListener(@Nullable OnMemberFilterChangeListener listener) {
        this.onMemberFilterChangeListener = listener;
    }

    public void setOnTypeFilterChangeListener(@Nullable OnTypeFilterChangeListener listener) {
        this.onTypeFilterChangeListener = listener;
    }

    public void setOnSortTypeChangeListener(@Nullable OnSortTypeChangeListener listener) {
        this.onSortTypeChangeListener = listener;
    }

    public void render(@NonNull Model model) {
        renderTabs(model.selectedTab);
        renderFilters(
                model.selectedTab,
                model.memberFilterOptions,
                model.selectedMemberFilterId,
                model.typeFilterOptions,
                model.selectedTypeFilterValue,
                model.sortOptions,
                model.selectedSortTypeValue
        );
        renderEmptyState(model.isEmpty);
        renderPieChart(model.categories, model.selectedTab, model.isEmpty);
        renderCategories(model.categories, model.selectedTab, model.isEmpty);
        renderExpenses(model.expenses, model.selectedTab, model.isEmpty);
    }

    private void renderTabs(@NonNull Tab selectedTab) {
        boolean isCategories = selectedTab == Tab.CATEGORIES;

        updateTabStyle(btnTabCategories, isCategories);
        updateTabStyle(btnTabExpenses, !isCategories);
    }

    private void renderFilters(
            @NonNull Tab selectedTab,
            @NonNull List<FilterOptionUi> memberFilterOptions,
            @Nullable String selectedMemberFilterId,
            @NonNull List<FilterOptionUi> typeFilterOptions,
            @Nullable String selectedTypeFilterValue,
            @NonNull List<SortOptionUi> sortOptions,
            @Nullable String selectedSortTypeValue
    ) {
        if (selectedTab != Tab.EXPENSES && selectedTab != Tab.CATEGORIES) {
            filtersContainer.setVisibility(View.GONE);
            return;
        }

        filtersContainer.setVisibility(View.VISIBLE);

        if (selectedTab == Tab.CATEGORIES) {
            btnSort.setAlpha(0f);
            btnSort.setEnabled(false);
            btnSort.setClickable(false);
            btnSort.setFocusable(false);
        } else {
            btnSort.setAlpha(1f);
            btnSort.setEnabled(true);
            btnSort.setClickable(true);
            btnSort.setFocusable(true);
        }

        currentMemberFilterOptions = new ArrayList<>(memberFilterOptions);
        currentTypeFilterOptions = new ArrayList<>(typeFilterOptions);
        currentSortOptions = new ArrayList<>(sortOptions);
        currentSelectedMemberFilterId = selectedMemberFilterId;
        currentSelectedTypeFilterValue = selectedTypeFilterValue;
        currentSelectedSortTypeValue = selectedSortTypeValue;

        renderFilterDropdown(
                btnFilterUser,
                txtFilterUser,
                ivFilterUserChevron,
                selectedMemberFilterId == null ? "Usuario" : findFilterLabel(memberFilterOptions, selectedMemberFilterId),
                selectedMemberFilterId != null
        );
        renderFilterDropdown(
                btnFilterType,
                txtFilterType,
                ivFilterTypeChevron,
                selectedTypeFilterValue == null ? "Tipo" : findFilterLabel(typeFilterOptions, selectedTypeFilterValue),
                selectedTypeFilterValue != null
        );
        updateSortButtonContentDescription(sortOptions, selectedSortTypeValue);
    }

    private void renderFilterDropdown(
            @NonNull LinearLayout container,
            @NonNull TextView labelView,
            @NonNull ImageView chevronView,
            @NonNull String label,
            boolean isActive
    ) {
        container.setBackgroundResource(isActive
                ? R.drawable.bg_filter_dropdown_active
                : R.drawable.bg_filter_dropdown_inactive);
        labelView.setText(label);
        int color = getAttrColor(isActive
                ? R.attr.filterChipActiveText
                : R.attr.filterChipInactiveText);
        labelView.setTextColor(color);
        Typeface typeface = ResourcesCompat.getFont(
                getContext(),
                isActive ? R.font.rajdhani_semibold : R.font.rajdhani_medium
        );
        labelView.setTypeface(typeface != null ? typeface : labelView.getTypeface());
        if (isActive) {
            chevronView.setImageResource(R.drawable.ic_close_simple);
            chevronView.setBackgroundResource(R.drawable.bg_filter_clear_icon);
            chevronView.setPadding(dpToPx(5), dpToPx(5), dpToPx(5), dpToPx(5));
            chevronView.setColorFilter(getAttrColor(R.attr.filterChipClearIcon), PorterDuff.Mode.SRC_IN);
        } else {
            chevronView.setImageResource(R.drawable.chevron_down);
            chevronView.setBackground(null);
            chevronView.setPadding(0, 0, 0, 0);
            chevronView.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        }
    }

    private void handleFilterClick(
            @NonNull View anchor,
            @NonNull List<FilterOptionUi> options,
            @Nullable String selectedValue,
            @NonNull FilterSelectionCallback callback
    ) {
        if (selectedValue != null) {
            callback.onSelected(null);
            return;
        }

        showFilterMenu(anchor, options, null, callback);
    }

    private String findFilterLabel(
            @NonNull List<FilterOptionUi> options,
            @Nullable String selectedValue
    ) {
        for (FilterOptionUi option : options) {
            if (equalsNullable(option.id, selectedValue)) {
                return option.label;
            }
        }
        return "";
    }

    private void showFilterMenu(
            @NonNull View anchor,
            @NonNull List<FilterOptionUi> options,
            @Nullable String selectedValue,
            @NonNull FilterSelectionCallback callback
    ) {
        if (options.isEmpty()) {
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(getContext());
        View popupView = inflater.inflate(R.layout.view_sort_dropdown, null);
        LinearLayout container = popupView.findViewById(R.id.sortDropdownContainer);

        PopupWindow popupWindow = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );

        for (int i = 0; i < options.size(); i++) {
            FilterOptionUi option = options.get(i);
            boolean isSelected = equalsNullable(option.id, selectedValue);

            TextView itemView = createDropdownItem(option.label, isSelected);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );

            if (i > 0) {
                params.topMargin = dpToPx(8);
            }

            itemView.setLayoutParams(params);
            itemView.setOnClickListener(v -> {
                popupWindow.dismiss();
                callback.onSelected(isSelected ? null : option.id);
            });

            container.addView(itemView);
        }

        showDropdownPopup(anchor, popupView, popupWindow, false);
    }

    private TextView createDropdownItem(@NonNull String label, boolean isSelected) {
        TextView itemView = new TextView(getContext());
        itemView.setText(label);
        itemView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        itemView.setTypeface(
                itemView.getTypeface(),
                isSelected ? Typeface.BOLD : Typeface.NORMAL
        );
        itemView.setTextColor(getAttrColor(
                isSelected ? R.attr.sortDropdownTextSelected : R.attr.sortDropdownText
        ));
        itemView.setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14));

        if (isSelected) {
            itemView.setBackgroundResource(R.drawable.bg_sort_dropdown_item_selected);
        }

        return itemView;
    }

    private void showDropdownPopup(
            @NonNull View anchor,
            @NonNull View popupView,
            @NonNull PopupWindow popupWindow,
            boolean alignEnd
    ) {
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setOutsideTouchable(true);
        popupWindow.setElevation(dpToPx(8));

        popupView.measure(
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        );

        int popupWidth = popupView.getMeasuredWidth();
        int xOff = alignEnd ? anchor.getWidth() - popupWidth : 0;

        popupWindow.showAsDropDown(anchor, xOff, dpToPx(8));
    }

    private void showSortMenu() {
        if (currentSortOptions.isEmpty() || onSortTypeChangeListener == null) {
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(getContext());
        View popupView = inflater.inflate(R.layout.view_sort_dropdown, null);
        LinearLayout container = popupView.findViewById(R.id.sortDropdownContainer);

        PopupWindow popupWindow = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );

        for (int i = 0; i < currentSortOptions.size(); i++) {
            SortOptionUi option = currentSortOptions.get(i);
            boolean isSelected = equalsNullable(option.value, currentSelectedSortTypeValue);

            TextView itemView = createDropdownItem(option.label, isSelected);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );

            if (i > 0) {
                params.topMargin = dpToPx(8);
            }

            itemView.setLayoutParams(params);

            itemView.setOnClickListener(v -> {
                popupWindow.dismiss();
                onSortTypeChangeListener.onSortTypeChanged(option.value);
            });

            container.addView(itemView);
        }

        showDropdownPopup(btnSort, popupView, popupWindow, true);
    }

    private void updateSortButtonContentDescription(
            @NonNull List<SortOptionUi> sortOptions,
            @Nullable String selectedSortTypeValue
    ) {
        String selectedLabel = "Orden";

        for (SortOptionUi option : sortOptions) {
            if (equalsNullable(option.value, selectedSortTypeValue)) {
                selectedLabel = option.label;
                break;
            }
        }

        btnSort.setContentDescription(selectedLabel);
    }

    private void renderEmptyState(boolean isEmpty) {
        tvEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    private void renderCategories(
            @NonNull List<CategoryItemUi> items,
            @NonNull Tab selectedTab,
            boolean isEmpty
    ) {
        if (selectedTab != Tab.CATEGORIES || isEmpty) {
            categoriesContainer.setVisibility(View.GONE);
            renderedExpandedCategoryIds.clear();
            return;
        }

        categoriesContainer.setVisibility(View.VISIBLE);
        categoriesContainer.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(getContext());
        Set<String> previouslyExpandedIds = new HashSet<>(renderedExpandedCategoryIds);
        Set<String> nextExpandedIds = new HashSet<>();

        double totalAmount = 0d;
        for (CategoryItemUi item : items) {
            totalAmount += item.amount;
        }

        for (CategoryItemUi item : items) {
            View row = inflater.inflate(R.layout.item_content_category, categoriesContainer, false);

            LinearLayout categoryRow = row.findViewById(R.id.categoryRow);
            FrameLayout categoryIconContainer = row.findViewById(R.id.categoryIconContainer);
            ImageView ivCategoryIcon = row.findViewById(R.id.ivCategoryIcon);
            TextView tvCategoryName = row.findViewById(R.id.tvCategoryName);
            TextView tvCategoryPercentage = row.findViewById(R.id.tvCategoryPercentage);
            TextView tvCategoryAmount = row.findViewById(R.id.tvCategoryAmount);
            ImageView ivCategoryChevron = row.findViewById(R.id.ivCategoryChevron);
            LinearLayout categoryExpensesContainer = row.findViewById(R.id.categoryExpensesContainer);

            com.example.expensetracker.Category category =
                    com.example.expensetracker.Category.fromString(item.categoryName);

            tvCategoryName.setText(item.categoryName);
            tvCategoryAmount.setText(formatAmount(item.amount));
            tvCategoryPercentage.setText(formatCategoryPercentage(item.amount, totalAmount));

            ivCategoryIcon.setImageResource(category.getIconRes());
            categoryIconContainer.setBackground(createCategoryIconBackground(category.getColor()));

            boolean wasExpanded = previouslyExpandedIds.contains(item.categoryId);
            boolean shouldAnimateExpansion = item.expanded && !wasExpanded;
            if (item.expanded) {
                nextExpandedIds.add(item.categoryId);
            }

            ivCategoryChevron.setRotation(shouldAnimateExpansion ? 0f : (item.expanded ? 180f : 0f));

            categoryRow.setOnClickListener(v -> {
                if (onCategoryClickListener == null) {
                    return;
                }

                if (item.expanded) {
                    categoryRow.setEnabled(false);
                    animateCategoryCollapse(categoryExpensesContainer, ivCategoryChevron, () -> {
                        categoryRow.setEnabled(true);
                        onCategoryClickListener.onCategoryClick(item.categoryId);
                    });
                } else {
                    onCategoryClickListener.onCategoryClick(item.categoryId);
                }
            });

            renderCategoryExpenses(categoryExpensesContainer, item);

            if (shouldAnimateExpansion) {
                prepareCategoryExpansionStart(categoryExpensesContainer);
            }

            categoriesContainer.addView(row);

            if (shouldAnimateExpansion) {
                animateCategoryExpansion(categoryExpensesContainer, ivCategoryChevron);
            }
        }

        renderedExpandedCategoryIds.clear();
        renderedExpandedCategoryIds.addAll(nextExpandedIds);
    }

    private void renderCategoryExpenses(
            @NonNull LinearLayout container,
            @NonNull CategoryItemUi category
    ) {
        if (!category.expanded) {
            container.setVisibility(View.GONE);
            return;
        }

        container.setVisibility(View.VISIBLE);
        container.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(getContext());

        for (CategoryExpenseItem expense : category.expenses) {
            View row = inflater.inflate(R.layout.item_content_category_expense, container, false);

            TextView tvExpenseTitle = row.findViewById(R.id.tvExpenseTitle);
            TextView tvExpenseAmount = row.findViewById(R.id.tvExpenseAmount);

            tvExpenseTitle.setText(safeString(expense.getDescription()));
            tvExpenseAmount.setText(formatAmount(expense.getAmount()));

            row.setOnClickListener(v -> {
                if (onExpenseClickListener != null) {
                    onExpenseClickListener.onExpenseClick(expense.getExpenseId());
                }
            });

            container.addView(row);
        }
    }

    private void prepareCategoryExpansionStart(@NonNull View container) {
        ViewGroup.LayoutParams params = container.getLayoutParams();
        params.height = 0;
        container.setLayoutParams(params);
        container.setAlpha(0f);
        container.setVisibility(View.VISIBLE);
    }

    private void animateCategoryExpansion(
            @NonNull View container,
            @NonNull ImageView chevron
    ) {
        container.post(() -> {
            ViewGroup.LayoutParams measureParams = container.getLayoutParams();
            measureParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            container.setLayoutParams(measureParams);

            int targetHeight = container.getMeasuredHeight();

            if (targetHeight <= 0) {
                container.measure(
                        MeasureSpec.makeMeasureSpec(container.getWidth(), MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
                );
                targetHeight = container.getMeasuredHeight();
            }

            if (targetHeight <= 0) {
                chevron.animate().rotation(180f).setDuration(180L).start();
                return;
            }

            ViewGroup.LayoutParams params = container.getLayoutParams();
            params.height = 0;
            container.setLayoutParams(params);
            container.setAlpha(0f);
            container.setVisibility(View.VISIBLE);

            ValueAnimator animator = ValueAnimator.ofInt(0, targetHeight);
            animator.setDuration(180L);
            animator.setInterpolator(new DecelerateInterpolator());
            animator.addUpdateListener(animation -> {
                ViewGroup.LayoutParams animatedParams = container.getLayoutParams();
                animatedParams.height = (int) animation.getAnimatedValue();
                container.setLayoutParams(animatedParams);
            });
            animator.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    ViewGroup.LayoutParams endParams = container.getLayoutParams();
                    endParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    container.setLayoutParams(endParams);
                    container.setAlpha(1f);
                }
            });

            container.animate()
                    .alpha(1f)
                    .setDuration(140L)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
            chevron.animate()
                    .rotation(180f)
                    .setDuration(180L)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
            animator.start();
        });
    }

    private void animateCategoryCollapse(
            @NonNull View container,
            @NonNull ImageView chevron,
            @NonNull Runnable onAnimationEnd
    ) {
        int startHeight = container.getHeight();

        if (startHeight <= 0 || container.getVisibility() != View.VISIBLE) {
            onAnimationEnd.run();
            return;
        }

        ValueAnimator animator = ValueAnimator.ofInt(startHeight, 0);
        animator.setDuration(160L);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            ViewGroup.LayoutParams params = container.getLayoutParams();
            params.height = (int) animation.getAnimatedValue();
            container.setLayoutParams(params);
        });
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                container.setVisibility(View.GONE);
                container.setAlpha(1f);
                onAnimationEnd.run();
            }
        });

        container.animate()
                .alpha(0f)
                .setDuration(120L)
                .setInterpolator(new DecelerateInterpolator())
                .start();
        chevron.animate()
                .rotation(0f)
                .setDuration(160L)
                .setInterpolator(new DecelerateInterpolator())
                .start();
        animator.start();
    }

    private void renderExpenses(
            @NonNull List<ExpenseItemUi> items,
            @NonNull Tab selectedTab,
            boolean isEmpty
    ) {
        if (isEmpty) {
            expensesContainer.setVisibility(View.GONE);
            expensesContainer.removeAllViews();
            lastRenderedExpensesSignature = null;
            pendingExpensePreloadSignature = null;
            return;
        }

        String signature = buildExpensesSignature(items);

        if (selectedTab != Tab.EXPENSES) {
            expensesContainer.setVisibility(View.GONE);
            scheduleExpenseRowsPreload(items, signature);
            return;
        }

        if (!signature.equals(lastRenderedExpensesSignature)) {
            renderExpenseRows(items, signature);
        }

        pendingExpensePreloadSignature = null;
        expensesContainer.setVisibility(View.VISIBLE);
    }

    private void scheduleExpenseRowsPreload(
            @NonNull List<ExpenseItemUi> items,
            @NonNull String signature
    ) {
        if (signature.equals(lastRenderedExpensesSignature)
                || signature.equals(pendingExpensePreloadSignature)) {
            return;
        }

        ArrayList<ExpenseItemUi> snapshot = new ArrayList<>(items);
        pendingExpensePreloadSignature = signature;

        expensesContainer.post(() -> {
            if (!signature.equals(pendingExpensePreloadSignature)
                    || signature.equals(lastRenderedExpensesSignature)) {
                return;
            }

            renderExpenseRows(snapshot, signature);
            expensesContainer.setVisibility(View.GONE);
            pendingExpensePreloadSignature = null;
        });
    }

    private void renderExpenseRows(
            @NonNull List<ExpenseItemUi> items,
            @NonNull String signature
    ) {
        expensesContainer.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(getContext());

        for (int i = 0; i < items.size(); i++) {
            ExpenseItemUi item = items.get(i);
            View row = inflater.inflate(R.layout.item_content_expense, expensesContainer, false);

            TextView tvExpenseTitle = row.findViewById(R.id.tvExpenseTitle);
            TextView tvExpenseDate = row.findViewById(R.id.tvExpenseDate);
            TextView tvExpenseMember = row.findViewById(R.id.tvExpenseMember);
            TextView tvExpenseAmount = row.findViewById(R.id.tvExpenseAmount);
            ImageView ivIndividualExpense = row.findViewById(R.id.ivIndividualExpense);
            View expenseListDivider = row.findViewById(R.id.expenseListDivider);

            tvExpenseTitle.setText(item.title);
            tvExpenseDate.setText(item.date);
            tvExpenseMember.setText(item.memberName);
            tvExpenseAmount.setText(formatAmount(item.amount));
            ivIndividualExpense.setVisibility(item.individual ? View.VISIBLE : View.GONE);
            expenseListDivider.setVisibility(i == items.size() - 1 ? View.GONE : View.VISIBLE);

            row.setOnClickListener(v -> {
                if (onExpenseClickListener != null) {
                    onExpenseClickListener.onExpenseClick(item.id);
                }
            });

            expensesContainer.addView(row);
        }

        lastRenderedExpensesSignature = signature;
    }

    private String buildExpensesSignature(@NonNull List<ExpenseItemUi> items) {
        StringBuilder builder = new StringBuilder();
        builder.append(items.size());

        for (ExpenseItemUi item : items) {
            builder
                    .append('|')
                    .append(item.id)
                    .append(':')
                    .append(item.title)
                    .append(':')
                    .append(item.date)
                    .append(':')
                    .append(item.memberName)
                    .append(':')
                    .append(Math.round(item.amount * 100d))
                    .append(':')
                    .append(item.individual);
        }

        return builder.toString();
    }

    private String formatCategoryPercentage(double amount, double totalAmount) {
        if (totalAmount <= 0d) {
            return "0%";
        }

        int percentage = (int) Math.round((amount * 100d) / totalAmount);
        return percentage + "%";
    }

    private Drawable createCategoryIconBackground(int color) {
        android.graphics.drawable.GradientDrawable drawable =
                new android.graphics.drawable.GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadii(new float[] {
                dpToPx(8), dpToPx(8),
                0, 0,
                0, 0,
                dpToPx(8), dpToPx(8)
        });
        return drawable;
    }
    private String formatAmount(double amount) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
        return format.format(amount);
    }

    private String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    private String safeString(String value) {
        return value == null ? "" : value;
    }

    private boolean equalsNullable(String a, String b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.equals(b);
    }

    private void setupPieChart() {
        pieChartCategories.setUsePercentValues(false);
        pieChartCategories.getDescription().setEnabled(false);
        pieChartCategories.setHoleRadius(75f);
        pieChartCategories.setTransparentCircleRadius(0f);
        pieChartCategories.getLegend().setEnabled(false);
        pieChartCategories.setDrawEntryLabels(true);
        pieChartCategories.setEntryLabelColor(Color.TRANSPARENT);
        pieChartCategories.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        pieChartCategories.setRotationEnabled(false);
        pieChartCategories.setTouchEnabled(false);
        pieChartCategories.setHighlightPerTapEnabled(false);
        pieChartCategories.setDrawRoundedSlices(false);
        //pieChartCategories.setExtraOffsets(0, 4, 0, 4);
        pieChartCategories.setRenderer(
                new SliceIconPieChartRenderer(
                        pieChartCategories,
                        pieChartCategories.getAnimator(),
                        pieChartCategories.getViewPortHandler(),
                        1f,
                        0f
                )
        );
        pieChartCategories.setDrawHoleEnabled(true);
        pieChartCategories.setHoleColor(Color.TRANSPARENT);
        pieChartCategories.setDrawCenterText(true);
        pieChartCategories.setCenterTextSize(30f);
        Typeface chartTypeface = ResourcesCompat.getFont(getContext(), R.font.rajdhani_semibold);
        pieChartCategories.setCenterTextTypeface(chartTypeface != null ? chartTypeface : Typeface.DEFAULT_BOLD);
        pieChartCategories.setCenterTextColor(getAttrColor(R.attr.textPrimary));
    }

    private void renderPieChart(
            @NonNull List<CategoryItemUi> items,
            @NonNull Tab selectedTab,
            boolean isEmpty
    ) {
        if (selectedTab != Tab.CATEGORIES || isEmpty) {
            pieChartCategories.setVisibility(View.GONE);
            pieChartCategories.clear();
            lastPieChartSignature = null;
            return;
        }

        ArrayList<CategoryItemUi> visibleItems = new ArrayList<>();
        double total = 0;

        for (CategoryItemUi item : items) {
            if (item.amount <= 0) {
                continue;
            }
            visibleItems.add(item);
            total += item.amount;
        }

        if (visibleItems.isEmpty() || total <= 0) {
            pieChartCategories.setVisibility(View.GONE);
            pieChartCategories.clear();
            lastPieChartSignature = null;
            return;
        }

        String pieChartSignature = buildPieChartSignature(visibleItems, total);
        boolean shouldAnimateChart = !pieChartSignature.equals(lastPieChartSignature);
        lastPieChartSignature = pieChartSignature;

        if (pieChartValueAnimator != null) {
            pieChartValueAnimator.cancel();
            pieChartValueAnimator = null;
        }

        if (shouldAnimateChart && !lastPieChartItems.isEmpty()) {
            pieChartCategories.setVisibility(View.VISIBLE);
            animatePieChartValues(lastPieChartItems, visibleItems);
            return;
        }

        ArrayList<Float> displayPercentages = new ArrayList<>();
        float totalExtraFromFloor = 0f;
        int biggestIndex = -1;
        double biggestAmount = -1;

        for (int i = 0; i < visibleItems.size(); i++) {
            CategoryItemUi item = visibleItems.get(i);

            float actualPercentage = (float) (item.amount * 100d / total);
            float displayPercentage = Math.max(actualPercentage, item.amount > 0 ? 1.3f : 0f);

            displayPercentages.add(displayPercentage);
            totalExtraFromFloor += (displayPercentage - actualPercentage);

            if (item.amount > biggestAmount) {
                biggestAmount = item.amount;
                biggestIndex = i;
            }
        }

        if (biggestIndex >= 0) {
            float sumOfOthers = 0f;

            for (int i = 0; i < displayPercentages.size(); i++) {
                if (i != biggestIndex) {
                    sumOfOthers += displayPercentages.get(i);
                }
            }

            displayPercentages.set(biggestIndex, 100f - sumOfOthers);
        }

        ArrayList<PieEntry> entries = new ArrayList<>();
        ArrayList<Integer> colors = new ArrayList<>();

        for (int i = 0; i < visibleItems.size(); i++) {
            CategoryItemUi item = visibleItems.get(i);
            float displayPercentage = displayPercentages.get(i);

            Category category = Category.fromString(item.categoryName);
            PieEntry entry = new PieEntry(displayPercentage, item.categoryName);

            if (displayPercentage >= 5f && getContext() != null) {
                Drawable icon = ContextCompat.getDrawable(getContext(), category.getIconRes());

                if (icon != null) {
                    icon = icon.mutate();
                    icon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
                    //icon.setBounds(2, -26, -2, 26);
                    entry.setIcon(icon);
                }
            }

            entries.add(entry);
            colors.add(category.getColor());
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setSliceSpace(8f);
        dataSet.setSelectionShift(0f);
        dataSet.setDrawValues(false);
        dataSet.setDrawIcons(true);
        dataSet.setIconsOffset(new MPPointF(0f, 0f));


        PieData data = new PieData(dataSet);
        pieChartCategories.setData(data);

// 🔥 Rounded slices (traído de ResumenFragment)
        Paint paint = pieChartCategories.getRenderer().getPaintRender();
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(0f);
        paint.setPathEffect(null);
        paint.setAntiAlias(true);

// Ajuste para que no se desborde visualmente
        pieChartCategories.setHoleRadius(77f);

// Center text (lo dejamos como lo tenías)
        pieChartCategories.setCenterText(formatAmount(total));

        pieChartCategories.setVisibility(View.VISIBLE);
        pieChartCategories.notifyDataSetChanged();
        pieChartCategories.invalidate();
        lastPieChartItems = copyPieChartItems(visibleItems);
    }

    private void animatePieChartRefresh() {
        pieChartCategories.animate().cancel();
        pieChartCategories.setAlpha(0.72f);
        pieChartCategories.setScaleX(0.985f);
        pieChartCategories.setScaleY(0.985f);
        pieChartCategories.invalidate();
        pieChartCategories.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(220L)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private void animatePieChartValues(
            @NonNull List<CategoryItemUi> previousItems,
            @NonNull List<CategoryItemUi> targetItems
    ) {
        Map<String, CategoryItemUi> previousById = mapCategoryItemsById(previousItems);
        Map<String, CategoryItemUi> targetById = mapCategoryItemsById(targetItems);
        ArrayList<CategoryItemUi> animationOrder = new ArrayList<>();

        for (CategoryItemUi targetItem : targetItems) {
            animationOrder.add(targetItem);
        }

        for (CategoryItemUi previousItem : previousItems) {
            if (!targetById.containsKey(previousItem.categoryId)) {
                animationOrder.add(previousItem);
            }
        }

        pieChartValueAnimator = ValueAnimator.ofFloat(0f, 1f);
        pieChartValueAnimator.setDuration(360L);
        pieChartValueAnimator.setInterpolator(new DecelerateInterpolator());
        pieChartValueAnimator.addUpdateListener(animation -> {
            float progress = (float) animation.getAnimatedValue();
            ArrayList<CategoryItemUi> animatedItems = new ArrayList<>();

            for (CategoryItemUi item : animationOrder) {
                double fromAmount = previousById.containsKey(item.categoryId)
                        ? previousById.get(item.categoryId).amount
                        : 0d;
                double toAmount = targetById.containsKey(item.categoryId)
                        ? targetById.get(item.categoryId).amount
                        : 0d;
                double animatedAmount = fromAmount + ((toAmount - fromAmount) * progress);

                if (animatedAmount <= 0.01d) {
                    continue;
                }

                animatedItems.add(new CategoryItemUi(
                        item.categoryId,
                        item.categoryName,
                        animatedAmount,
                        item.expanded,
                        item.expenses
                ));
            }

            applyPieChartData(animatedItems);
        });
        pieChartValueAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                applyPieChartData(targetItems);
                lastPieChartItems = copyPieChartItems(targetItems);
                pieChartValueAnimator = null;
            }
        });
        pieChartValueAnimator.start();
    }

    private void applyPieChartData(@NonNull List<CategoryItemUi> visibleItems) {
        double total = 0d;

        for (CategoryItemUi item : visibleItems) {
            if (item.amount > 0d) {
                total += item.amount;
            }
        }

        if (visibleItems.isEmpty() || total <= 0d) {
            pieChartCategories.clear();
            pieChartCategories.invalidate();
            return;
        }

        ArrayList<Float> displayPercentages = new ArrayList<>();
        int biggestIndex = -1;
        double biggestAmount = -1;

        for (int i = 0; i < visibleItems.size(); i++) {
            CategoryItemUi item = visibleItems.get(i);
            float actualPercentage = (float) (item.amount * 100d / total);
            float displayPercentage = Math.max(actualPercentage, item.amount > 0 ? 1.3f : 0f);
            displayPercentages.add(displayPercentage);

            if (item.amount > biggestAmount) {
                biggestAmount = item.amount;
                biggestIndex = i;
            }
        }

        if (biggestIndex >= 0) {
            float sumOfOthers = 0f;

            for (int i = 0; i < displayPercentages.size(); i++) {
                if (i != biggestIndex) {
                    sumOfOthers += displayPercentages.get(i);
                }
            }

            displayPercentages.set(biggestIndex, 100f - sumOfOthers);
        }

        ArrayList<PieEntry> entries = new ArrayList<>();
        ArrayList<Integer> colors = new ArrayList<>();

        for (int i = 0; i < visibleItems.size(); i++) {
            CategoryItemUi item = visibleItems.get(i);
            float displayPercentage = displayPercentages.get(i);
            Category category = Category.fromString(item.categoryName);
            PieEntry entry = new PieEntry(displayPercentage, item.categoryName);

            if (displayPercentage >= 5f && getContext() != null) {
                Drawable icon = ContextCompat.getDrawable(getContext(), category.getIconRes());

                if (icon != null) {
                    icon = icon.mutate();
                    icon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
                    entry.setIcon(icon);
                }
            }

            entries.add(entry);
            colors.add(category.getColor());
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setSliceSpace(8f);
        dataSet.setSelectionShift(0f);
        dataSet.setDrawValues(false);
        dataSet.setDrawIcons(true);
        dataSet.setIconsOffset(new MPPointF(0f, 0f));

        pieChartCategories.setData(new PieData(dataSet));
        Paint paint = pieChartCategories.getRenderer().getPaintRender();
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(0f);
        paint.setPathEffect(null);
        paint.setAntiAlias(true);
        pieChartCategories.setHoleRadius(77f);
        pieChartCategories.setCenterText(formatAmount(total));
        pieChartCategories.notifyDataSetChanged();
        pieChartCategories.invalidate();
    }

    private Map<String, CategoryItemUi> mapCategoryItemsById(@NonNull List<CategoryItemUi> items) {
        Map<String, CategoryItemUi> result = new HashMap<>();

        for (CategoryItemUi item : items) {
            result.put(item.categoryId, item);
        }

        return result;
    }

    private List<CategoryItemUi> copyPieChartItems(@NonNull List<CategoryItemUi> items) {
        ArrayList<CategoryItemUi> result = new ArrayList<>();

        for (CategoryItemUi item : items) {
            result.add(new CategoryItemUi(
                    item.categoryId,
                    item.categoryName,
                    item.amount,
                    item.expanded,
                    item.expenses
            ));
        }

        return result;
    }

    private String buildPieChartSignature(
            @NonNull List<CategoryItemUi> visibleItems,
            double total
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append(Math.round(total * 100d));

        for (CategoryItemUi item : visibleItems) {
            builder
                    .append('|')
                    .append(item.categoryId)
                    .append(':')
                    .append(Math.round(item.amount * 100d));
        }

        return builder.toString();
    }

    private int getChartColor(int index) {
        int[] colors = new int[] {
                0xFFEF5350,
                0xFFAB47BC,
                0xFF5C6BC0,
                0xFF29B6F6,
                0xFF26A69A,
                0xFF9CCC65,
                0xFFFFCA28,
                0xFFFFA726,
                0xFF8D6E63,
                0xFF78909C
        };
        return colors[index % colors.length];
    }
    private void updateTabStyle(TextView tab, boolean isActive) {
        if (isActive) {
            tab.setBackgroundResource(R.drawable.bg_tab_active);
            tab.setTextColor(Color.WHITE);
        } else {
            tab.setBackgroundResource(R.drawable.bg_tab_inactive);
            tab.setTextColor(getAttrColor(R.attr.contentTabInactiveText));
        }

        Typeface typeface = ResourcesCompat.getFont(
                getContext(),
                isActive ? R.font.rajdhani_semibold : R.font.rajdhani_medium
        );
        tab.setTypeface(typeface != null ? typeface : tab.getTypeface());
    }
    private int getAttrColor(int attr) {
        TypedValue typedValue = new TypedValue();
        getContext().getTheme().resolveAttribute(attr, typedValue, true);

        if (typedValue.resourceId != 0) {
            return androidx.core.content.ContextCompat.getColor(getContext(), typedValue.resourceId);
        }

        return typedValue.data;
    }
    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }


}
