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
    private TextView txtMember2Name;
    private TextView txtMember2Amount;
    private View barMember1;
    private View barMember2;

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
        txtMember2Name = findViewById(R.id.txtMember2Name);
        txtMember2Amount = findViewById(R.id.txtMember2Amount);
        barMember1 = findViewById(R.id.barMember1);
        barMember2 = findViewById(R.id.barMember2);
    }

    public void render(ExpenseSummary expenseSummary) {
        txtTotalLabel.setText("Total");

        if (expenseSummary == null) {
            txtTotalAmount.setText("-");
            txtMember1Name.setText("-");
            txtMember1Amount.setText("");
            txtMember2Name.setText("-");
            txtMember2Amount.setText("");

            barMember1.setLayoutParams(new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f));
            barMember2.setLayoutParams(new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f));
            return;
        }

        double total = expenseSummary.getTotalAmount();
        txtTotalAmount.setText(formatCurrency(total));

        if (expenseSummary.getMemberSummaries() == null || expenseSummary.getMemberSummaries().isEmpty()) {
            txtMember1Name.setText("-");
            txtMember1Amount.setText("");
            txtMember2Name.setText("-");
            txtMember2Amount.setText("");

            barMember1.setLayoutParams(new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f));
            barMember2.setLayoutParams(new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f));
            return;
        }

        MemberExpenseSummary m1 = expenseSummary.getMemberSummaries().size() > 0
                ? expenseSummary.getMemberSummaries().get(0)
                : null;

        MemberExpenseSummary m2 = expenseSummary.getMemberSummaries().size() > 1
                ? expenseSummary.getMemberSummaries().get(1)
                : null;

        if (m1 != null) {
            txtMember1Name.setText(m1.getMemberName() + ":");
            txtMember1Amount.setText(formatCurrency(m1.getAmount()));
        } else {
            txtMember1Name.setText("-");
            txtMember1Amount.setText("");
        }

        if (m2 != null) {
            txtMember2Name.setText(m2.getMemberName() + ":");
            txtMember2Amount.setText(formatCurrency(m2.getAmount()));
        } else {
            txtMember2Name.setText("-");
            txtMember2Amount.setText("");
        }

        double amount1 = m1 != null ? m1.getAmount() : 0d;
        double amount2 = m2 != null ? m2.getAmount() : 0d;
        double totalAmount = amount1 + amount2;

        if (totalAmount <= 0d) {
            barMember1.setLayoutParams(new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f));
            barMember2.setLayoutParams(new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f));
            return;
        }

        float p1 = (float) (amount1 / totalAmount);
        float p2 = (float) (amount2 / totalAmount);

        barMember1.setLayoutParams(new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, p1));
        barMember2.setLayoutParams(new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, p2));
    }

    private String formatCurrency(double amount) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
        return format.format(amount);
    }
}