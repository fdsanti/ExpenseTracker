package com.example.expensetracker;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.example.expensetracker.calculator.ExpenseListQuery;
import com.example.expensetracker.data.TrackerRepository;
import com.example.expensetracker.ui.expense.BalanceActivity;
import com.example.expensetracker.ui.expense.dialogs.EditExpenseDialog;
import com.example.expensetracker.ui.expense.ExpenseScreenController;
import com.example.expensetracker.ui.expense.ExpenseScreenListener;
import com.example.expensetracker.ui.expense.ExpenseScreenState;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.expensetracker.ui.expense.ExpenseUiMapper;
import com.example.expensetracker.ui.expense.components.SummaryCardView;
import com.example.expensetracker.ui.expense.components.BalanceCardView;
import com.example.expensetracker.ui.expense.components.MembersCardView;
import com.example.expensetracker.ui.expense.components.ContentCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupWindow;


public class ExpenseActivityV2 extends AppCompatActivity implements ExpenseScreenListener {

    private ExpenseScreenController controller;
    private TextView txtTrackerName;
    private SummaryCardView summaryCard;
    private TextView txtLoading;
    private BalanceCardView balanceCard;
    private MembersCardView membersCard;
    private ContentCardView contentCard;
    private View btnBack;
    private View btnMoreOptions;
    private ExpenseScreenState currentState;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense_v2);
        txtTrackerName = findViewById(R.id.txtTrackerName);
        txtLoading = findViewById(R.id.txtLoading);
        summaryCard = findViewById(R.id.summaryCard);
        balanceCard = findViewById(R.id.balanceCard);
        membersCard = findViewById(R.id.membersCard);
        contentCard = findViewById(R.id.contentCard);
        btnBack = findViewById(R.id.btnBack);
        btnMoreOptions = findViewById(R.id.btnMoreOptions);

        balanceCard.setOnReviewBalanceClickListener(v -> {
            if (currentState == null || currentState.tracker == null || currentState.tracker.getId() == null) {
                return;
            }

            Intent intent = new Intent(this, BalanceActivity.class);
            intent.putExtra(BalanceActivity.EXTRA_TRACKER_ID, currentState.tracker.getId());
            startActivity(intent);
        });

        membersCard.setOnEditMembersClickListener(v -> {
            if (currentState == null || currentState.tracker == null || currentState.tracker.getId() == null) {
                Toast.makeText(this, "No se pudo abrir configuración", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(this, SettingsActivity.class);
            intent.putExtra("trackerId", currentState.tracker.getId());
            intent.putExtra("fromExpenseV2", true);
            startActivity(intent);
        });

        contentCard.setOnCategoriesClickListener(v ->
                controller.setSelectedTab(ExpenseScreenState.ContentTab.CATEGORIES)
        );

        contentCard.setOnExpensesClickListener(v ->
                controller.setSelectedTab(ExpenseScreenState.ContentTab.EXPENSES)
        );

        contentCard.setOnCategoryClickListener(categoryId ->
                controller.toggleCategoryExpanded(categoryId)
        );

        contentCard.setOnExpenseClickListener(expenseId ->
                EditExpenseDialog.showEdit(this, currentState, expenseId, controller)
        );

        contentCard.setOnMemberFilterChangeListener(memberId ->
                controller.setMemberFilter(memberId)
        );

        contentCard.setOnSortTypeChangeListener(sortTypeValue -> {
            ExpenseListQuery.SortType sortType =
                    ExpenseListQuery.SortType.valueOf(sortTypeValue);

            controller.setSortType(sortType);
        });

        btnBack.setOnClickListener(v -> finish());
        btnMoreOptions.setOnClickListener(v -> showMoreOptionsMenu());

        View fab = findViewById(R.id.fabAddExpense);

        fab.setOnClickListener(v ->
                EditExpenseDialog.showCreate(this, currentState, controller)
        );

        Log.d("ExpenseV2", "ExpenseActivityV2 started");


        String trackerId = getIntent().getStringExtra("trackerId");

        if (trackerId == null) {
            Log.e("ExpenseV2", "trackerId is null");
            finish();
            return;
        }

        TrackerRepository repository = new TrackerRepository();

        controller = new ExpenseScreenController(repository);
        controller.setListener(this);
        controller.setTrackerId(trackerId);

        controller.load();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (controller != null && currentState != null) {
            controller.refresh();
        }
    }
    @Override
    public void onStateChanged(ExpenseScreenState state) {
        currentState = state;
        render(state);
    }
    private void render(ExpenseScreenState state) {
        renderHeader(state);
        renderCards(state);
        renderLoading(state);
    }

    private void renderHeader(ExpenseScreenState state) {
        if (state.tracker != null) {
            txtTrackerName.setText(state.tracker.getName());
        } else {
            txtTrackerName.setText("Cargando tracker...");
        }
    }

    private void renderCards(ExpenseScreenState state) {
        summaryCard.render(state.expenseSummary);
        balanceCard.render(state.debtSummary);
        membersCard.render(state.members);
        contentCard.render(ExpenseUiMapper.toContentCardModel(state));
    }

    private void renderLoading(ExpenseScreenState state) {
        txtLoading.setVisibility(state.loading ? View.VISIBLE : View.GONE);
    }

    private void showMoreOptionsMenu() {
        if (btnMoreOptions == null) {
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        View popupView = inflater.inflate(R.layout.view_sort_dropdown, null);
        LinearLayout container = popupView.findViewById(R.id.sortDropdownContainer);

        PopupWindow popupWindow = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );

        String closeOptionLabel;

        if (currentState != null && currentState.tracker != null && currentState.tracker.isClosed()) {
            closeOptionLabel = "Abrir tracker";
        } else {
            closeOptionLabel = "Cerrar tracker";
        }

        addMoreOptionItem(container, "Editar nombre", popupWindow, this::showEditTrackerNameDialog);
        addMoreOptionItem(container, "Configuración", popupWindow, this::openSettings);
        addMoreOptionItem(container, closeOptionLabel, popupWindow, this::confirmCloseTracker);

        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setOutsideTouchable(true);
        popupWindow.setElevation(dpToPx(8));

        popupView.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );

        int popupWidth = popupView.getMeasuredWidth();
        int xOff = btnMoreOptions.getWidth() - popupWidth;

        popupWindow.showAsDropDown(btnMoreOptions, xOff, dpToPx(8));
    }

    private void addMoreOptionItem(
            LinearLayout container,
            String label,
            PopupWindow popupWindow,
            Runnable action
    ) {
        android.widget.TextView itemView = new android.widget.TextView(this);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );

        if (container.getChildCount() > 0) {
            params.topMargin = dpToPx(8);
        }

        itemView.setLayoutParams(params);
        itemView.setText(label);
        itemView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 16);
        itemView.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        itemView.setTextColor(getAttrColor(R.attr.sortDropdownText));
        itemView.setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14));
        itemView.setBackgroundResource(R.drawable.bg_content_category_expense_click);
        itemView.setClickable(true);
        itemView.setFocusable(true);

        itemView.setOnClickListener(v -> {
            popupWindow.dismiss();
            action.run();
        });

        container.addView(itemView);
    }

    private void showEditTrackerNameDialog() {
        if (currentState == null || currentState.tracker == null) {
            return;
        }

        String currentName = currentState.tracker.getName();

        final android.widget.EditText input = new android.widget.EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(currentName);
        input.setSelection(input.getText().length());

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Editar nombre")
                .setView(input)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String newName = input.getText().toString().trim();

                    if (newName.isEmpty()) {
                        Toast.makeText(this, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    controller.updateTrackerName(newName);
                    HCardDB.setName(currentState.tracker.getId(), newName);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void openSettings() {
        if (currentState == null || currentState.tracker == null || currentState.tracker.getId() == null) {
            Toast.makeText(this, "No se pudo abrir configuración", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, SettingsActivity.class);
        intent.putExtra("trackerId", currentState.tracker.getId());
        intent.putExtra("fromExpenseV2", true);
        startActivity(intent);
    }

    private void confirmCloseTracker() {
        if (currentState == null || currentState.tracker == null || currentState.tracker.getId() == null) {
            Toast.makeText(this, "No se pudo actualizar el tracker", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean currentlyClosed = currentState.tracker.isClosed();
        String title = currentlyClosed ? "Abrir tracker" : "Cerrar tracker";
        String message = currentlyClosed
                ? "¿Estás seguro que querés abrir el tracker nuevamente?"
                : "¿Estás seguro que querés cerrar el tracker? Esto deshabilitará las funcionalidades del mismo.";

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Confirmar", (dialog, which) -> {
                    boolean newClosedValue = !currentlyClosed;

                    controller.updateTrackerClosed(newClosedValue);
                    HCardDB.setCerrado(newClosedValue);

                    Toast.makeText(
                            this,
                            newClosedValue ? "Tracker cerrado" : "Tracker abierto",
                            Toast.LENGTH_SHORT
                    ).show();

                    finish();
                })
                .show();
    }

    private int getAttrColor(int attr) {
        android.util.TypedValue typedValue = new android.util.TypedValue();
        getTheme().resolveAttribute(attr, typedValue, true);

        if (typedValue.resourceId != 0) {
            return androidx.core.content.ContextCompat.getColor(this, typedValue.resourceId);
        }

        return typedValue.data;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

}

