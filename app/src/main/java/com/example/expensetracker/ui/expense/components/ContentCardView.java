package com.example.expensetracker.ui.expense.components;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

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

    private Button btnTabCategories;
    private Button btnTabExpenses;
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
        boolean categoriesSelected = selectedTab == Tab.CATEGORIES;

        btnTabCategories.setEnabled(!categoriesSelected);
        btnTabExpenses.setEnabled(categoriesSelected);

        btnTabCategories.setAlpha(categoriesSelected ? 1f : 0.6f);
        btnTabExpenses.setAlpha(categoriesSelected ? 0.6f : 1f);
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
            chip.setCheckable(true);
            chip.setClickable(true);
            chip.setCheckedIconVisible(false);

            final String optionId = option.id;

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

        if (checkedChipId != null) {
            chipGroupMembers.check(checkedChipId);
        } else {
            chipGroupMembers.clearCheck();
        }
    }

    private void showSortMenu() {
        if (currentSortOptions.isEmpty() || onSortTypeChangeListener == null) {
            return;
        }

        PopupMenu popupMenu = new PopupMenu(getContext(), btnSort);
        Menu menu = popupMenu.getMenu();

        for (int i = 0; i < currentSortOptions.size(); i++) {
            SortOptionUi option = currentSortOptions.get(i);
            menu.add(Menu.NONE, i, i, option.label);
        }

        popupMenu.setOnMenuItemClickListener(item -> {
            int index = item.getItemId();
            if (index < 0 || index >= currentSortOptions.size()) {
                return false;
            }

            onSortTypeChangeListener.onSortTypeChanged(
                    currentSortOptions.get(index).value
            );
            return true;
        });

        popupMenu.show();
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

        for (CategoryItemUi item : items) {
            View row = inflater.inflate(R.layout.item_content_category, categoriesContainer, false);

            TextView tvCategoryName = row.findViewById(R.id.tvCategoryName);
            TextView tvCategoryAmount = row.findViewById(R.id.tvCategoryAmount);
            TextView tvCategoryIndicator = row.findViewById(R.id.tvCategoryIndicator);
            LinearLayout categoryExpensesContainer = row.findViewById(R.id.categoryExpensesContainer);

            tvCategoryName.setText(item.categoryName);
            tvCategoryAmount.setText(formatAmount(item.amount));
            tvCategoryIndicator.setText(item.expanded ? "▼" : "▶");

            row.setOnClickListener(v -> {
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
            TextView tvExpenseMeta = row.findViewById(R.id.tvExpenseMeta);
            TextView tvExpenseAmount = row.findViewById(R.id.tvExpenseAmount);

            tvExpenseTitle.setText(safeString(expense.getDescription()));
            tvExpenseMeta.setText(
                    formatDate(expense.getDate()) + " • " + safeString(expense.getMemberName())
            );
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
            TextView tvExpenseMeta = row.findViewById(R.id.tvExpenseMeta);
            TextView tvExpenseAmount = row.findViewById(R.id.tvExpenseAmount);

            tvExpenseTitle.setText(item.title);
            tvExpenseMeta.setText(item.date + " • " + item.memberName + " • " + item.categoryName);
            tvExpenseAmount.setText(formatAmount(item.amount));

            row.setOnClickListener(v -> {
                if (onExpenseClickListener != null) {
                    onExpenseClickListener.onExpenseClick(item.id);
                }
            });

            expensesContainer.addView(row);
        }
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
        pieChartCategories.setDrawHoleEnabled(true);
        pieChartCategories.setHoleColor(Color.TRANSPARENT);
        pieChartCategories.setHoleRadius(75f);
        pieChartCategories.setTransparentCircleRadius(0f);
        pieChartCategories.setDrawCenterText(true);
        pieChartCategories.getLegend().setEnabled(false);
        pieChartCategories.setDrawEntryLabels(true);
        pieChartCategories.setEntryLabelColor(Color.TRANSPARENT);
        pieChartCategories.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        pieChartCategories.setRotationEnabled(false);
        pieChartCategories.setTouchEnabled(false);
        pieChartCategories.setHighlightPerTapEnabled(false);
        pieChartCategories.setDrawRoundedSlices(false);
        pieChartCategories.setExtraOffsets(0, 5f, 0, 5f);
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
                    icon.setBounds(4, -32, -4, 32);
                    entry.setIcon(icon);
                }
            }

            entries.add(entry);
            colors.add(category.getColor());
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setSliceSpace(12f);
        dataSet.setSelectionShift(0f);
        dataSet.setDrawValues(false);
        dataSet.setDrawIcons(true);
        dataSet.setIconsOffset(new MPPointF(8f, 0f));

        PieData data = new PieData(dataSet);

        pieChartCategories.setVisibility(View.VISIBLE);
        pieChartCategories.setData(data);
        pieChartCategories.setCenterText(formatAmount(total));
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

}