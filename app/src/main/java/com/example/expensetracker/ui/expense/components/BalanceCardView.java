package com.example.expensetracker.ui.expense.components;

import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.example.expensetracker.R;
import com.example.expensetracker.calculator.BalanceDetail;
import com.example.expensetracker.model.Tracker;
import com.example.expensetracker.ui.expense.TrackerDateUtils;

import java.text.NumberFormat;
import java.util.Locale;

public class BalanceCardView extends LinearLayout {

    private TextView txtDebtTitle;
    private TextView txtDebtLabel;
    private TextView txtDebtAmount;
    private TextView btnCloseExpense;
    private ImageView ivBalanceExpand;
    private View balanceCardContainer;
    @Nullable
    private BalanceDetail currentBalanceDetail;
    @Nullable
    private OnClickListener closeTrackerClickListener;

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

        txtDebtTitle = findViewById(R.id.txtDebtTitle);
        txtDebtLabel = findViewById(R.id.txtDebtLabel);
        txtDebtAmount = findViewById(R.id.txtDebtAmount);
        btnCloseExpense = findViewById(R.id.btnCloseExpense);
        ivBalanceExpand = findViewById(R.id.ivBalanceExpand);
        balanceCardContainer = findViewById(R.id.balanceCardContainer);
        balanceCardContainer.setOnClickListener(v -> showBalanceDetailDialog());
        btnCloseExpense.setOnClickListener(v -> {
            if (closeTrackerClickListener != null) {
                closeTrackerClickListener.onClick(v);
            }
        });
    }

    public void render(@Nullable BalanceDetail balanceDetail) {
        render(balanceDetail, null);
    }

    public void render(@Nullable BalanceDetail balanceDetail, @Nullable Tracker tracker) {
        currentBalanceDetail = balanceDetail;
        boolean closingVariant = TrackerDateUtils.shouldShowClosingVariant(tracker);
        applyVariant(closingVariant);

        if (hasDebt(balanceDetail)) {
            txtDebtLabel.setText(
                    balanceDetail.getDebtorName()
                            + " le debe a "
                            + balanceDetail.getCreditorName()
            );
            txtDebtAmount.setText(formatCurrency(balanceDetail.getDebtAmount()));
        } else {
            txtDebtLabel.setText("No hay deuda");
            txtDebtAmount.setText(formatCurrency(0));
        }
    }

    public void setOnCloseTrackerClickListener(OnClickListener listener) {
        closeTrackerClickListener = listener;
    }

    public void setOnReviewBalanceClickListener(OnClickListener listener) {
        // Kept for compatibility with the old activity wiring. This card now opens its own detail modal.
    }

    private void applyVariant(boolean closingVariant) {
        int textColorAttr = closingVariant
                ? R.attr.debtClosingTextColor
                : R.attr.expensePrimaryTextColor;
        int iconColorAttr = closingVariant
                ? R.attr.debtClosingTextColor
                : R.attr.trackerSummaryLabelColor;
        int textColor = getAttrColor(textColorAttr);
        int iconColor = getAttrColor(iconColorAttr);

        balanceCardContainer.setBackgroundResource(
                closingVariant
                        ? R.drawable.bg_balance_card_closing_shadow
                        : R.drawable.bg_balance_card_shadow
        );
        btnCloseExpense.setVisibility(closingVariant ? VISIBLE : GONE);
        txtDebtTitle.setTextColor(textColor);
        txtDebtLabel.setTextColor(textColor);
        txtDebtAmount.setTextColor(textColor);
        ivBalanceExpand.setImageTintList(ColorStateList.valueOf(iconColor));
    }

    private void showBalanceDetailDialog() {
        BalanceDetail detail = currentBalanceDetail;
        if (detail == null) {
            return;
        }

        Dialog dialog = new Dialog(getContext());
        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_balance_detail, null);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(dialogView);

        ImageButton closeButton = dialogView.findViewById(R.id.btnBalanceDialogClose);
        TextView txtMember1Name = dialogView.findViewById(R.id.txtBalanceMember1Name);
        TextView txtMember2Name = dialogView.findViewById(R.id.txtBalanceMember2Name);
        TextView txtDebtLabel = dialogView.findViewById(R.id.txtBalanceDebtLabel);
        TextView txtDebtAmount = dialogView.findViewById(R.id.txtBalanceDebtAmount);

        txtMember1Name.setText(detail.getMember1Name());
        txtMember2Name.setText(detail.getMember2Name());

        bindDetailRow(
                dialogView.findViewById(R.id.rowMember1Spent),
                "Gastos",
                formatCurrency(detail.getMember1TotalSpent())
        );
        bindDetailRow(
                dialogView.findViewById(R.id.rowMember1Proportional),
                "Correspondería (" + detail.getMember1Percentage() + "%)",
                formatCurrency(detail.getMember1Proportional())
        );
        bindDetailRow(
                dialogView.findViewById(R.id.rowMember1Balance),
                "Balance",
                formatSignedCurrency(detail.getMember1Balance())
        );
        bindDetailRow(
                dialogView.findViewById(R.id.rowMember2Spent),
                "Gastos",
                formatCurrency(detail.getMember2TotalSpent())
        );
        bindDetailRow(
                dialogView.findViewById(R.id.rowMember2Proportional),
                "Correspondería (" + detail.getMember2Percentage() + "%)",
                formatCurrency(detail.getMember2Proportional())
        );
        bindDetailRow(
                dialogView.findViewById(R.id.rowMember2Balance),
                "Balance",
                formatSignedCurrency(detail.getMember2Balance())
        );

        if (hasDebt(detail)) {
            txtDebtLabel.setText(detail.getDebtorName() + " le debe a " + detail.getCreditorName());
            txtDebtAmount.setText(formatCurrency(detail.getDebtAmount()));
        } else {
            txtDebtLabel.setText("No hay deuda");
            txtDebtAmount.setText(formatCurrency(0));
        }

        closeButton.setOnClickListener(v -> dialog.dismiss());

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        dialog.setOnShowListener(dialogInterface -> {
            Window shownWindow = dialog.getWindow();
            if (shownWindow != null) {
                shownWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                shownWindow.setDimAmount(0.60f);
                int dialogWidth = getResources().getDisplayMetrics().widthPixels - dpToPx(40);
                shownWindow.setLayout(
                        dialogWidth,
                        WindowManager.LayoutParams.WRAP_CONTENT
                );
            }
        });
        dialog.show();
    }

    private void bindDetailRow(View row, String label, String value) {
        TextView labelView = row.findViewById(R.id.txtBalanceDetailRowLabel);
        TextView valueView = row.findViewById(R.id.txtBalanceDetailRowValue);

        labelView.setText(label);
        valueView.setText(value);
    }

    private boolean hasDebt(@Nullable BalanceDetail detail) {
        return detail != null
                && detail.getDebtAmount() > 0
                && detail.getDebtorName() != null
                && !detail.getDebtorName().isEmpty()
                && detail.getCreditorName() != null
                && !detail.getCreditorName().isEmpty();
    }

    private String formatCurrency(double amount) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
        return format.format(Math.abs(amount));
    }

    private String formatSignedCurrency(double amount) {
        if (amount < 0) {
            return "- " + formatCurrency(amount);
        }

        return formatCurrency(amount);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private int getAttrColor(int attr) {
        TypedValue typedValue = new TypedValue();
        getContext().getTheme().resolveAttribute(attr, typedValue, true);

        if (typedValue.resourceId != 0) {
            return androidx.core.content.ContextCompat.getColor(getContext(), typedValue.resourceId);
        }

        return typedValue.data;
    }
}
