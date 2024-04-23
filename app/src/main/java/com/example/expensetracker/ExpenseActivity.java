package com.example.expensetracker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

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

import java.util.ArrayList;
import java.util.List;

public class ExpenseActivity extends AppCompatActivity {
    private MaterialToolbar toolbar;
    private LockableViewPager viewPager;
    private TabLayout tabLayout;
    private GastosFragment gastosFragment;
    private SaldosFragment saldosFragment;
    private RecyclerView recyclerView;
    private RowViewAdapter adapter;
    private RelativeLayout filtrosBar;
    private MenuItem icn_checked;

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

        gastosFragment = new GastosFragment();
        saldosFragment = new SaldosFragment(this);

        gastosFragment.setSaldosFragment(saldosFragment);
        saldosFragment.setGastosFragment(gastosFragment);

        tabLayout.setupWithViewPager(viewPager);

        ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager(),0);
        viewPagerAdapter.addFragment(gastosFragment, "Gastos");
        viewPagerAdapter.addFragment(saldosFragment, "Saldos");
        viewPager.setAdapter(viewPagerAdapter);
        viewPager.setSwipeable(false);

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

    private class ViewPagerAdapter extends FragmentPagerAdapter {
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


}

