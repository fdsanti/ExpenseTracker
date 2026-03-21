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


public class ExpenseActivityV2 extends AppCompatActivity implements ExpenseScreenListener {

    private ExpenseScreenController controller;
    private TextView txtTrackerName;
    private SummaryCardView summaryCard;
    private TextView txtLoading;
    private BalanceCardView balanceCard;
    private MembersCardView membersCard;
    private ContentCardView contentCard;
    private View btnEditTrackerName;
    private View btnBack;
    private View btnSettingsTop;
    private View btnCloseTracker;
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
        btnSettingsTop = findViewById(R.id.btnSettingsTop);
        btnEditTrackerName = findViewById(R.id.btnEditTrackerName);
        btnCloseTracker = findViewById(R.id.btnCloseTracker);

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

        btnSettingsTop.setOnClickListener(v -> {
            if (currentState == null || currentState.tracker == null || currentState.tracker.getId() == null) {
                Toast.makeText(this, "No se pudo abrir configuración", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(this, SettingsActivity.class);
            intent.putExtra("trackerId", currentState.tracker.getId());
            intent.putExtra("fromExpenseV2", true);
            startActivity(intent);
        });

        btnEditTrackerName.setOnClickListener(v -> {
            if (currentState == null || currentState.tracker == null) {
                return;
            }

            String currentName = currentState.tracker.getName();

            final android.widget.EditText input = new android.widget.EditText(this);
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

                        // 🔥 actualizar cache local de Home
                        HCardDB.setName(currentState.tracker.getId(), newName);
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });

        btnCloseTracker.setOnClickListener(v -> {
            if (currentState == null || currentState.tracker == null || currentState.tracker.getId() == null) {
                Toast.makeText(this, "No se pudo actualizar el tracker", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean currentlyClosed = currentState.tracker.isClosed();
            String title = currentlyClosed ? "Abrir Reporte" : "Cerrar Reporte";
            String message = currentlyClosed
                    ? "¿Estás seguro que querés abrir el reporte nuevamente?"
                    : "¿Estás seguro que querés cerrar el reporte? Esto deshabilitará las funcionalidades del mismo.";

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
                                newClosedValue ? "Expense Cerrado" : "Expense Abierto",
                                Toast.LENGTH_SHORT
                        ).show();

                        finish();
                    })
                    .show();
        });

        FloatingActionButton fab = findViewById(R.id.fabAddExpense);

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
        if (state.tracker != null && btnCloseTracker instanceof android.widget.Button) {
            ((android.widget.Button) btnCloseTracker).setText(
                    state.tracker.isClosed() ? "Abrir Expense" : "Cerrar Expense"
            );
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

}

