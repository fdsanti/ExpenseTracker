package com.example.expensetracker.ui.expense.components;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.example.expensetracker.R;
import com.example.expensetracker.calculator.ExpenseSummary;
import com.example.expensetracker.calculator.MemberExpenseSummary;
import com.example.expensetracker.calculator.TotalTrend;
import com.example.expensetracker.model.Tracker;
import com.example.expensetracker.ui.expense.TrackerDateUtils;
import java.text.NumberFormat;
import java.util.Locale;

public class SummaryCardView extends LinearLayout {
    private TextView txtTotalLabel;
    private TextView txtTotalAmount;
    private TextView txtTotalTrend;
    private View totalTrendBadge;
    private ImageView ivTotalTrend;
    private TextView txtMember1Meta;
    private TextView txtMember1Amount;
    private TextView txtMember2Meta;
    private TextView txtMember2Amount;
    private View totalSummaryCard;
    private View memberCard1;
    private View memberCard2;
    private ExpenseSummary expenseSummary;
    private MemberExpenseSummary memberSummary1;
    private MemberExpenseSummary memberSummary2;

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
        txtTotalTrend = findViewById(R.id.txtTotalTrend);
        totalTrendBadge = findViewById(R.id.totalTrendBadge);
        ivTotalTrend = findViewById(R.id.ivTotalTrend);
        txtMember1Meta = findViewById(R.id.txtMember1Meta);
        txtMember1Amount = findViewById(R.id.txtMember1Amount);
        txtMember2Meta = findViewById(R.id.txtMember2Meta);
        txtMember2Amount = findViewById(R.id.txtMember2Amount);
        totalSummaryCard = findViewById(R.id.totalSummaryCard);
        memberCard1 = findViewById(R.id.memberCard1);
        memberCard2 = findViewById(R.id.memberCard2);

