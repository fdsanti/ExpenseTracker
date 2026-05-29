package com.example.expensetracker.ui.expense.components;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.example.expensetracker.R;
import com.example.expensetracker.calculator.ExpenseSummary;
import com.example.expensetracker.calculator.MemberExpenseSummary;
import java.text.NumberFormat;
import java.util.Locale;

public class SummaryCardView extends LinearLayout {

    private TextView txtTotalLabel;
    private TextView txtTotalAmount;
    private TextView txtMember1Name;
    private TextView txtMember1Amount;
    private TextView txtMember1Percentage;
    private TextView txtMember2Name;
    private TextView txtMember2Amount;
    private TextView txtMember2Percentage;

    public SummaryCardView(Context context) {
        super(context);
        init(context);
    }

    public SummaryCardView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SummaryCardView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        inflate(context, R.layout.view_summary_card, this);

        txtTotalLabel = findViewById(R.id.txtTotalLabel);
        txtTotalAmount = findViewById(R.id.txtTotalAmount);
        txtMember1Name = findViewById(R.id.txtMember1Name);
        txtMember1Amount = findViewById(R.id.txtMember1Amount);
        txtMember1Percentage = findViewById(R.id.txtMember1Percentage);
        txtMember2Name = findViewById(R.id.txtMember2Name);
        txtMember2Amount = findViewById(R.id.txtMember2Amount);
        txtMember2Percentage = findViewById(R.id.txtMember2Percentage);
    }

    public void render(ExpenseSummary expenseSummary) {
        txtTotalLabel.setText("Gastos totales");

        if (expenseSummary == null) {
            txtTotalAmount.setText("-");
            txtMember1Name.setText("-");
            txtMember1Amount.setText("");
            txtMember1Percentage.setText("0%");
            txtMember2Name.setText("-");
            txtMember2Amount.setText("");
            txtMember2Percentage.setText("0%");
            return;
        }

        double total = expenseSummary.getTotalAmount();
        txtTotalAmount.setText(formatCurrency(total));

        if (expenseSummary.getMemberSummaries() == null || expenseSummary.getMemberSummaries().isEmpty()) {
            txtMember1Name.setText("-");
            txtMember1Amount.setText("");
            txtMember1Percentage.setText("0%");
            txtMember2Name.setText("-");
            txtMember2Amount.setText("");
            txtMember2Percentage.setText("0%");
            return;
        }

        MemberExpenseSummary m1 = expenseSummary.getMemberSummaries().size() > 0
                ? expenseSummary.getMemberSummaries().get(0)
                : null;

        MemberExpenseSummary m2 = expenseSummary.getMemberSummaries().size() > 1
                ? expenseSummary.getMemberSummaries().get(1)
                : null;

        if (m1 != null) {
            txtMember1Name.setText(m1.getMemberName());
            txtMember1Amount.setText(formatCurrency(m1.getAmount()));
        } else {
            txtMember1Name.setText("-");
            txtMember1Amount.setText("");
        }

        if (m2 != null) {
            txtMember2Name.setText(m2.getMemberName());
            txtMember2Amount.setText(formatCurrency(m2.getAmount()));
        } else {
            txtMember2Name.setText("-");
            txtMember2Amount.setText("");
        }

        double amount1 = m1 != null ? m1.getAmount() : 0d;
        double amount2 = m2 != null ? m2.getAmount() : 0d;
        double totalAmount = amount1 + amount2;

        txtMember1Percentage.setText(formatPercentage(amount1, totalAmount));
        txtMember2Percentage.setText(formatPercentage(amount2, totalAmount));
    }

    private String formatCurrency(double amount) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
        return format.format(amount);
    }

    private String formatPercentage(double amount, double totalAmount) {
        if (totalAmount <= 0d) {
            return "0%";
        }

        int percentage = (int) Math.round((amount * 100d) / totalAmount);
        return percentage + "%";
    }
}
