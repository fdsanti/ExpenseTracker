package com.example.expensetracker;

import com.google.firebase.auth.FirebaseAuth;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.example.expensetracker.data.DefaultCategories;
import com.example.expensetracker.data.UserProfileRepository;
import com.example.expensetracker.ui.common.AppDialog;
import com.example.expensetracker.ui.common.AppSnackbar;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;

import android.graphics.drawable.ColorDrawable;
import androidx.core.content.ContextCompat;

import android.graphics.Color;
import android.util.TypedValue;

import androidx.core.graphics.drawable.DrawableCompat;

public class MainActivity extends AppCompatActivity implements CallBackItemTouch, SwipeRefreshLayout.OnRefreshListener {

    public static final String EXTRA_HOME_MESSAGE = "com.example.expensetracker.EXTRA_HOME_MESSAGE";

    private Toolbar toolbar;
    private TabLayout tabLayout;
    private LockableViewPager viewPager;
    private MainActualFragment actualFragment;
    private MainPastFragment pastFragment;
    private LinearProgressIndicator progressBar;
    private RecyclerView homeRecycler;
    private Connection connection;
    private DBHandler handler;
    private ProgressDialog progressDialog;
    private ExtendedFloatingActionButton btnNewHomeCard;
    private HCardsViewAdapter adapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private ArrayList<HomeCard> hCards;
    private Drawable mIcon;
    private MaterialButton btnSync;
    private static final String TAG = "MainActivity";
    private static final int SUMMARY_VERSION = 1;
    private FirebaseAuth mAuth;
    private UserProfileRepository userProfileRepository;
    private String currentNickname = "";

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AuthGuard.checkAccess(this, () -> {
            setContentView(R.layout.activity_main2);
            //Code for migrating to firebase trackers_v2
            /*FirebaseMigrationHelper.migrateLegacyToTrackersV2(new FirebaseMigrationHelper.MigrationCallback() {
                @Override
                public void onSuccess(int migratedTrackers) {
                    AppSnackbar.show(MainActivity.this, "Migración OK. Trackers migrados: " + migratedTrackers);
                }

                @Override
                public void onError(@NonNull Exception e) {
                    AppSnackbar.show(MainActivity.this, "Error migrando: " + e.getMessage());
                    Log.e("Migration", "Migration failed", e);
                }
            });*/
            userProfileRepository = new UserProfileRepository();
            ensureNicknameThenStart();
        });
    }

    private void ensureNicknameThenStart() {
        userProfileRepository.loadCurrentNickname(new UserProfileRepository.Callback<String>() {
            @Override
            public void onSuccess(String nickname) {
                runOnUiThread(() -> {
                    currentNickname = nickname != null ? nickname.trim() : "";
                    if (currentNickname.isEmpty()) {
                        showNicknameDialog(true);
                        return;
                    }

                    initializePage();
                    showHomeMessageIfNeeded(getIntent());
                });
            }

            @Override
            public void onError(Exception exception) {
                Log.e(TAG, "Error loading profile", exception);
                runOnUiThread(() -> {
                    AppSnackbar.show(MainActivity.this, "No se pudo cargar tu perfil");
                    initializePage();
                    showHomeMessageIfNeeded(getIntent());
                });
            }
        });
    }

    private void showNicknameDialog(boolean requiredBeforeStart) {
        AppDialog.TextInputCallback callback = value -> userProfileRepository.saveCurrentNickname(value, new UserProfileRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                runOnUiThread(() -> {
                    currentNickname = value;
                    AppSnackbar.show(MainActivity.this, "Apodo guardado");
                    if (requiredBeforeStart) {
                        initializePage();
                        showHomeMessageIfNeeded(getIntent());
                    }
                });
            }

            @Override
            public void onError(Exception exception) {
                Log.e(TAG, "Error saving nickname", exception);
                runOnUiThread(() -> {
                    AppSnackbar.show(MainActivity.this, "No se pudo guardar el apodo");
                    if (requiredBeforeStart) {
                        showNicknameDialog(true);
                    }
                });
            }
        });

        if (requiredBeforeStart) {
            AppDialog.showRequiredTextInput(
                    this,
                    "Elegí tu apodo",
                    currentNickname,
                    "Tu apodo",
                    "Guardar",
                    "Ingresá un apodo.",
                    null,
                    callback
            );
            return;
        }

        AppDialog.showTextInput(
                this,
                "Elegí tu apodo",
                currentNickname,
                "Tu apodo",
                "Guardar",
                "Ingresá un apodo.",
                null,
                callback
        );
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        showHomeMessageIfNeeded(intent);
    }

    private void showHomeMessageIfNeeded(Intent intent) {
        if (intent == null || !intent.hasExtra(EXTRA_HOME_MESSAGE)) {
            return;
        }

        String message = intent.getStringExtra(EXTRA_HOME_MESSAGE);
        intent.removeExtra(EXTRA_HOME_MESSAGE);

        if (message != null && !message.trim().isEmpty()) {
            AppSnackbar.show(this, message);
        }
    }

    public void redirectToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);

        int iconColor = resolveThemeColor(R.attr.primary_text);

        MenuItem addItem = menu.findItem(R.id.btnRefresh);
        if (addItem != null && addItem.getIcon() != null) {
            Drawable addIcon = DrawableCompat.wrap(addItem.getIcon().mutate());
            DrawableCompat.setTint(addIcon, iconColor);
            addItem.setIcon(addIcon);
        }

        MenuItem moreItem = menu.findItem(R.id.btnMore);
        if (moreItem != null && moreItem.getIcon() != null) {
            Drawable moreIcon = DrawableCompat.wrap(moreItem.getIcon().mutate());
            DrawableCompat.setTint(moreIcon, iconColor);
            moreItem.setIcon(moreIcon);
        }

        return true;
    }

    public void initializePage() {
        toolbar = findViewById(R.id.toolbar_home);
        setSupportActionBar(toolbar);

        progressBar = findViewById(R.id.progressBar);

        viewPager = findViewById(R.id.viewPagerMain);
        tabLayout = findViewById(R.id.homeTabsComponent);
        tabLayout.setupWithViewPager(viewPager);

        actualFragment = new MainActualFragment();
        pastFragment = new MainPastFragment();

        actualFragment.setPastFragment(pastFragment);
        pastFragment.setActualFragment(actualFragment);

        ExpenseActivity.ViewPagerAdapter viewPagerAdapter = new ExpenseActivity.ViewPagerAdapter(getSupportFragmentManager(), 0);
        viewPagerAdapter.addFragment(actualFragment, "Abiertos");
        viewPagerAdapter.addFragment(pastFragment, "Cerrados");
        viewPager.setAdapter(viewPagerAdapter);
        viewPager.setSwipeable(false);
    }

    private int resolveThemeColor(int attrResId) {
        TypedValue typedValue = new TypedValue();
        boolean resolved = getTheme().resolveAttribute(attrResId, typedValue, true);
        if (!resolved) {
            return Color.WHITE;
        }

        if (typedValue.resourceId != 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return getColor(typedValue.resourceId);
            }
        }

        return typedValue.data;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.btnRefresh) {
            AppDialog.showTextInput(
                    this,
                    "Agregar reporte",
                    "",
                    "Nombre tracker",
                    "Crear",
                    "Es necesario elegir un nombre.",
                    value -> HCardDB.containsDescription(value)
                            ? "El nombre ya existe. Elija otro."
                            : null,
                    this::createTrackerV2
            );

            return true;

        } else if (id == R.id.btnMore) {
            showMoreOptionsMenu();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showMoreOptionsMenu() {
        View anchorView = findViewById(R.id.btnMore);
        if (anchorView == null) {
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

        addMoreOptionItem(container, "Editar apodo", popupWindow, () -> showNicknameDialog(false));
        addMoreOptionItem(container, "Cerrar sesión", popupWindow, () -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setOutsideTouchable(true);
        popupWindow.setElevation(dpToPx(8));

        popupView.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );

        int popupWidth = popupView.getMeasuredWidth();
        int xOff = anchorView.getWidth() - popupWidth;

        popupWindow.showAsDropDown(anchorView, xOff, dpToPx(8));
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
        itemView.setTextColor(resolveThemeColor(R.attr.sortDropdownText));
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

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void createTrackerV2(@NonNull String trackerName) {
        int newID = HCardDB.getBiggestID() + 1;
        String trackerId = "DATA" + newID;
        final LocalDate today = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? LocalDate.now() : null;
        HomeCard hc = HomeCard.fromTrackerId(
                trackerId,
                today,
                trackerName,
                false,
                false,
                HomeCard.TYPE_MANUAL,
                null,
                0d,
                SUMMARY_VERSION
        );

        userProfileRepository.loadDefaultParticipants(new UserProfileRepository.Callback<java.util.Map<String, Object>>() {
            @Override
            public void onSuccess(java.util.Map<String, Object> defaultParticipants) {
                createTrackerV2WithParticipants(trackerId, today, trackerName, hc, defaultParticipants);
            }

            @Override
            public void onError(Exception exception) {
                Log.e(TAG, "Error loading default participants", exception);
                runOnUiThread(() -> AppSnackbar.show(MainActivity.this, "No se pudo crear el tracker"));
            }
        });
    }

    private void createTrackerV2WithParticipants(
            @NonNull String trackerId,
            LocalDate today,
            @NonNull String trackerName,
            @NonNull HomeCard hc,
            @NonNull java.util.Map<String, Object> defaultParticipants
    ) {
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();

        HashMap<String, Object> updates = new HashMap<>();
        HashMap<String, Object> defaultCategories = new HashMap<>(DefaultCategories.asFirebaseMap());

        // trackers_v2
        updates.put("trackers_v2/" + trackerId + "/meta/legacyId", trackerId);
        updates.put("trackers_v2/" + trackerId + "/meta/name", trackerName);
        updates.put("trackers_v2/" + trackerId + "/meta/createdAt", today.toString());
        updates.put("trackers_v2/" + trackerId + "/meta/updatedAt", today.toString());
        updates.put("trackers_v2/" + trackerId + "/meta/closed", false);
        updates.put("trackers_v2/" + trackerId + "/meta/type", HomeCard.TYPE_MANUAL);
        updates.put("trackers_v2/" + trackerId + "/meta/version", 2);
        updates.put("trackers_v2/" + trackerId + "/meta/migratedFrom", "created-directly-in-v2");

        updates.put("trackers_v2/" + trackerId + "/participants", defaultParticipants);
        updates.put("trackers_v2/" + trackerId + "/categories", defaultCategories);
        updates.put("trackers_v2/" + trackerId + "/expenses", new HashMap<>());
        updates.put("trackers_v2/" + trackerId + "/summary/expenseCount", 0);
        updates.put("trackers_v2/" + trackerId + "/summary/totalAmount", 0);
        updates.put("trackers_v2/" + trackerId + "/summary/participantCount", defaultParticipants.size());
        updates.put("trackers_v2/" + trackerId + "/summary/categoryCount", defaultCategories.size());
        updates.put("trackers_v2/" + trackerId + "/summary/version", SUMMARY_VERSION);

        // home_index
        updates.put("home_index/" + trackerId + "/trackerId", trackerId);
        updates.put("home_index/" + trackerId + "/name", trackerName);
        updates.put("home_index/" + trackerId + "/createdAt", today.toString());
        updates.put("home_index/" + trackerId + "/closed", false);
        updates.put("home_index/" + trackerId + "/isSetupComplete", false);
        updates.put("home_index/" + trackerId + "/type", HomeCard.TYPE_MANUAL);
        updates.put("home_index/" + trackerId + "/totalAmount", 0);
        updates.put("home_index/" + trackerId + "/summaryVersion", SUMMARY_VERSION);

        rootRef.updateChildren(updates)
                .addOnSuccessListener(unused -> {
                    HCardDB.addExpense(hc.getTableID(), hc);
                    actualFragment.addHCards(0, hc);
                    AppSnackbar.show(MainActivity.this, "Tracker creado");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creando tracker", e);
                    AppSnackbar.show(MainActivity.this, "No se pudo crear el tracker");
                });
    }

    @Override
    public void itemTuchOnMove(int oldPosition, int newPosition) {
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int position) {
    }

    @Override
    public void onRefresh() {
    }

}


