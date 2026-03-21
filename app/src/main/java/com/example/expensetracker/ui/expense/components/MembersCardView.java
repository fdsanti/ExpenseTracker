package com.example.expensetracker.ui.expense.components;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.example.expensetracker.R;
import com.example.expensetracker.model.Member;

import java.util.List;

public class MembersCardView extends LinearLayout {

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

        txtMemberSalary1 = findViewById(R.id.txtMemberSalary1);
        txtMemberSalary2 = findViewById(R.id.txtMemberSalary2);
        btnEditMembers = findViewById(R.id.btnEditMembers);
    }

    public void render(List<Member> members) {
        if (members != null && members.size() > 0) {

            if (members.size() > 0) {
                txtMemberSalary1.setText(
                        members.get(0).getName()
                                + " - $ "
                                + members.get(0).getSalary()
                );
            } else {
                txtMemberSalary1.setText("User 1 - $ -");
            }

            if (members.size() > 1) {
                txtMemberSalary2.setText(
                        members.get(1).getName()
                                + " - $ "
                                + members.get(1).getSalary()
                );
            } else {
                txtMemberSalary2.setText("User 2 - $ -");
            }

        } else {
            txtMemberSalary1.setText("User 1 - $ -");
            txtMemberSalary2.setText("User 2 - $ -");
        }
    }

    public void setOnEditMembersClickListener(OnClickListener listener) {
        btnEditMembers.setOnClickListener(listener);
    }
}
