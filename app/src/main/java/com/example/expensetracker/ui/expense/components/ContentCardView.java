package com.example.expensetracker.ui.expense.components;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
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

import com.example.expensetracker.Category;
import com.example.expensetracker.R;
import com.example.expensetracker.calculator.CategoryExpenseItem;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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

    public interface OnSortTypeChangeListener {
        void onSortTypeChanged(@NonNull String sortTypeValue);
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
        public final List<SortOptionUi> sortOptions;
        public final String selectedSortTypeValue;

        public Model(
                @NonNull Tab selectedTab,
                @NonNull List<CategoryItemUi> categories,
                @NonNull List<ExpenseItemUi> expenses,
                boolean isEmpty,
                @NonNull List<FilterOptionUi> memberFilterOptions,
                @Nullable String selectedMemberFilterId,
                @NonNull List<SortOptionUi> sortOptions,
                @Nullable String selectedSortTypeValue
        ) {
            this.selectedTab = selectedTab;
            this.categories = categories;
            this.expenses = expenses;
            this.isEmpty = isEmpty;
            this.memberFilterOptions = memberFilterOptions;
            this.selectedMemberFilterId = selectedMemberFilterId;
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

        public ExpenseItemUi(
                @NonNull String id,
                @NonNull String title,
                @NonNull String date,
                @NonNull String memberName,
                double amount,
                @NonNull String categoryName
        ) {
            this.id = id;
            this.title = title;
            this.date = date;
            this.memberName = memberName;
            this.amount = amount;
            this.categoryName = categoryName;
        }
    }

    private TextView btnTabCategories;
    private TextView btnTabExpenses;
    private LinearLayout filtersContainer;
    private ChipGroup chipGroupMembers;
    private ImageView btnSort;
    private TextView tvEmptyState;
    private PieChart pieChartCategories;
    private LinearLayout categoriesContainer;
    private LinearLayout expensesContainer;

    private OnCategoryClickListener onCategoryClickListener;
    private OnExpenseClickListener onExpenseClickListener;
    private OnMemberFilterChangeListener onMemberFilterChangeListener;
    private OnSortTypeChangeListener onSortTypeChangeListener;

    private List<FilterOptionUi> currentMemberFilterOptions = new ArrayList<>();
    private List<SortOptionUi> currentSortOptions = new ArrayList<>();
    @Nullable
    private String currentSelectedMemberFilterId;
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
        chipGroupMembers = findViewById(R.id.chipGroupMembers);
        btnSort = findViewById(R.id.btnSort);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        pieChartCategories = findViewById(R.id.pieChartCategories);
        setupPieChart();
        categoriesContainer = findViewById(R.id.categoriesContainer);
        expensesContainer = findViewById(R.id.expensesContainer);

        btnSort.setOnClickListener(v -> showSortMenu());
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

    public void setOnSortTypeChangeListener(@Nullable OnSortTypeChangeListener listener) {
        this.onSortTypeChangeListener = listener;
    }

    public void render(@NonNull Model model) {
        renderTabs(model.selectedTab);
        renderFilters(
                model.selectedTab,
                model.memberFilterOptions,
                model.selectedMemberFilterId,
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
        currentSortOptions = new ArrayList<>(sortOptions);
        currentSelectedMemberFilterId = selectedMemberFilterId;
        currentSelectedSortTypeValue = selectedSortTypeValue;

        renderMemberChips(memberFilterOptions, selectedMemberFilterId);
        updateSortButtonContentDescription(sortOptions, selectedSortTypeValue);
    }

    private void renderMemberChips(
            @NonNull List<FilterOptionUi> memberFilterOptions,
            @Nullable String selectedMemberFilterId
    ) {
        chipGroupMembers.setOnCheckedStateChangeListener(null);
        chipGroupMembers.removeAllViews();

        Integer checkedChipId = null;

        for (int i = 0; i < memberFilterOptions.size(); i++) {
            FilterOptionUi option = memberFilterOptions.get(i);

            Chip chip = new Chip(getContext());
            chip.setId(View.generateViewId());
            chip.setText(option.label);
            chip.setCheckable(false);
            chip.setClickable(true);
            chip.setCloseIconVisible(false);
            chip.setEnsureMinTouchTargetSize(false);
            chip.setChipMinHeight(dpToPx(32));
            chip.setChipCornerRadius(dpToPx(8));
            chip.setChipStrokeWidth(dpToPx(1));
            chip.setChipStartPadding(dpToPx(0));
            chip.setTextStartPadding(dpToPx(0));
            chip.setTextEndPadding(dpToPx(0));
            chip.setChipEndPadding(dpToPx(0));
            chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            chip.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
            chip.setChipIconResource(com.google.android.material.R.drawable.ic_mtrl_chip_checked_black);
            chip.setChipIconVisible(false);
            chip.setChipIconSize(dpToPx(18));
            chip.setIconStartPadding(dpToPx(8));
            chip.setIconEndPadding(dpToPx(8));

            final String optionId = option.id;
            chip.setTag(optionId);

            chip.setOnClickListener(v -> {
                if (onMemberFilterChangeListener == null) {
                    return;
                }

                if (equalsNullable(currentSelectedMemberFilterId, optionId)) {
                    onMemberFilterChangeListener.onMemberFilterChanged(null);
                } else {
                    onMemberFilterChangeListener.onMemberFilterChanged(optionId);
                }
            });

            if (equalsNullable(option.id, selectedMemberFilterId)) {
                checkedChipId = chip.getId();
            }


            chipGroupMembers.addView(chip);
        }

        for (int i = 0; i < chipGroupMembers.getChildCount(); i++) {
            View child = chipGroupMembers.getChildAt(i);
            if (child instanceof Chip) {
                Chip chip = (Chip) child;
                String chipOptionId = (String) chip.getTag();
                boolean isChecked = equalsNullable(chipOptionId, selectedMemberFilterId);
                applyFilterChipStyle(chip, isChecked);
            }
        }
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

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );

            if (i > 0) {
                params.topMargin = dpToPx(8);
            }

            TextView itemView = new TextView(getContext());
            itemView.setLayoutParams(params);
            itemView.setText(option.label);
            itemView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            itemView.setTypeface(
                    Typeface.DEFAULT,
                    isSelected ? Typeface.BOLD : Typeface.NORMAL
            );
            itemView.setTextColor(getAttrColor(
                    isSelected ? R.attr.sortDropdownTextSelected : R.attr.sortDropdownText
            ));
            itemView.setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14));

            if (isSelected) {
                itemView.setBackgroundResource(R.drawable.bg_sort_dropdown_item_selected);
            }

            itemView.setOnClickListener(v -> {
                popupWindow.dismiss();
                onSortTypeChangeListener.onSortTypeChanged(option.value);
            });

            container.addView(itemView);
        }

        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setOutsideTouchable(true);
        popupWindow.setElevation(dpToPx(8));

        popupView.measure(
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        );

        int popupWidth = popupView.getMeasuredWidth();
        int xOff = btnSort.getWidth() - popupWidth;

        popupWindow.showAsDropDown(btnSort, xOff, dpToPx(8));
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
            return;
        }

        categoriesContainer.setVisibility(View.VISIBLE);
        categoriesContainer.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(getContext());

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

            ivCategoryChevron.setRotation(item.expanded ? 180f : 0f);

            categoryRow.setOnClickListener(v -> {
                if (onCategoryClickListener != null) {
                    onCategoryClickListener.onCategoryClick(item.categoryId);
                }
            });

            renderCategoryExpenses(categoryExpensesContainer, item);

            categoriesContainer.addView(row);
        }
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

    private void renderExpenses(
            @NonNull List<ExpenseItemUi> items,
            @NonNull Tab selectedTab,
            boolean isEmpty
    ) {
        if (selectedTab != Tab.EXPENSES || isEmpty) {
            expensesContainer.setVisibility(View.GONE);
            return;
        }

        expensesContainer.setVisibility(View.VISIBLE);
        expensesContainer.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(getContext());

        for (ExpenseItemUi item : items) {
            View row = inflater.inflate(R.layout.item_content_expense, expensesContainer, false);

            TextView tvExpenseTitle = row.findViewById(R.id.tvExpenseTitle);
            TextView tvExpenseDate = row.findViewById(R.id.tvExpenseDate);
            TextView tvExpenseMember = row.findViewById(R.id.tvExpenseMember);
            TextView tvExpenseAmount = row.findViewById(R.id.tvExpenseAmount);

            tvExpenseTitle.setText(item.title);
            tvExpenseDate.setText(item.date);
            tvExpenseMember.setText(item.memberName);
            tvExpenseAmount.setText(formatAmount(item.amount));

            row.setOnClickListener(v -> {
                if (onExpenseClickListener != null) {
                    onExpenseClickListener.onExpenseClick(item.id);
                }
            });

            expensesContainer.addView(row);
        }
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
        pieChartCategories.setCenterTextSize(24f);
        pieChartCategories.setCenterTextTypeface(Typeface.DEFAULT_BOLD);
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
            tab.setTypeface(tab.getTypeface(), Typeface.BOLD);
        } else {
            tab.setBackgroundResource(R.drawable.bg_tab_inactive);
            tab.setTextColor(getAttrColor(R.attr.contentTabInactiveText));
            tab.setTypeface(tab.getTypeface(), Typeface.NORMAL);
        }
    }
    private int getAttrColor(int attr) {
        TypedValue typedValue = new TypedValue();
        getContext().getTheme().resolveAttribute(attr, typedValue, true);

        if (typedValue.resourceId != 0) {
            return androidx.core.content.ContextCompat.getColor(getContext(), typedValue.resourceId);
        }

        return typedValue.data;
    }
    private void applyFilterChipStyle(@NonNull Chip chip, boolean isChecked) {
        if (isChecked) {
            chip.setChipIconVisible(true);

            chip.setChipBackgroundColorResource(android.R.color.transparent);
            chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(getAttrColor(R.attr.filterChipActiveBg)));
            chip.setTextColor(getAttrColor(R.attr.filterChipActiveText));
            chip.setChipIconTint(android.content.res.ColorStateList.valueOf(getAttrColor(R.attr.filterChipActiveIcon)));
            chip.setChipStrokeColor(android.content.res.ColorStateList.valueOf(getAttrColor(R.attr.filterChipActiveBg)));

            chip.setChipStartPadding(dpToPx(8));
            chip.setIconStartPadding(dpToPx(0));
            chip.setIconEndPadding(dpToPx(8));
            chip.setTextStartPadding(dpToPx(0));
            chip.setTextEndPadding(dpToPx(0));
            chip.setChipEndPadding(dpToPx(16));
        } else {
            chip.setChipIconVisible(false);

            chip.setChipBackgroundColorResource(android.R.color.transparent);
            chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(Color.TRANSPARENT));
            chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(getAttrColor(R.attr.filterChipInactiveBg)));
            chip.setChipStrokeWidth(1f);
            chip.setChipStrokeColor(android.content.res.ColorStateList.valueOf(getAttrColor(R.attr.filterChipInactiveBorder)));
            chip.setTextColor(getAttrColor(R.attr.filterChipInactiveText));
            chip.setChipStartPadding(dpToPx(16));
            chip.setIconStartPadding(dpToPx(0));
            chip.setIconEndPadding(dpToPx(0));
            chip.setTextStartPadding(dpToPx(0));
            chip.setTextEndPadding(dpToPx(0));
            chip.setChipEndPadding(dpToPx(16));
        }
    }
    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }


}