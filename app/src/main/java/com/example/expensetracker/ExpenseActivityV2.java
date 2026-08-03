package com.example.expensetracker;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.example.expensetracker.calculator.ExpenseListQuery;
import com.example.expensetracker.data.TrackerRepository;
import com.example.expensetracker.data.UserProfileRepository;
import com.example.expensetracker.model.Expense;
import com.example.expensetracker.ui.common.AppDialog;
import com.example.expensetracker.ui.common.AppSnackbar;
import com.example.expensetracker.ui.expense.dialogs.CategoriesBottomSheetDialog;
import com.example.expensetracker.ui.expense.dialogs.EditExpenseDialog;
import com.example.expensetracker.ui.expense.ExpenseScreenController;
import com.example.expensetracker.ui.expense.ExpenseScreenListener;
import com.example.expensetracker.ui.expense.ExpenseScreenState;
import com.example.expensetracker.ui.expense.TrackerDateUtils;
import android.view.View;
import android.widget.TextView;

import com.example.expensetracker.ui.expense.ExpenseUiMapper;
import com.example.expensetracker.ui.expense.components.SummaryCardView;
import com.example.expensetracker.ui.expense.components.BalanceCardView;
import com.example.expensetracker.ui.expense.components.MembersCardView;
import com.example.expensetracker.ui.expense.components.ContentCardView;
import com.example.expensetracker.ui.expense.dialogs.ExpenseBottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.animation.AnimationUtils;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import java.util.HashMap;
import java.util.Map;


