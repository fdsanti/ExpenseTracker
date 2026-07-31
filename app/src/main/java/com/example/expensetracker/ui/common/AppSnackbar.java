package com.example.expensetracker.ui.common;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.example.expensetracker.R;
import com.google.android.material.snackbar.Snackbar;

public final class AppSnackbar {

    private AppSnackbar() {
    }

    public static void show(Context context, String message) {
        if (context == null) {
            return;
        }

        Activity activity = findActivity(context);
        if (activity == null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            return;
        }

        show(activity, message);
    }

    public static void show(Activity activity, String message) {
        if (activity == null) {
            return;
        }

        View root = activity.findViewById(android.R.id.content);
        if (root == null) {
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
            return;
        }

        Snackbar snackbar = Snackbar.make(root, "", Snackbar.LENGTH_SHORT);
        View snackbarView = snackbar.getView();
        snackbarView.setBackgroundResource(R.drawable.bg_expense_snackbar);
        snackbarView.setPadding(0, 0, 0, 0);
        snackbarView.setElevation(dpToPx(activity, 6));

        ViewGroup.LayoutParams rawParams = snackbarView.getLayoutParams();
        rawParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        rawParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;

        if (rawParams instanceof FrameLayout.LayoutParams) {
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) rawParams;
            params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            params.setMargins(0, 0, 0, dpToPx(activity, 24));
            snackbarView.setLayoutParams(params);
        } else if (rawParams instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) rawParams;
            params.setMargins(0, 0, 0, dpToPx(activity, 24));
            snackbarView.setLayoutParams(params);
        } else {
            snackbarView.setLayoutParams(rawParams);
        }

        if (snackbarView instanceof ViewGroup) {
            ViewGroup snackbarLayout = (ViewGroup) snackbarView;
            snackbarLayout.removeAllViews();
            snackbarLayout.addView(createContent(activity, message));
        }

        snackbar.show();
    }

    private static View createContent(Context context, String message) {
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.HORIZONTAL);
        content.setGravity(Gravity.CENTER_VERTICAL);
        content.setPadding(dpToPx(context, 18), dpToPx(context, 14), dpToPx(context, 20), dpToPx(context, 14));

        ImageView icon = new ImageView(context);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dpToPx(context, 20), dpToPx(context, 20));
        icon.setLayoutParams(iconParams);
        icon.setImageResource(R.drawable.checkcircle);
        icon.setColorFilter(getAttrColor(context, R.attr.expenseSnackbarContentColor));
        content.addView(icon);

        TextView text = new TextView(context);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        textParams.setMarginStart(dpToPx(context, 12));
        text.setLayoutParams(textParams);
        text.setIncludeFontPadding(false);
        text.setText(message);
        text.setTextColor(getAttrColor(context, R.attr.expenseSnackbarContentColor));
        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        text.setTypeface(ResourcesCompat.getFont(context, R.font.rajdhani_semibold));
        content.addView(text);

        return content;
    }

    private static Activity findActivity(Context context) {
        Context cursor = context;
        while (cursor instanceof ContextWrapper) {
            if (cursor instanceof Activity) {
                return (Activity) cursor;
            }
            cursor = ((ContextWrapper) cursor).getBaseContext();
        }
        return null;
    }

    private static int getAttrColor(Context context, int attr) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(attr, typedValue, true);

        if (typedValue.resourceId != 0) {
            return ContextCompat.getColor(context, typedValue.resourceId);
        }

        return typedValue.data;
    }

    private static int dpToPx(Context context, int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }
}