        totalSummaryCard.setOnClickListener(v -> showTotalSummaryDialog());
        memberCard1.setOnClickListener(v -> showMemberSummaryDialog(memberSummary1));
        memberCard2.setOnClickListener(v -> showMemberSummaryDialog(memberSummary2));
    }

    public void render(ExpenseSummary expenseSummary) {
        render(expenseSummary, null, null);
    }

    public void render(ExpenseSummary expenseSummary, @Nullable Tracker tracker) {
        render(expenseSummary, tracker, null);
    }

    public void render(ExpenseSummary expenseSummary, @Nullable Tracker tracker, @Nullable TotalTrend totalTrend) {
        boolean closedTracker = tracker != null && tracker.isClosed();
        int trackerDay = closedTracker ? 0 : TrackerDateUtils.getTrackerDay(tracker);
        if (trackerDay > 0) {
            txtTotalLabel.setText("Gastos totales: D\u00eda " + trackerDay);
        } else {
            txtTotalLabel.setText("Gastos totales:");
        }

        if (expenseSummary == null) {
            this.expenseSummary = null;
            memberSummary1 = null;
            memberSummary2 = null;
            txtTotalAmount.setText("-");
            renderTotalTrend(null);
            txtMember1Meta.setText("-  |  0%");
            txtMember1Amount.setText("");
            txtMember2Meta.setText("-  |  0%");
            txtMember2Amount.setText("");
            return;
        }

        this.expenseSummary = expenseSummary;
        double total = expenseSummary.getTotalAmount();
        txtTotalAmount.setText(formatCurrency(total));
        renderTotalTrend(totalTrend);

        if (expenseSummary.getMemberSummaries() == null || expenseSummary.getMemberSummaries().isEmpty()) {
            memberSummary1 = null;
            memberSummary2 = null;
            txtMember1Meta.setText("-  |  0%");
            txtMember1Amount.setText("");
            txtMember2Meta.setText("-  |  0%");
            txtMember2Amount.setText("");
            return;
        }

        MemberExpenseSummary m1 = expenseSummary.getMemberSummaries().size() > 0
                ? expenseSummary.getMemberSummaries().get(0)
                : null;

        MemberExpenseSummary m2 = expenseSummary.getMemberSummaries().size() > 1
                ? expenseSummary.getMemberSummaries().get(1)
                : null;
        memberSummary1 = m1;
        memberSummary2 = m2;

        double amount1 = m1 != null ? m1.getAmount() : 0d;
        double amount2 = m2 != null ? m2.getAmount() : 0d;
        double totalAmount = amount1 + amount2;

        if (m1 != null) {
            txtMember1Meta.setText(formatMemberMeta(m1.getMemberName(), amount1, totalAmount));
            txtMember1Amount.setText(formatCurrency(amount1));
        } else {
            txtMember1Meta.setText("-  |  0%");
            txtMember1Amount.setText("");
        }

        if (m2 != null) {
            txtMember2Meta.setText(formatMemberMeta(m2.getMemberName(), amount2, totalAmount));
            txtMember2Amount.setText(formatCurrency(amount2));
        } else {
            txtMember2Meta.setText("-  |  0%");
            txtMember2Amount.setText("");
        }
    }

    private void showTotalSummaryDialog() {
        if (expenseSummary == null) {
            return;
        }

        double groupAmount = 0d;
        double individualAmount = 0d;

        if (expenseSummary.getMemberSummaries() != null) {
            for (MemberExpenseSummary memberSummary : expenseSummary.getMemberSummaries()) {
                if (memberSummary == null) {
                    continue;
                }

                groupAmount += memberSummary.getGroupAmount();
                individualAmount += memberSummary.getIndividualAmount();
            }
        }

        showExpenseSummaryDialog(
                "Gastos totales",
                expenseSummary.getTotalAmount(),
                groupAmount,
                individualAmount
        );
    }

    private void showMemberSummaryDialog(@Nullable MemberExpenseSummary memberSummary) {
        if (memberSummary == null) {
            return;
        }

        showExpenseSummaryDialog(
                memberSummary.getMemberName(),
                memberSummary.getAmount(),
                memberSummary.getGroupAmount(),
                memberSummary.getIndividualAmount()
        );
    }

    private void showExpenseSummaryDialog(
            String title,
            double totalAmount,
            double groupAmount,
            double individualAmount
    ) {
        Dialog dialog = new Dialog(getContext(), R.style.ExpenseTrackerDialogNoWindowAnimation);
        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_member_expense_summary, null);
        prepareDialogEnterAnimation(dialogView);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(dialogView);

        TextView txtName = dialogView.findViewById(R.id.txtMemberDialogName);
        TextView txtTotal = dialogView.findViewById(R.id.txtMemberDialogTotal);
        TextView txtGroupAmount = dialogView.findViewById(R.id.txtGroupAmount);
        TextView txtGroupPercentage = dialogView.findViewById(R.id.txtGroupPercentage);
        TextView txtIndividualAmount = dialogView.findViewById(R.id.txtIndividualAmount);
        TextView txtIndividualPercentage = dialogView.findViewById(R.id.txtIndividualPercentage);
        ImageButton closeButton = dialogView.findViewById(R.id.btnMemberDialogClose);

        txtName.setText(title);
        txtTotal.setText("Gastos: " + formatCurrency(totalAmount));
        txtGroupAmount.setText(formatCurrency(groupAmount));
        txtGroupPercentage.setText(formatPercentage(groupAmount, totalAmount));
        txtIndividualAmount.setText(formatCurrency(individualAmount));
        txtIndividualPercentage.setText(formatPercentage(individualAmount, totalAmount));
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

            dialogView.post(() -> playDialogEnterAnimation(dialogView));
        });
        dialog.show();
    }

    private void prepareDialogEnterAnimation(View dialogView) {
        dialogView.setAlpha(0f);
        dialogView.setScaleX(0.96f);
        dialogView.setScaleY(0.96f);
        dialogView.setTranslationY(dpToPx(12));
    }

    private void playDialogEnterAnimation(View dialogView) {
        dialogView.animate().cancel();
        dialogView.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .translationY(0f)
                .setDuration(180L)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private void renderTotalTrend(@Nullable TotalTrend totalTrend) {
        if (totalTrend == null || totalTrendBadge == null || ivTotalTrend == null) {
            if (totalTrendBadge != null) {
                totalTrendBadge.setVisibility(View.GONE);
            }
            return;
        }

        boolean up = totalTrend.getDirection() == TotalTrend.Direction.UP;
        totalTrendBadge.setVisibility(View.VISIBLE);
        totalTrendBadge.setBackgroundResource(up ? R.drawable.bg_total_trend_up : R.drawable.bg_total_trend_down);
        ivTotalTrend.setImageResource(up ? R.drawable.trendingup : R.drawable.trendingdown);
        txtTotalTrend.setText(totalTrend.getPercentage() + "%");
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

    private String formatMemberMeta(String name, double amount, double totalAmount) {
        return name + "  |  " + formatPercentage(amount, totalAmount);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