public class ExpenseActivityV2 extends AppCompatActivity implements ExpenseScreenListener {
    private ExpenseScreenController controller;
    private TextView txtTrackerName;
    private SummaryCardView summaryCard;
    private TextView txtLoading;
    private BalanceCardView balanceCard;
    private MembersCardView membersCard;
    private ContentCardView contentCard;
    private LinearLayout trackerCardsContainer;
    private LinearLayout expenseContentContainer;
    private View expenseSkeletonContainer;
    private View btnBack;
    private View btnMoreOptions;
    private View fabAddExpense;
    private ExpenseScreenState currentState;
    private ExpenseScreenState previousRenderedState;
    private boolean hasRenderedExpenseContent;
    private boolean salarySetupCheckStarted;
    private boolean salarySetupSaving;
    private String currentNickname = "";
    private String initialTrackerName = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense_v2);
        txtTrackerName = findViewById(R.id.txtTrackerName);
        txtLoading = findViewById(R.id.txtLoading);
        summaryCard = findViewById(R.id.summaryCard);
        balanceCard = findViewById(R.id.balanceCard);
        membersCard = findViewById(R.id.membersCard);
        contentCard = findViewById(R.id.contentCard);
        trackerCardsContainer = findViewById(R.id.trackerCardsContainer);
        expenseContentContainer = findViewById(R.id.expenseContentContainer);
        expenseSkeletonContainer = findViewById(R.id.expenseSkeletonContainer);
        btnBack = findViewById(R.id.btnBack);
        btnMoreOptions = findViewById(R.id.btnMoreOptions);

        membersCard.setOnEditMembersClickListener(v -> {
            if (currentState == null || currentState.tracker == null || currentState.tracker.getId() == null) {
                AppSnackbar.show(this, "No se pudo abrir configuración");
                return;
            }

            showEditSalaryDialog();
        });

        contentCard.setOnCategoriesClickListener(v ->
                controller.setSelectedTab(ExpenseScreenState.ContentTab.CATEGORIES)
        );

        contentCard.setOnExpensesClickListener(v ->
                controller.setSelectedTab(ExpenseScreenState.ContentTab.EXPENSES)
        );

        contentCard.setOnCategoryClickListener(categoryId ->
                controller.toggleCategoryExpanded(categoryId)
        );

        contentCard.setOnExpenseClickListener(expenseId -> {
            if (currentState == null || currentState.expenses == null) return;

            for (Expense e : currentState.expenses) {
                if (e != null && e.getId().equals(expenseId)) {

                    ExpenseBottomSheetDialog dialog =
                            ExpenseBottomSheetDialog.newEditInstance(currentState, controller, e);

                    dialog.show(getSupportFragmentManager(), "EDIT_EXPENSE");
                    return;
                }
            }
        });

        contentCard.setOnMemberFilterChangeListener(memberId ->
                controller.setMemberFilter(memberId)
        );

        contentCard.setOnTypeFilterChangeListener(typeFilterValue -> {
            ExpenseListQuery.TypeFilter typeFilter = typeFilterValue != null
                    ? ExpenseListQuery.TypeFilter.valueOf(typeFilterValue)
                    : null;

            controller.setTypeFilter(typeFilter);
        });

        contentCard.setOnSortTypeChangeListener(sortTypeValue -> {
            ExpenseListQuery.SortType sortType =
                    ExpenseListQuery.SortType.valueOf(sortTypeValue);

            controller.setSortType(sortType);
        });

        btnBack.setOnClickListener(v -> finish());
        btnMoreOptions.setOnClickListener(v -> showMoreOptionsMenu());
        balanceCard.setOnCloseTrackerClickListener(v -> confirmCloseTracker());

        fabAddExpense = findViewById(R.id.fabAddExpense);

        fabAddExpense.setOnClickListener(v -> {
            ExpenseBottomSheetDialog dialog =
                    ExpenseBottomSheetDialog.newCreateInstance(currentState, controller);

            dialog.show(getSupportFragmentManager(), "CREATE_EXPENSE");
        });

        String trackerId = getIntent().getStringExtra("trackerId");
        String trackerName = getIntent().getStringExtra("trackerName");
        initialTrackerName = trackerName != null ? trackerName.trim() : "";
        txtTrackerName.setText(initialTrackerName);

        if (trackerId == null) {
            Log.e("ExpenseV2", "trackerId is null");
            finish();
            return;
        }

        TrackerRepository repository = new TrackerRepository();

        controller = new ExpenseScreenController(repository);
        controller.setListener(this);
        controller.setTrackerId(trackerId);

        controller.load();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (controller != null && hasRenderedExpenseContent) {
            controller.refresh();
        }
    }
    @Override
    public void onStateChanged(ExpenseScreenState state) {
        currentState = state;
        render(state);
        promptCurrentUserSalaryIfNeeded(state);
    }
    private void render(ExpenseScreenState state) {
        boolean showInitialSkeleton = shouldShowInitialSkeleton(state);

        renderHeader(state);
        if (showInitialSkeleton) {
            renderSkeleton(true);
            renderContentVisibility(true);
            renderLoading(state, true);
            previousRenderedState = state;
            return;
        }

        boolean revealAfterInitialSkeleton = !hasRenderedExpenseContent
                && !state.loading
                && state.tracker != null;

        renderCards(state);

        if (revealAfterInitialSkeleton) {
            renderLoading(state, true);
            expenseContentContainer.post(() -> {
                renderSkeleton(false);
                renderContentVisibility(false);
                renderLoading(state, false);
            });
        } else {
            renderSkeleton(false);
            renderContentVisibility(false);
            renderLoading(state, false);
        }

        if (!state.loading && state.tracker != null) {
            hasRenderedExpenseContent = true;
        }
        previousRenderedState = state;
    }

    private void renderHeader(ExpenseScreenState state) {
        if (state.tracker != null) {
            txtTrackerName.setText(state.tracker.getName());
        } else {
            txtTrackerName.setText(initialTrackerName);
        }
    }

    private void renderCards(ExpenseScreenState state) {
        positionBalanceCard(TrackerDateUtils.shouldShowClosingVariant(state.tracker));

        boolean tabOnlyChange = previousRenderedState != null
                && previousRenderedState.tracker == state.tracker
                && previousRenderedState.members == state.members
                && previousRenderedState.expenses == state.expenses
                && previousRenderedState.categories == state.categories
                && previousRenderedState.expenseSummary == state.expenseSummary
                && previousRenderedState.totalTrend == state.totalTrend
                && previousRenderedState.balanceDetail == state.balanceDetail
                && previousRenderedState.categorySummary == state.categorySummary
                && previousRenderedState.visibleExpenses == state.visibleExpenses
                && previousRenderedState.selectedMemberFilter == state.selectedMemberFilter
                && previousRenderedState.selectedTypeFilter == state.selectedTypeFilter
                && previousRenderedState.selectedSortType == state.selectedSortType
                && previousRenderedState.selectedTab != state.selectedTab;

        if (!tabOnlyChange) {
            summaryCard.render(state.expenseSummary, state.tracker, state.totalTrend);
            balanceCard.render(state.balanceDetail, state.tracker);
            membersCard.render(state.members);
        }

        contentCard.render(ExpenseUiMapper.toContentCardModel(state));
    }

    private void positionBalanceCard(boolean closingVariant) {
        if (expenseContentContainer == null || balanceCard == null) {
            return;
        }

        int targetIndex = closingVariant
                ? expenseContentContainer.indexOfChild(summaryCard)
                : expenseContentContainer.indexOfChild(contentCard) + 1;
        int currentIndex = expenseContentContainer.indexOfChild(balanceCard);

        if (targetIndex < 0 || currentIndex == targetIndex) {
            return;
        }

        ViewGroup.LayoutParams layoutParams = balanceCard.getLayoutParams();
        expenseContentContainer.removeView(balanceCard);

        if (currentIndex < targetIndex) {
            targetIndex--;
        }

        expenseContentContainer.addView(balanceCard, targetIndex, layoutParams);
    }

    private boolean shouldShowInitialSkeleton(ExpenseScreenState state) {
        return state != null
                && state.loading
                && !hasRenderedExpenseContent;
    }

    private void renderSkeleton(boolean showSkeleton) {
        if (expenseSkeletonContainer == null) {
            return;
        }

        if (showSkeleton) {
            if (expenseSkeletonContainer.getVisibility() != View.VISIBLE) {
                expenseSkeletonContainer.setVisibility(View.VISIBLE);
                expenseSkeletonContainer.startAnimation(
                        AnimationUtils.loadAnimation(this, R.anim.skeleton_pulse)
                );
            }
            return;
        }

        expenseSkeletonContainer.clearAnimation();
        expenseSkeletonContainer.setVisibility(View.GONE);
    }

    private void renderContentVisibility(boolean showSkeleton) {
        if (expenseContentContainer != null) {
            expenseContentContainer.setVisibility(showSkeleton ? View.INVISIBLE : View.VISIBLE);
        }

        if (fabAddExpense != null) {
            fabAddExpense.setVisibility(showSkeleton ? View.INVISIBLE : View.VISIBLE);
        }
    }

    private void renderLoading(ExpenseScreenState state, boolean showSkeleton) {
        txtLoading.setVisibility(state.loading && !showSkeleton ? View.VISIBLE : View.GONE);
    }

    private void promptCurrentUserSalaryIfNeeded(ExpenseScreenState state) {
        if (salarySetupCheckStarted
                || salarySetupSaving
                || state == null
                || state.loading
                || state.tracker == null
                || state.tracker.getId() == null) {
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }

        salarySetupCheckStarted = true;
        UserProfileRepository userProfileRepository = new UserProfileRepository();
        userProfileRepository.loadCurrentNickname(new UserProfileRepository.Callback<String>() {
            @Override
            public void onSuccess(String nickname) {
                currentNickname = nickname != null ? nickname.trim() : "";
                if (currentNickname.isEmpty()) {
                    salarySetupCheckStarted = false;
                    return;
                }

                DatabaseReference participantsRef = FirebaseDatabase.getInstance()
                        .getReference()
                        .child("trackers_v2")
                        .child(state.tracker.getId())
                        .child("participants");

                participantsRef.get()
                        .addOnSuccessListener(snapshot -> {
                            DataSnapshot currentParticipantSnapshot = findCurrentUserParticipant(snapshot, user);
                            if (currentParticipantSnapshot != null) {
                                Boolean incomePending = currentParticipantSnapshot
                                        .child("incomePending")
                                        .getValue(Boolean.class);
                                if (Boolean.TRUE.equals(incomePending)) {
                                    runOnUiThread(() -> showSalaryDialog(
                                            state.tracker.getId(),
                                            snapshot,
                                            user,
                                            "",
                                            () -> saveCurrentUserSalary(state.tracker.getId(), snapshot, user, 1)
                                    ));
                                }
                                return;
                            }

                            if (attachCurrentUserToMatchingParticipant(state.tracker.getId(), snapshot, user)) {
                                return;
                            }

                            runOnUiThread(() -> showSalaryDialog(
                                    state.tracker.getId(),
                                    snapshot,
                                    user,
                                    "",
                                    () -> saveCurrentUserSalary(state.tracker.getId(), snapshot, user, 1)
                            ));
                        })
                        .addOnFailureListener(error -> {
                            Log.e("ExpenseV2", "Error checking participant setup", error);
                            salarySetupCheckStarted = false;
                        });
            }

            @Override
            public void onError(Exception exception) {
                Log.e("ExpenseV2", "Error loading profile", exception);
                salarySetupCheckStarted = false;
            }
        });
    }

    private DataSnapshot findCurrentUserParticipant(DataSnapshot snapshot, FirebaseUser user) {
        for (DataSnapshot child : snapshot.getChildren()) {
            String userId = child.child("userId").getValue(String.class);
            if (user.getUid().equals(userId)) {
                return child;
            }
        }
        return null;
    }

    private boolean attachCurrentUserToMatchingParticipant(String trackerId, DataSnapshot snapshot, FirebaseUser user) {
        for (DataSnapshot child : snapshot.getChildren()) {
            String participantId = child.getKey();
            String name = child.child("name").getValue(String.class);
            String userId = child.child("userId").getValue(String.class);
            if (participantId != null
                    && userId == null
                    && name != null
                    && name.trim().equalsIgnoreCase(currentNickname)) {
                FirebaseDatabase.getInstance()
                        .getReference()
                        .child("trackers_v2")
                        .child(trackerId)
                        .child("participants")
                        .child(participantId)
                        .child("userId")
                        .setValue(user.getUid());
                return true;
            }
        }
        return false;
    }

    private void showEditSalaryDialog() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || currentState == null || currentState.tracker == null || currentState.tracker.getId() == null) {
            AppSnackbar.show(this, "No se pudo editar el sueldo");
            return;
        }

        String trackerId = currentState.tracker.getId();
        FirebaseDatabase.getInstance()
                .getReference()
                .child("trackers_v2")
                .child(trackerId)
                .child("participants")
                .get()
                .addOnSuccessListener(snapshot -> {
                    DataSnapshot participantSnapshot = findCurrentUserParticipant(snapshot, user);
                    if (participantSnapshot == null) {
                        participantSnapshot = findCurrentUserParticipantByNickname(snapshot);
                    }

                    String initialSalary = "";
                    if (participantSnapshot != null) {
                        initialSalary = formatSalaryInput(readDouble(participantSnapshot.child("income").getValue()));
                    }

                    String initialSalaryValue = initialSalary;
                    runOnUiThread(() -> showSalaryDialog(trackerId, snapshot, user, initialSalaryValue, null));
                })
                .addOnFailureListener(error -> {
                    Log.e("ExpenseV2", "Error loading salary for edit", error);
                    AppSnackbar.show(this, "No se pudo editar el sueldo");
                });
    }

    private DataSnapshot findCurrentUserParticipantByNickname(DataSnapshot snapshot) {
        for (DataSnapshot child : snapshot.getChildren()) {
            String name = child.child("name").getValue(String.class);
            if (name != null && name.trim().equalsIgnoreCase(currentNickname)) {
                return child;
            }
        }
        return null;
    }

    private void showSalaryDialog(
            String trackerId,
            DataSnapshot participantsSnapshot,
            FirebaseUser user,
            String initialSalary,
            Runnable onCancel
    ) {
        AppDialog.showNumberInput(
                this,
                "Tu sueldo",
                initialSalary,
                "Sueldo",
                "Guardar",
                "Ingresá tu sueldo.",
                value -> {
                    try {
                        Double.parseDouble(value);
                        return null;
                    } catch (NumberFormatException e) {
                        return "El sueldo debe ser numérico.";
                    }
                },
                value -> saveCurrentUserSalary(trackerId, participantsSnapshot, user, Double.parseDouble(value)),
                onCancel
        );
    }

    private double readDouble(Object value) {
        if (value instanceof Long) {
            return ((Long) value).doubleValue();
        }
        if (value instanceof Integer) {
            return ((Integer) value).doubleValue();
        }
        if (value instanceof Double) {
            return (Double) value;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception e) {
            return 0;
        }
    }

    private String formatSalaryInput(double salary) {
        if (salary <= 0) {
            return "";
        }
        if (salary == (long) salary) {
            return String.valueOf((long) salary);
        }
        return String.valueOf(salary);
    }

    private void saveCurrentUserSalary(
            String trackerId,
            DataSnapshot participantsSnapshot,
            FirebaseUser user,
            double salary
    ) {
        if (salarySetupSaving) {
            return;
        }

        salarySetupSaving = true;
        String participantId = resolveCurrentParticipantId(participantsSnapshot, user);
        String otherParticipantId = "p1".equals(participantId) ? "p2" : "p1";

        Map<String, Object> currentParticipant = new HashMap<>();
        currentParticipant.put("active", true);
        currentParticipant.put("income", salary);
        currentParticipant.put("order", "p2".equals(participantId) ? 2 : 1);
        currentParticipant.put("userId", user.getUid());
        currentParticipant.put("incomePending", false);

        Map<String, Object> updates = new HashMap<>();
        updates.put("trackers_v2/" + trackerId + "/participants/" + participantId, currentParticipant);
        updates.put("trackers_v2/" + trackerId + "/summary/participantCount", 2);
        updates.put("home_index/" + trackerId + "/isSetupComplete", true);

        if (!participantsSnapshot.child(otherParticipantId).exists()) {
            Map<String, Object> otherParticipant = new HashMap<>();
            otherParticipant.put("active", true);
            otherParticipant.put("income", 1);
            otherParticipant.put("order", "p2".equals(otherParticipantId) ? 2 : 1);
            updates.put("trackers_v2/" + trackerId + "/participants/" + otherParticipantId, otherParticipant);
        }

        FirebaseDatabase.getInstance()
                .getReference()
                .updateChildren(updates)
                .addOnSuccessListener(unused -> {
                    HCardDB.setSetupComplete(trackerId, true);
                    salarySetupSaving = false;
                    if (controller != null) {
                        controller.refresh();
                    }
                })
                .addOnFailureListener(error -> {
                    Log.e("ExpenseV2", "Error saving salary setup", error);
                    salarySetupSaving = false;
                    salarySetupCheckStarted = false;
                    AppSnackbar.show(this, "No se pudo guardar tu sueldo");
                });
    }

    private String resolveCurrentParticipantId(DataSnapshot snapshot, FirebaseUser user) {
        for (DataSnapshot child : snapshot.getChildren()) {
            String userId = child.child("userId").getValue(String.class);
            if (user.getUid().equals(userId)) {
                return child.getKey();
            }
        }

        for (DataSnapshot child : snapshot.getChildren()) {
            String participantId = child.getKey();
            String name = child.child("name").getValue(String.class);
            if (participantId != null && name != null && name.trim().equalsIgnoreCase(currentNickname)) {
                return participantId;
            }
        }

        if (!snapshot.child("p1").exists() || isAvailableParticipant(snapshot.child("p1"))) {
            return "p1";
        }

        if (!snapshot.child("p2").exists() || isAvailableParticipant(snapshot.child("p2"))) {
            return "p2";
        }

        return "p1";
    }

    private boolean isAvailableParticipant(DataSnapshot participantSnapshot) {
        String userId = participantSnapshot.child("userId").getValue(String.class);
        String name = participantSnapshot.child("name").getValue(String.class);
        return userId == null
                && (name == null
                || name.trim().isEmpty()
                || UserProfileRepository.TBD_NAME.equalsIgnoreCase(name.trim()));
    }

    private void showMoreOptionsMenu() {
        if (btnMoreOptions == null) {
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        View popupView = inflater.inflate(R.layout.view_sort_dropdown, null);
        LinearLayout container = popupView.findViewById(R.id.sortDropdownContainer);

        PopupWindow popupWindow = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );

        String closeOptionLabel;

        if (currentState != null && currentState.tracker != null && currentState.tracker.isClosed()) {
            closeOptionLabel = "Abrir tracker";
        } else {
            closeOptionLabel = "Cerrar tracker";
        }

        if (canEditTrackerName()) {
            addMoreOptionItem(container, "Editar nombre", popupWindow, this::showEditTrackerNameAppDialog);
        }
        addMoreOptionItem(container, "Editar categor\u00edas", popupWindow, this::showEditCategoriesDialog);
        addMoreOptionItem(container, "Configuración", popupWindow, this::openSettings);
        addMoreOptionItem(container, closeOptionLabel, popupWindow, this::confirmCloseTracker);
        if (currentState == null
                || currentState.tracker == null
                || !currentState.tracker.isMonthly()) {
            addMoreOptionItem(container, "Eliminar tracker", popupWindow, this::confirmDeleteTracker);
        }

        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setOutsideTouchable(true);
        popupWindow.setElevation(dpToPx(8));

        popupView.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );

        int popupWidth = popupView.getMeasuredWidth();
        int xOff = btnMoreOptions.getWidth() - popupWidth;

        popupWindow.showAsDropDown(btnMoreOptions, xOff, dpToPx(8));
    }

    private void addMoreOptionItem(
            LinearLayout container,
            String label,
            PopupWindow popupWindow,
            Runnable action
    ) {
        android.widget.TextView itemView = new android.widget.TextView(this);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );

        if (container.getChildCount() > 0) {
            params.topMargin = dpToPx(8);
        }

        itemView.setLayoutParams(params);
        itemView.setText(label);
        itemView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 16);
        itemView.setTypeface(itemView.getTypeface(), android.graphics.Typeface.BOLD);
        itemView.setTextColor(getAttrColor(R.attr.sortDropdownText));
        itemView.setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14));
        itemView.setBackgroundResource(R.drawable.bg_content_category_expense_click);
        itemView.setClickable(true);
        itemView.setFocusable(true);

        itemView.setOnClickListener(v -> {
            popupWindow.dismiss();
            action.run();
        });

        container.addView(itemView);
    }

    private void showEditTrackerNameAppDialog() {
        if (!canEditTrackerName()) {
            return;
        }

        String currentName = currentState.tracker.getName();

        AppDialog.showTextInput(
                this,
                "Editar nombre",
                currentName,
                "Nombre tracker",
                "Guardar",
                "El nombre no puede estar vacÃ­o",
                null,
                newName -> {
                    controller.updateTrackerName(newName);
                    HCardDB.setName(currentState.tracker.getId(), newName);
                }
        );
    }

    private void showEditTrackerNameDialog() {
        if (!canEditTrackerName()) {
            return;
        }

        String currentName = currentState.tracker.getName();

        final android.widget.EditText input = new android.widget.EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(currentName);
        input.setSelection(input.getText().length());

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Editar nombre")
                .setView(input)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String newName = input.getText().toString().trim();

                    if (newName.isEmpty()) {
                        AppSnackbar.show(this, "El nombre no puede estar vacío");
                        return;
                    }

                    controller.updateTrackerName(newName);
                    HCardDB.setName(currentState.tracker.getId(), newName);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private boolean canEditTrackerName() {
        return currentState != null
                && currentState.tracker != null
                && !currentState.tracker.isMonthly();
    }

    private void showEditCategoriesDialog() {
        if (currentState == null || currentState.tracker == null) {
            AppSnackbar.show(this, "No se pudieron abrir las categorías");
            return;
        }

        CategoriesBottomSheetDialog
                .newInstance(currentState, controller)
                .show(getSupportFragmentManager(), "CATEGORIES_BOTTOM_SHEET");
    }

    private void openSettings() {
        if (currentState == null || currentState.tracker == null || currentState.tracker.getId() == null) {
            AppSnackbar.show(this, "No se pudo abrir configuración");
            return;
        }

        Intent intent = new Intent(this, SettingsActivity.class);
        intent.putExtra("trackerId", currentState.tracker.getId());
        intent.putExtra("fromExpenseV2", true);
        startActivity(intent);
    }

    private void confirmCloseTracker() {
        if (currentState == null || currentState.tracker == null || currentState.tracker.getId() == null) {
            AppSnackbar.show(this, "No se pudo actualizar el tracker");
            return;
        }

        boolean currentlyClosed = currentState.tracker.isClosed();
        String title = currentlyClosed ? "Abrir tracker" : "Cerrar tracker";
        String message = currentlyClosed
                ? "¿Estás seguro que querés abrir el tracker nuevamente?"
                : "¿Estás seguro que querés cerrar el tracker? Esto deshabilitará las funcionalidades del mismo.";

        AppDialog.showConfirmation(
                this,
                title,
                message,
                "Confirmar",
                AppDialog.ActionStyle.PRIMARY,
                () -> {
                    boolean newClosedValue = !currentlyClosed;

                    controller.updateTrackerClosed(newClosedValue);
                    HCardDB.setCerrado(newClosedValue);

                    AppSnackbar.show(this, newClosedValue ? "Tracker cerrado" : "Tracker abierto");

                    finish();
                }
        );
    }

    private void confirmDeleteTracker() {
        if (currentState == null || currentState.tracker == null || currentState.tracker.getId() == null) {
            AppSnackbar.show(this, "No se pudo eliminar el tracker");
            return;
        }

        if (currentState.tracker.isMonthly()) {
            AppSnackbar.show(this, "Los trackers mensuales no se pueden eliminar");
            return;
        }

        String trackerName = currentState.tracker.getName();

        AppDialog.showConfirmation(
                this,
                "Eliminar tracker",
                "Est\u00e1s seguro que quer\u00e9s eliminar el tracker " + trackerName + "?",
                "Eliminar",
                AppDialog.ActionStyle.DESTRUCTIVE,
                this::deleteCurrentTracker
        );
    }

    private void deleteCurrentTracker() {
        if (currentState == null || currentState.tracker == null || currentState.tracker.getId() == null) {
            AppSnackbar.show(this, "No se pudo eliminar el tracker");
            return;
        }

        if (currentState.tracker.isMonthly()) {
            AppSnackbar.show(this, "Los trackers mensuales no se pueden eliminar");
            return;
        }

        String trackerId = currentState.tracker.getId();
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        HashMap<String, Object> updates = new HashMap<>();
        updates.put("home_index/" + trackerId, null);
        updates.put("trackers_v2/" + trackerId, null);

        rootRef.updateChildren(updates)
                .addOnSuccessListener(unused -> {
                    HCardDB.removeReportFromArrayList(trackerId);
                    SettingsDB.removeReportFromArrayList(trackerId);

                    Intent intent = new Intent(this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    intent.putExtra(MainActivity.EXTRA_HOME_MESSAGE, "Tracker eliminado");
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(error -> {
                    Log.e("ExpenseV2", "Error deleting tracker", error);
                    AppSnackbar.show(this, "No se pudo eliminar el tracker");
                });
    }

    private int getAttrColor(int attr) {
        android.util.TypedValue typedValue = new android.util.TypedValue();
        getTheme().resolveAttribute(attr, typedValue, true);

        if (typedValue.resourceId != 0) {
            return androidx.core.content.ContextCompat.getColor(this, typedValue.resourceId);
        }

        return typedValue.data;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

}

