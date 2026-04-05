package com.example.expensetracker;

import com.google.firebase.auth.FirebaseAuth;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
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

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
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
    private FirebaseAuth mAuth;

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
                    Toast.makeText(MainActivity.this,
                            "Migración OK. Trackers migrados: " + migratedTrackers,
                            Toast.LENGTH_LONG).show();
                }

                @Override
                public void onError(@NonNull Exception e) {
                    Toast.makeText(MainActivity.this,
                            "Error migrando: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    Log.e("Migration", "Migration failed", e);
                }
            });*/
            initializePage();
        });
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
            MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
            dialog.setCancelable(false);
            dialog.setTitle("Agregar reporte");
            dialog.setMessage("Por favor escriba un nombre para el reporte");

            LayoutInflater inflater = this.getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.input1, null);
            dialog.setView(dialogView);
            dialog.setNegativeButton("Cancelar", null);
            dialog.setPositiveButton("Crear", null);

            AlertDialog alertDialog = dialog.create();
            alertDialog.show();

            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                TextInputEditText editText = dialogView.findViewById(R.id.edit_text);
                TextInputLayout inputField = dialogView.findViewById(R.id.filledTextField);

                String input = editText.getText() != null ? editText.getText().toString().trim() : "";
                if (HCardDB.containsDescription(input)) {
                    inputField.setErrorEnabled(true);
                    inputField.setError("El nombre ya existe. Elija otro.");
                    inputField.setErrorIconDrawable(R.drawable.ic_info);
                } else if (input.isEmpty()) {
                    inputField.setErrorEnabled(true);
                    inputField.setError("Es necesario elegir un nombre.");
                    inputField.setErrorIconDrawable(R.drawable.ic_info);
                } else {
                    alertDialog.dismiss();
                    createTrackerV2(input);
                }
            });

            return true;

        } else if (id == R.id.btnMore) {
            View anchorView = findViewById(R.id.btnMore);
            if (anchorView != null) {
                PopupMenu popup = new PopupMenu(this, anchorView);
                popup.getMenu().add("Cerrar sesión");

                popup.setOnMenuItemClickListener(menuItem -> {
                    if ("Cerrar sesión".contentEquals(menuItem.getTitle())) {
                        FirebaseAuth.getInstance().signOut();
                        startActivity(new Intent(this, LoginActivity.class));
                        finish();
                        return true;
                    }
                    return false;
                });

                popup.show();
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void createTrackerV2(@NonNull String trackerName) {
        int newID = HCardDB.getBiggestID() + 1;
        String trackerId = "DATA" + newID;
        LocalDate today = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            today = LocalDate.now();
        }
        HomeCard hc = HomeCard.fromTrackerId(trackerId, today, trackerName, false, false);

        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();

        HashMap<String, Object> updates = new HashMap<>();

        // trackers_v2
        updates.put("trackers_v2/" + trackerId + "/meta/legacyId", trackerId);
        updates.put("trackers_v2/" + trackerId + "/meta/name", trackerName);
        updates.put("trackers_v2/" + trackerId + "/meta/createdAt", today.toString());
        updates.put("trackers_v2/" + trackerId + "/meta/updatedAt", today.toString());
        updates.put("trackers_v2/" + trackerId + "/meta/closed", false);
        updates.put("trackers_v2/" + trackerId + "/meta/version", 2);
        updates.put("trackers_v2/" + trackerId + "/meta/migratedFrom", "created-directly-in-v2");

        updates.put("trackers_v2/" + trackerId + "/participants", new HashMap<>());
        updates.put("trackers_v2/" + trackerId + "/categories", new HashMap<>());
        updates.put("trackers_v2/" + trackerId + "/expenses", new HashMap<>());
        updates.put("trackers_v2/" + trackerId + "/summary/expenseCount", 0);
        updates.put("trackers_v2/" + trackerId + "/summary/totalAmount", 0);
        updates.put("trackers_v2/" + trackerId + "/summary/participantCount", 0);
        updates.put("trackers_v2/" + trackerId + "/summary/categoryCount", 0);

        // home_index
        updates.put("home_index/" + trackerId + "/trackerId", trackerId);
        updates.put("home_index/" + trackerId + "/name", trackerName);
        updates.put("home_index/" + trackerId + "/createdAt", today.toString());
        updates.put("home_index/" + trackerId + "/closed", false);
        updates.put("home_index/" + trackerId + "/isSetupComplete", false);

        rootRef.updateChildren(updates)
                .addOnSuccessListener(unused -> {
                    HCardDB.addExpense(hc.getTableID(), hc);
                    actualFragment.addHCards(0, hc);
                    Toast.makeText(MainActivity.this, "¡El expense ha sido creado con éxito!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creando tracker", e);
                    Toast.makeText(MainActivity.this, "No se pudo crear el expense", Toast.LENGTH_SHORT).show();
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
