package com.example.expensetracker.ui.expense.dialogs;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.expensetracker.R;
import com.example.expensetracker.model.Category;
import com.example.expensetracker.model.Expense;
import com.example.expensetracker.model.Member;
import com.example.expensetracker.ui.expense.ExpenseScreenController;
import com.example.expensetracker.ui.expense.ExpenseScreenState;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class EditExpenseDialog {

    public static void showEdit(
            AppCompatActivity activity,
            ExpenseScreenState state,
            String expenseId,
            ExpenseScreenController controller
    ) {
        Expense expense = findExpenseById(state, expenseId);
        if (expense == null) {
            Toast.makeText(activity, "No se encontró el gasto", Toast.LENGTH_SHORT).show();
            return;
        }

        showInternal(activity, state, controller, expense);
    }

    public static void showCreate(
            AppCompatActivity activity,
            ExpenseScreenState state,
            ExpenseScreenController controller
    ) {
        showInternal(activity, state, controller, null);
    }

    private static void showInternal(
            AppCompatActivity activity,
            ExpenseScreenState state,
            ExpenseScreenController controller,
            @Nullable Expense expense
    ) {
        boolean isEditMode = expense != null;

        View dialogView = LayoutInflater.from(activity)
                .inflate(R.layout.input_newrow, null);

        TextInputEditText txtNombre = dialogView.findViewById(R.id.editText_NombreGasto);
        TextInputEditText txtFecha = dialogView.findViewById(R.id.editText_FechaGasto);
        TextInputEditText txtMonto = dialogView.findViewById(R.id.editText_Gasto);

        AutoCompleteTextView dropdownPersona = dialogView.findViewById(R.id.dropdown_nombres);
        AutoCompleteTextView dropdownCategoria = dialogView.findViewById(R.id.cat_dropdown);
        TextInputLayout inputLayoutWho = dialogView.findViewById(R.id.inputLayout_Who);
        TextInputLayout inputLayoutNombre = dialogView.findViewById(R.id.inputLayout_NombreGasto);
        TextInputLayout inputLayoutFecha = dialogView.findViewById(R.id.inputLayout_FechaGasto);
        TextInputLayout inputLayoutCategoria = dialogView.findViewById(R.id.menu_category);
        TextInputLayout inputLayoutMonto = dialogView.findViewById(R.id.inputLayout_Gasto);

        final long[] selectedDate = {isEditMode ? expense.getDate() : todayAtLocalMidnight()};

        txtNombre.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                clearError(inputLayoutNombre);
            }
        });

        txtMonto.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                clearError(inputLayoutMonto);
            }
        });

        dropdownPersona.setOnItemClickListener((parent, view, position, id) -> {
            clearError(inputLayoutWho);
        });

        dropdownPersona.setOnClickListener(v -> {
            clearError(inputLayoutWho);
        });

        dropdownCategoria.setOnItemClickListener((parent, view, position, id) -> {
            clearError(inputLayoutCategoria);
        });

        dropdownCategoria.setOnClickListener(v -> {
            clearError(inputLayoutCategoria);
        });

        txtFecha.setOnClickListener(v -> {
            clearError(inputLayoutFecha);

            MaterialDatePicker<Long> picker =
                    MaterialDatePicker.Builder.datePicker()
                            .setTitleText("Seleccionar fecha")
                            .setSelection(toUtc(selectedDate[0]))
                            .build();

            picker.addOnPositiveButtonClickListener(selection -> {
                selectedDate[0] = fromUtc(selection);
                txtFecha.setText(formatDate(selectedDate[0]));
                clearError(inputLayoutFecha);
            });

            picker.show(activity.getSupportFragmentManager(), "DATE_PICKER");
        });

        dropdownPersona.setAdapter(new ArrayAdapter<>(
                activity,
                android.R.layout.simple_dropdown_item_1line,
                mapMemberNames(state.members)
        ));

        List<Category> categories = state.categories;

        if (categories == null || categories.isEmpty()) {
            categories = getDefaultCategories();
        }

        dropdownCategoria.setAdapter(new ArrayAdapter<>(
                activity,
                android.R.layout.simple_dropdown_item_1line,
                mapCategoryNames(categories)
        ));

        if (isEditMode) {
            txtNombre.setText(expense.getDescription());
            txtMonto.setText(String.valueOf(expense.getAmount()));
            dropdownPersona.setText(resolveMemberName(expense.getPaidByMemberId(), state.members), false);
            dropdownCategoria.setText(resolveCategoryName(expense.getCategoryId(), categories), false);
        }

        txtFecha.setText(formatDate(selectedDate[0]));
        txtFecha.setFocusable(false);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity)
                .setTitle(isEditMode ? "Editar expense" : "Nuevo expense")
                .setView(dialogView)
                .setNegativeButton("Cancelar", (d, w) -> d.dismiss())
                .setPositiveButton(isEditMode ? "Guardar" : "Crear", null);

        if (isEditMode) {
            builder.setNeutralButton("Eliminar", null);
        }

        AlertDialog dialog = builder.create();

        dialog.show();

        if (isEditMode) {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                new MaterialAlertDialogBuilder(activity)
                        .setTitle("Eliminar expense")
                        .setMessage("¿Querés eliminar este gasto?")
                        .setNegativeButton("Cancelar", (d, w) -> d.dismiss())
                        .setPositiveButton("Eliminar", (d, w) -> {
                            controller.deleteExpense(expense.getId());
                            dialog.dismiss();
                        })
                        .show();
            });
        }

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {

            String description = safe(txtNombre);
            String amountText = safe(txtMonto);
            String memberName = dropdownPersona.getText() != null ? dropdownPersona.getText().toString().trim() : "";
            String categoryName = dropdownCategoria.getText() != null ? dropdownCategoria.getText().toString().trim() : "";

            boolean hasError = false;

            clearError(inputLayoutWho);
            clearError(inputLayoutNombre);
            clearError(inputLayoutFecha);
            clearError(inputLayoutCategoria);
            clearError(inputLayoutMonto);

            if (description.isEmpty()) {
                inputLayoutNombre.setError("Ingresá un nombre");
                hasError = true;
            }

            if (amountText.isEmpty()) {
                inputLayoutMonto.setError("Ingresá un monto");
                hasError = true;
            }

            double amount = 0;
            if (!amountText.isEmpty()) {
                try {
                    amount = Double.parseDouble(amountText);
                } catch (Exception e) {
                    inputLayoutMonto.setError("Monto inválido");
                    hasError = true;
                }
            }

            if (memberName.isEmpty()) {
                inputLayoutWho.setError("Seleccioná una persona");
                hasError = true;
            }

            if (categoryName.isEmpty()) {
                inputLayoutCategoria.setError("Seleccioná una categoría");
                hasError = true;
            }

            if (selectedDate[0] <= 0) {
                inputLayoutFecha.setError("Seleccioná una fecha");
                hasError = true;
            }

            if (hasError) {
                return;
            }

            String memberId = findMemberId(memberName, state.members);

            List<Category> availableCategories = state.categories;
            if (availableCategories == null || availableCategories.isEmpty()) {
                availableCategories = getDefaultCategories();
            }

            String categoryId = findCategoryId(categoryName, availableCategories);

            if (memberId == null) {
                inputLayoutWho.setError("Persona inválida");
                return;
            }

            if (categoryId == null) {
                inputLayoutCategoria.setError("Categoría inválida");
                return;
            }

            if (isEditMode) {
                controller.updateExpense(
                        expense.getId(),
                        description,
                        amount,
                        memberId,
                        categoryId,
                        selectedDate[0]
                );
            } else {
                controller.createExpense(
                        description,
                        amount,
                        memberId,
                        categoryId,
                        selectedDate[0]
                );
            }

            dialog.dismiss();
        });
    }

    private static Expense findExpenseById(ExpenseScreenState state, String id) {
        if (state == null || state.expenses == null) {
            return null;
        }

        for (Expense e : state.expenses) {
            if (e != null && e.getId().equals(id)) {
                return e;
            }
        }
        return null;
    }

    private static List<String> mapMemberNames(List<Member> members) {
        List<String> list = new ArrayList<>();
        if (members == null) {
            return list;
        }

        for (Member m : members) {
            if (m != null) {
                list.add(m.getName());
            }
        }
        return list;
    }

    private static List<String> mapCategoryNames(List<Category> categories) {
        List<String> list = new ArrayList<>();
        if (categories == null) {
            return list;
        }

        for (Category c : categories) {
            if (c != null) {
                list.add(c.getName());
            }
        }
        return list;
    }

    private static String resolveMemberName(String id, List<Member> members) {
        if (members == null) {
            return "";
        }

        for (Member m : members) {
            if (m != null && m.getId().equals(id)) {
                return m.getName();
            }
        }
        return "";
    }

    private static String resolveCategoryName(String id, List<Category> categories) {
        if (categories == null) {
            return "";
        }

        for (Category c : categories) {
            if (c != null && c.getId().equals(id)) {
                return c.getName();
            }
        }
        return "";
    }

    private static String findMemberId(String name, List<Member> members) {
        if (members == null) {
            return null;
        }

        for (Member m : members) {
            if (m != null && m.getName().equals(name)) {
                return m.getId();
            }
        }
        return null;
    }

    private static String findCategoryId(String name, List<Category> categories) {
        if (categories == null) {
            return null;
        }

        for (Category c : categories) {
            if (c != null && c.getName().equals(name)) {
                return c.getId();
            }
        }
        return null;
    }

    private static String safe(TextInputEditText t) {
        return t.getText() != null ? t.getText().toString().trim() : "";
    }

    private static String formatDate(long ts) {
        return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                .format(new Date(ts));
    }

    private static long todayAtLocalMidnight() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private static long toUtc(long local) {
        Calendar l = Calendar.getInstance();
        l.setTimeInMillis(local);

        Calendar u = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        u.clear();
        u.set(
                l.get(Calendar.YEAR),
                l.get(Calendar.MONTH),
                l.get(Calendar.DAY_OF_MONTH),
                0,
                0,
                0
        );
        return u.getTimeInMillis();
    }

    private static long fromUtc(long utc) {
        Calendar u = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        u.setTimeInMillis(utc);

        Calendar l = Calendar.getInstance();
        l.clear();
        l.set(
                u.get(Calendar.YEAR),
                u.get(Calendar.MONTH),
                u.get(Calendar.DAY_OF_MONTH),
                0,
                0,
                0
        );
        return l.getTimeInMillis();
    }
    private static void clearError(TextInputLayout inputLayout) {
        inputLayout.setError(null);
        inputLayout.setErrorEnabled(false);
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    }

    private static List<Category> getDefaultCategories() {
        List<Category> list = new ArrayList<>();

        list.add(new Category("supermercado", "Supermercado", true, 0));
        list.add(new Category("delivery", "Delivery", true, 1));
        list.add(new Category("nafta_peajes", "Nafta / Peajes", true, 2));
        list.add(new Category("salidas", "Salidas", true, 3));
        list.add(new Category("servicios", "Servicios", true, 4));
        list.add(new Category("suscripciones", "Suscripciones", true, 5));
        list.add(new Category("auto", "Auto", true, 6));
        list.add(new Category("pago_casa", "Pago Casa", true, 7));
        list.add(new Category("compras", "Compras", true, 8));
        list.add(new Category("gatitas", "Gatitas", true, 9));
        list.add(new Category("otros", "Otros", true, 10));

        return list;
    }
}