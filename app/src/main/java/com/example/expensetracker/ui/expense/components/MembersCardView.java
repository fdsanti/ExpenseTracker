package com.example.expensetracker.ui.expense.components;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.example.expensetracker.R;
import com.example.expensetracker.model.Member;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class MembersCardView extends LinearLayout {

    private TextView txtMemberName1;
    private TextView txtMemberName2;
    private TextView txtMemberSalary1;
    private TextView txtMemberSalary2;
    private Button btnEditMembers;

    public MembersCardView(Context context) {
        super(context);
        init(context);
    }

    public MembersCardView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MembersCardView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        inflate(context, R.layout.view_members_card, this);

        txtMemberName1 = findViewById(R.id.txtMemberName1);
        txtMemberName2 = findViewById(R.id.txtMemberName2);
        txtMemberSalary1 = findViewById(R.id.txtMemberSalary1);
        txtMemberSalary2 = findViewById(R.id.txtMemberSalary2);
        btnEditMembers = findViewById(R.id.btnEditMembers);
    }

    public void render(List<Member> members) {
        if (members == null || members.isEmpty()) {
            renderEmptyState();
            return;
        }

        double salary1 = members.size() > 0 ? members.get(0).getSalary() : 0d;
        double salary2 = members.size() > 1 ? members.get(1).getSalary() : 0d;
        double totalSalary = salary1 + salary2;

        if (members.size() > 0) {
            Member member1 = members.get(0);
            txtMemberName1.setText(
                    safeString(member1.getName()) + " (" + formatPercentage(member1.getSalary(), totalSalary) + ")"
            );
            txtMemberSalary1.setText(formatAmount(member1.getSalary()));
        } else {
            txtMemberName1.setText("User 1");
            txtMemberSalary1.setText("$ -");
        }

        if (members.size() > 1) {
            Member member2 = members.get(1);
            txtMemberName2.setText(
                    safeString(member2.getName()) + " (" + formatPercentage(member2.getSalary(), totalSalary) + ")"
            );
            txtMemberSalary2.setText(formatAmount(member2.getSalary()));
        } else {
            txtMemberName2.setText("User 2");
            txtMemberSalary2.setText("$ -");
        }
    }

    public void setOnEditMembersClickListener(OnClickListener listener) {
        btnEditMembers.setOnClickListener(listener);
    }

    private void renderEmptyState() {
        txtMemberName1.setText("User 1");
        txtMemberSalary1.setText("$ -");
        txtMemberName2.setText("User 2");
        txtMemberSalary2.setText("$ -");
    }

    private String formatAmount(double amount) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
        return format.format(amount);
    }

    private String formatPercentage(double value, double total) {
        if (total <= 0d) {
            return "0%";
        }

        int percentage = (int) Math.round((value * 100d) / total);
        return percentage + "%";
    }

    private String safeString(String value) {
        return value == null ? "" : value;
    }
}