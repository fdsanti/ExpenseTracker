package com.example.expensetracker.ui.expense.components;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.example.expensetracker.R;
import com.example.expensetracker.calculator.ExpenseSummary;
import com.example.expensetracker.calculator.MemberExpenseSummary;

public class SummaryCardView extends LinearLayout {

    private TextView txtTotal;
    private TextView txtMember1;
    private TextView txtMember2;

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

        txtTotal = findViewById(R.id.txtTotal);
        txtMember1 = findViewById(R.id.txtMember1);
        txtMember2 = findViewById(R.id.txtMember2);
    }

    public void render(ExpenseSummary expenseSummary) {
        if (expenseSummary != null) {
            txtTotal.setText("Total: " + expenseSummary.getTotalAmount());
        } else {
            txtTotal.setText("Total: -");
        }

        if (expenseSummary != null
                && expenseSummary.getMemberSummaries() != null
                && expenseSummary.getMemberSummaries().size() > 0) {

            if (expenseSummary.getMemberSummaries().size() > 0) {
                MemberExpenseSummary member1 = expenseSummary.getMemberSummaries().get(0);
                txtMember1.setText(member1.getMemberName() + ": " + member1.getAmount());
            } else {
                txtMember1.setText("User 1: -");
            }

            if (expenseSummary.getMemberSummaries().size() > 1) {
                MemberExpenseSummary member2 = expenseSummary.getMemberSummaries().get(1);
                txtMember2.setText(member2.getMemberName() + ": " + member2.getAmount());
            } else {
                txtMember2.setText("User 2: -");
            }

        } else {
            txtMember1.setText("User 1: -");
            txtMember2.setText("User 2: -");
        }
    }
}
