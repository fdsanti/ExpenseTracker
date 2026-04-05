package com.example.expensetracker.ui.expense.components;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;

import androidx.annotation.Nullable;

import com.example.expensetracker.R;
import com.example.expensetracker.calculator.DebtSummary;

import java.text.NumberFormat;
import java.util.Locale;

public class BalanceCardView extends LinearLayout {

    private TextView txtDebtLabel;
    private TextView txtDebtAmount;
    private MaterialButton btnReviewBalance;

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

        txtDebtLabel = findViewById(R.id.txtDebtLabel);
        txtDebtAmount = findViewById(R.id.txtDebtAmount);
        btnReviewBalance = findViewById(R.id.btnReviewBalance);
    }

    public void render(DebtSummary debtSummary) {
        if (debtSummary != null
                && debtSummary.getFromMemberName() != null
                && debtSummary.getToMemberName() != null
                && debtSummary.getAmount() > 0) {

            txtDebtLabel.setText(
                    debtSummary.getFromMemberName()
                            + " le debe a "
                            + debtSummary.getToMemberName()
            );

            txtDebtAmount.setText(formatCurrency(debtSummary.getAmount()));
        } else {
            txtDebtLabel.setText("-");
            txtDebtAmount.setText("-");
        }
    }

    public void setOnReviewBalanceClickListener(OnClickListener listener) {
        btnReviewBalance.setOnClickListener(listener);
    }

    private String formatCurrency(double amount) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
        return format.format(amount);
    }
}