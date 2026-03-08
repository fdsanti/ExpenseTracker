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

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
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
        LocalDate today = LocalDate.now();
        HomeCard hc = HomeCard.fromTrackerId(trackerId, today, trackerName, false, false);

        DatabaseReference trackerRef = FirebaseDatabase.getInstance()
                .getReference()
                .child(HomeFirebaseV2Repository.ROOT)
                .child(trackerId);

        trackerRef.child("meta").child("legacyId").setValue(trackerId);
        trackerRef.child("meta").child("name").setValue(trackerName);
        trackerRef.child("meta").child("createdAt").setValue(today.toString());
        trackerRef.child("meta").child("updatedAt").setValue(today.toString());
        trackerRef.child("meta").child("status").setValue("open");
        trackerRef.child("meta").child("closed").setValue(false);
        trackerRef.child("meta").child("version").setValue(2);
        trackerRef.child("meta").child("migratedFrom").setValue("created-directly-in-v2");

        trackerRef.child("participants").setValue(new HashMap<>());
        trackerRef.child("categories").setValue(new HashMap<>());
        trackerRef.child("expenses").setValue(new HashMap<>());
        trackerRef.child("summary").child("expenseCount").setValue(0);
        trackerRef.child("summary").child("totalAmount").setValue(0);
        trackerRef.child("summary").child("participantCount").setValue(0);
        trackerRef.child("summary").child("categoryCount").setValue(0)
                .addOnSuccessListener(unused -> {
                    HCardDB.addExpense(hc.getTableID(), hc);
                    actualFragment.addHCards(0, hc);
                    Toast.makeText(MainActivity.this, "¡El expense ha sido creado con éxito!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creando tracker en trackers_v2", e);
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
