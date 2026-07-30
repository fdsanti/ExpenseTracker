package com.example.expensetracker.ui.common;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.expensetracker.R;
import com.google.android.material.textfield.TextInputEditText;

public final class AppDialog {

    public enum ActionStyle {
        PRIMARY,
        DESTRUCTIVE
    }

    public interface TextInputCallback {
        void onConfirm(String value);
    }

    public interface TextInputValidator {
        @Nullable
        String validate(String value);
    }

    private AppDialog() {
    }

    public static void showConfirmation(
            Context context,
            String title,
            String message,
            String primaryText,
            ActionStyle actionStyle,
            Runnable onConfirm
    ) {
        showConfirmation(context, title, message, primaryText, actionStyle, onConfirm, null);
    }

    public static void showConfirmation(
            Context context,
            String title,
            String message,
            String primaryText,
            ActionStyle actionStyle,
            Runnable onConfirm,
            @Nullable Runnable onCancel
    ) {
        Dialog dialog = createDialog(context);
        View content = inflateContent(context);

        TextView titleView = content.findViewById(R.id.txtCategoryDialogTitle);
        TextView messageView = content.findViewById(R.id.txtCategoryDialogMessage);
        TextInputEditText input = content.findViewById(R.id.editCategoryDialogName);
        TextView secondaryButton = content.findViewById(R.id.btnCategoryDialogSecondary);
        TextView primaryButton = content.findViewById(R.id.btnCategoryDialogPrimary);

        titleView.setText(title);
        messageView.setText(message);
        messageView.setVisibility(View.VISIBLE);
        input.setVisibility(View.GONE);
        primaryButton.setText(primaryText);
        applyActionStyle(context, primaryButton, actionStyle);

        secondaryButton.setOnClickListener(v -> {
            dialog.dismiss();
            if (onCancel != null) {
                onCancel.run();
            }
        });
        primaryButton.setOnClickListener(v -> {
            dialog.dismiss();
            onConfirm.run();
        });

        showDialog(dialog, content, context);
    }

    public static void showTextInput(
            Context context,
            String title,
            String initialValue,
            String emptyErrorMessage,
            TextInputCallback onConfirm
    ) {
        showTextInput(context, title, initialValue, "", "Guardar", emptyErrorMessage, null, onConfirm);
    }

    public static void showTextInput(
            Context context,
            String title,
            String initialValue,
            String placeholder,
            String primaryText,
            String emptyErrorMessage,
            @Nullable TextInputValidator validator,
            TextInputCallback onConfirm
    ) {
        Dialog dialog = createDialog(context);
        View content = inflateContent(context);

        TextView titleView = content.findViewById(R.id.txtCategoryDialogTitle);
        TextView messageView = content.findViewById(R.id.txtCategoryDialogMessage);
        TextInputEditText input = content.findViewById(R.id.editCategoryDialogName);
        TextView secondaryButton = content.findViewById(R.id.btnCategoryDialogSecondary);
        TextView primaryButton = content.findViewById(R.id.btnCategoryDialogPrimary);

        titleView.setText(title);
        messageView.setVisibility(View.GONE);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setSingleLine(true);
        input.setHint(placeholder);
        input.setText(initialValue);
        input.setSelection(input.getText() != null ? input.getText().length() : 0);
        primaryButton.setText(primaryText);
        applyActionStyle(context, primaryButton, ActionStyle.PRIMARY);

        secondaryButton.setOnClickListener(v -> dialog.dismiss());
        primaryButton.setOnClickListener(v -> {
            String value = input.getText() != null ? input.getText().toString().trim() : "";
            if (value.isEmpty()) {
                Toast.makeText(context, emptyErrorMessage, Toast.LENGTH_SHORT).show();
                return;
            }

            if (validator != null) {
                String validationError = validator.validate(value);
                if (validationError != null && !validationError.isEmpty()) {
                    Toast.makeText(context, validationError, Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            dialog.dismiss();
            onConfirm.onConfirm(value);
        });

        showDialog(dialog, content, context);
    }

    private static Dialog createDialog(Context context) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    private static View inflateContent(Context context) {
        return LayoutInflater.from(context).inflate(R.layout.dialog_category_editor, null, false);
    }

    private static void applyActionStyle(Context context, TextView button, ActionStyle actionStyle) {
        if (actionStyle == ActionStyle.DESTRUCTIVE) {
            button.setBackgroundResource(R.drawable.bg_category_dialog_destructive_button);
            button.setTextColor(getAttrColor(context, R.attr.categoryDialogDestructiveText));
            return;
        }

        button.setBackgroundResource(R.drawable.bg_debt_closing_button_shadow);
        button.setTextColor(getAttrColor(context, R.attr.debtClosingButtonTextColor));
    }

    private static void showDialog(Dialog dialog, View content, Context context) {
        dialog.setContentView(content);
        dialog.show();

        Window window = dialog.getWindow();
        if (window == null) {
            return;
        }

        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams();
        params.copyFrom(window.getAttributes());
        params.width = context.getResources().getDisplayMetrics().widthPixels - dpToPx(context, 40);
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        window.setAttributes(params);
    }

    private static int getAttrColor(Context context, int attr) {
        android.util.TypedValue typedValue = new android.util.TypedValue();
        context.getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }

    private static int dpToPx(Context context, int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }
}
