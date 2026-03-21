package com.example.expensetracker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;


import java.util.ArrayList;
import java.util.List;

public class ExpenseActivity extends AppCompatActivity {
    private MaterialToolbar toolbar;
    private LockableViewPager viewPager;
    private TabLayout tabLayout;
    private GastosFragment gastosFragment;
    private SaldosFragment saldosFragment;
    private ResumenFragment resumenFragment;
    private RecyclerView recyclerView;
    private RowViewAdapter adapter;
    private RelativeLayout filtrosBar;
    private MenuItem icn_checked;
    private List<ExpenseRow> allRows;

    @SuppressLint("MissingInflatedId")
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        toolbar = findViewById(R.id.toolbar_report);
        setSupportActionBar(toolbar);
        //Al hacer click en back button
        toolbar.setTitle(HCardDB.getSelected().getName());
        toolbar.getChildAt(1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        filtrosBar = findViewById(R.id.filtrosBar);

        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);

        recyclerView = findViewById(R.id.expenseRecycler);

        loadSettingsAndSetupTabs();

    }

    private void setupTabs() {
        gastosFragment = new GastosFragment();
        saldosFragment = new SaldosFragment(this);
        resumenFragment = new ResumenFragment();

        gastosFragment.setSaldosFragment(saldosFragment);
        saldosFragment.setGastosFragment(gastosFragment);

        tabLayout.setupWithViewPager(viewPager);

        ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager(), 0);
        viewPagerAdapter.addFragment(gastosFragment, "Gastos");
        viewPagerAdapter.addFragment(saldosFragment, "Saldos");
        viewPagerAdapter.addFragment(resumenFragment, "Resumen");
        viewPager.setAdapter(viewPagerAdapter);
        viewPager.setOffscreenPageLimit(3);
        viewPager.setSwipeable(false);
    }

    private void loadSettingsAndSetupTabs() {
        String trackerId = HCardDB.getSelected().getTableID();

        FirebaseDatabase.getInstance()
                .getReference("trackers_v2")
                .child(trackerId)
                .child("participants")
                .get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Toast.makeText(this, "Error cargando settings", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    DataSnapshot participantsSnapshot = task.getResult();
                    String name1 = "";
                    String name2 = "";
                    int income1 = 0;
                    int income2 = 0;

                    DataSnapshot p1 = participantsSnapshot.child("p1");
                    DataSnapshot p2 = participantsSnapshot.child("p2");

                    if (p1.exists()) {
                        String p1Name = p1.child("name").getValue(String.class);
                        Object p1IncomeObj = p1.child("income").getValue();

                        name1 = p1Name != null ? p1Name : "";
                        if (p1IncomeObj instanceof Long) {
                            income1 = ((Long) p1IncomeObj).intValue();
                        } else if (p1IncomeObj instanceof Integer) {
                            income1 = (Integer) p1IncomeObj;
                        }
                    }

                    if (p2.exists()) {
                        String p2Name = p2.child("name").getValue(String.class);
                        Object p2IncomeObj = p2.child("income").getValue();

                        name2 = p2Name != null ? p2Name : "";
                        if (p2IncomeObj instanceof Long) {
                            income2 = ((Long) p2IncomeObj).intValue();
                        } else if (p2IncomeObj instanceof Integer) {
                            income2 = (Integer) p2IncomeObj;
                        }
                    }

                    SettingsDB.addToDB(new Settings(trackerId, name1, income1, name2, income2));
                    Log.d("DEBUG DATA", name1 + name2);
                    setupTabs();
                });
    }

    //need to make sure the home page is updated when an expense is closed

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings,menu);
        icn_checked = menu.findItem(R.id.iconChecked);
        updateCerrado();
        return true;
    }

    public void updateCerrado() {
        if (!HCardDB.getSelected().isCerrado()) {
            icn_checked.setVisible(false);
        }
        else {
            icn_checked.setVisible(true);
        }

    }


    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId()) {
            case R.id.btnSettings:
                Intent intent = new Intent(this, SettingsActivity.class);
                Bundle b = new Bundle();
                b.putInt("existingSetting", 1); //Your id
                intent.putExtras(b);
                this.startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        finish();
        //Intent intent = new Intent(ExpenseActivity.this, MainActivity.class);
        //ExpenseActivity.this.startActivity(intent);
    }

    static class ViewPagerAdapter extends FragmentPagerAdapter {
        private List<Fragment> fragments = new ArrayList<>();
        private List<String> fragmentTitle = new ArrayList<>();

        public ViewPagerAdapter(@NonNull FragmentManager fm, int behavior) {
            super(fm, behavior);
        }

        public void addFragment(Fragment fragment, String title) {
            fragments.add(fragment);
            fragmentTitle.add(title);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            return fragments.get(position);
        }

        @Override
        public int getCount() {
            return fragments.size();
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return fragmentTitle.get(position);
        }

    }

    // Inside ExpenseActivity
    public void onDataLoaded(List<ExpenseRow> rowsBoth) {
        this.allRows = rowsBoth; // Save a copy in the Activity too
        if (resumenFragment != null) {
            android.util.Log.d("DEBUG_DATA", "Sending rows to Resumen: " + rowsBoth.size());
            resumenFragment.updateData(rowsBoth);

        }
    }

    // Inside ExpenseActivity.java (at the bottom)
    public List<ExpenseRow> getAllRows() {
        return allRows;
    }

}

