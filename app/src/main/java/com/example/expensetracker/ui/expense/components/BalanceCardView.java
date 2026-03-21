package com.example.expensetracker.ui.expense.components;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.example.expensetracker.R;
import com.example.expensetracker.calculator.DebtSummary;

public class BalanceCardView extends LinearLayout {

    private TextView txtDebt;
    private Button btnReviewBalance;

    public BalanceCardView(Context context) {
        super(context);
        init(context);
    }

    public BalanceCardView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public BalanceCardView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {

        inflate(context, R.layout.view_balance_card, this);

        txtDebt = findViewById(R.id.txtDebt);
        btnReviewBalance = findViewById(R.id.btnReviewBalance);
    }

    public void render(DebtSummary debtSummary) {

        if (debtSummary != null
                && debtSummary.getFromMemberName() != null
                && debtSummary.getToMemberName() != null
                && debtSummary.getAmount() > 0) {

            txtDebt.setText(
                    debtSummary.getFromMemberName()
                            + " debe "
                            + debtSummary.getAmount()
                            + " a "
                            + debtSummary.getToMemberName()
            );

        } else {
            txtDebt.setText("Debt: -");
        }
    }

    public void setOnReviewBalanceClickListener(OnClickListener listener) {
        btnReviewBalance.setOnClickListener(listener);
    }
}
