package com.example.expensetracker.ui.expense;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.expensetracker.R;
import com.example.expensetracker.calculator.BalanceDetail;
import com.example.expensetracker.calculator.BalanceDetailCalculator;
import com.example.expensetracker.data.TrackerRepository;
import com.example.expensetracker.model.Expense;
import com.example.expensetracker.model.Member;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class BalanceActivity extends AppCompatActivity {

    public static final String EXTRA_TRACKER_ID = "extra_tracker_id";

    private TextView txtBalance;
    private TextView txtMember1Name;
    private TextView txtMember1TotalSpent;
    private TextView txtMember1Percentage;
    private TextView txtMember1Proportional;
    private TextView txtMember1Balance;
    private TextView txtMember2Name;
    private TextView txtMember2TotalSpent;
    private TextView txtMember2Percentage;
    private TextView txtMember2Proportional;
    private TextView txtMember2Balance;
    private TextView txtTotalExpenses;
    private TrackerRepository trackerRepository;
    private String trackerId;
    private List<Member> members;
    private List<Expense> expenses;
    private int pendingLoads = 0;
    private boolean hasLoadError = false;
    private View btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_balance);

        bindViews();
        btnBack.setOnClickListener(v -> finish());

        trackerRepository = new TrackerRepository();
        trackerId = getIntent().getStringExtra(EXTRA_TRACKER_ID);

        if (trackerId == null || trackerId.trim().isEmpty()) {
            renderEmptyState();
            return;
        }

        loadData();
    }

    private void bindViews() {
        txtBalance = findViewById(R.id.txtBalance);
        txtMember1Name = findViewById(R.id.txtMember1Name);
        txtMember1TotalSpent = findViewById(R.id.txtMember1TotalSpent);
        txtMember1Percentage = findViewById(R.id.txtMember1Percentage);
        txtMember1Proportional = findViewById(R.id.txtMember1Proportional);
        txtMember1Balance = findViewById(R.id.txtMember1Balance);
        txtMember2Name = findViewById(R.id.txtMember2Name);
        txtMember2TotalSpent = findViewById(R.id.txtMember2TotalSpent);
        txtMember2Percentage = findViewById(R.id.txtMember2Percentage);
        txtMember2Proportional = findViewById(R.id.txtMember2Proportional);
        txtMember2Balance = findViewById(R.id.txtMember2Balance);
        txtTotalExpenses = findViewById(R.id.txtTotalExpenses);
        btnBack = findViewById(R.id.btnBack);
    }

    private void loadData() {
        pendingLoads = 2;
        hasLoadError = false;

        trackerRepository.loadParticipants(trackerId, new TrackerRepository.RepositoryCallback<List<Member>>() {
            @Override
            public void onSuccess(List<Member> result) {
                members = result;
                onLoadFinished();
            }

            @Override
            public void onError(Exception exception) {
                Log.e("BalanceActivity", "Error loading participants", exception);
                hasLoadError = true;
                onLoadFinished();
            }
        });

        trackerRepository.loadExpenses(trackerId, new TrackerRepository.RepositoryCallback<List<Expense>>() {
            @Override
            public void onSuccess(List<Expense> result) {
                expenses = result;
                onLoadFinished();
            }

            @Override
            public void onError(Exception exception) {
                Log.e("BalanceActivity", "Error loading expenses", exception);
                hasLoadError = true;
                onLoadFinished();
            }
        });
    }

    private void onLoadFinished() {
        pendingLoads--;

        if (pendingLoads > 0) {
            return;
        }

        if (hasLoadError || members == null || expenses == null) {
            renderEmptyState();
            return;
        }

        BalanceDetail detail = BalanceDetailCalculator.calculate(expenses, members);
        render(detail);
    }

    private void render(BalanceDetail detail) {
        if (detail.getDebtAmount() > 0
                && detail.getDebtorName() != null
                && !detail.getDebtorName().isEmpty()
                && detail.getCreditorName() != null
                && !detail.getCreditorName().isEmpty()) {

            txtBalance.setText(
                    detail.getDebtorName()
                            + " le debe "
                            + formatCurrency(detail.getDebtAmount())
                            + " a "
                            + detail.getCreditorName()
            );
        } else {
            txtBalance.setText("No hay deuda entre participantes");
        }

        txtMember1Name.setText(detail.getMember1Name());
        txtMember1TotalSpent.setText("Total gastado: " + formatCurrency(detail.getMember1TotalSpent()));
        txtMember1Percentage.setText("Porcentaje: " + detail.getMember1Percentage() + "%");
        txtMember1Proportional.setText("Proporcional: " + formatCurrency(detail.getMember1Proportional()));
        txtMember1Balance.setText("Balance: " + formatCurrency(detail.getMember1Balance()));

        txtMember2Name.setText(detail.getMember2Name());
        txtMember2TotalSpent.setText("Total gastado: " + formatCurrency(detail.getMember2TotalSpent()));
        txtMember2Percentage.setText("Porcentaje: " + detail.getMember2Percentage() + "%");
        txtMember2Proportional.setText("Proporcional: " + formatCurrency(detail.getMember2Proportional()));
        txtMember2Balance.setText("Balance: " + formatCurrency(detail.getMember2Balance()));

        txtTotalExpenses.setText("Total de gastos: " + formatCurrency(detail.getTotalExpenses()));
    }

    private void renderEmptyState() {
        txtBalance.setText("Sin datos de balance");
        txtMember1Name.setText("-");
        txtMember1TotalSpent.setText("Total gastado: $ 0,00");
        txtMember1Percentage.setText("Porcentaje: 0%");
        txtMember1Proportional.setText("Proporcional: $ 0,00");
        txtMember1Balance.setText("Balance: $ 0,00");

        txtMember2Name.setText("-");
        txtMember2TotalSpent.setText("Total gastado: $ 0,00");
        txtMember2Percentage.setText("Porcentaje: 0%");
        txtMember2Proportional.setText("Proporcional: $ 0,00");
        txtMember2Balance.setText("Balance: $ 0,00");

        txtTotalExpenses.setText("Total de gastos: $ 0,00");
    }

    private String formatCurrency(double amount) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
        return format.format(amount);
    }
}