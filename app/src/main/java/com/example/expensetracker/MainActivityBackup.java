package com.example.expensetracker;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.app.ProgressDialog;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.ArrayList;

public class MainActivityBackup extends AppCompatActivity implements CallBackItemTouch, SwipeRefreshLayout.OnRefreshListener{

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
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);


        toolbar = findViewById(R.id.toolbar_home);
        setSupportActionBar(toolbar);

        progressBar = findViewById(R.id.progressBar);

        //homeRecycler = findViewById(R.id.homeRecycler);

        viewPager = findViewById(R.id.viewPagerMain);
        tabLayout = findViewById(R.id.homeTabsComponent);
        tabLayout.setupWithViewPager(viewPager);

        actualFragment = new MainActualFragment();
        pastFragment = new MainPastFragment();

        actualFragment.setPastFragment(pastFragment);
        pastFragment.setActualFragment(actualFragment);

        ExpenseActivity.ViewPagerAdapter viewPagerAdapter = new ExpenseActivity.ViewPagerAdapter(getSupportFragmentManager(),0);
        viewPagerAdapter.addFragment(actualFragment, "Abiertos");
        viewPagerAdapter.addFragment(pastFragment, "Cerrados");
        viewPager.setAdapter(viewPagerAdapter);
        viewPager.setSwipeable(false);
}

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu,menu);
        return true;
    }

    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        //Craete Alert dialog
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
        dialog.setCancelable(false);
        dialog.setTitle("Agregar reporte");
        dialog.setMessage("Por favor escriba un nombre para el reporte");
        //Add input field to dialog
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.input1, null);
        dialog.setView(dialogView);
        dialog.setNegativeButton("Cancelar", null);
        dialog.setPositiveButton("Crear", null);

        AlertDialog alertDialog = dialog.create();
        alertDialog.show();

        //Click en "Crear" btn
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View v) {
                TextInputEditText editText = dialogView.findViewById(R.id.edit_text);
                //si el nombre ya existe, entonces mostrar error state
                if (HCardDB.containsDescription(editText.getText().toString())) {

                    TextInputLayout inputField = dialogView.findViewById(R.id.filledTextField);
                    inputField.setErrorEnabled(true);
                    inputField.setError("El nombre ya existe. Elija otro.");
                    inputField.setErrorIconDrawable(R.drawable.ic_info);
                }
                //si el input field esta vacio, mostrar error state
                else if (editText.getText().toString().trim().isEmpty()) {

                    TextInputLayout inputField = dialogView.findViewById(R.id.filledTextField);
                    inputField.setErrorEnabled(true);
                    inputField.setError("Es necesario elegir un nombre.");
                    inputField.setErrorIconDrawable(R.drawable.ic_info);
                }
                //Show progress bar and create new report
                else {
                    alertDialog.dismiss();

                    //Find biggest Id and create the new report with id + 1
                    int newID = HCardDB.getBiggestID() + 1;
                    HomeCard hc = new HomeCard(String.valueOf(newID), LocalDate.now(), editText.getText().toString(),false);

                    //add new card to Firebase DB & update the array
                    FirebaseDatabase database = FirebaseDatabase.getInstance();
                    DatabaseReference myRef = database.getReference();
                    myRef.child("allTables").get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DataSnapshot> task) {
                            if (!task.isSuccessful()) {
                                Log.e("firebase", "Error getting data", task.getException());
                            }
                            else {
                                myRef.child("allTables").child(hc.getTableID()).child("creationDate").setValue(String.valueOf(hc.getCreationDate()));
                                myRef.child("allTables").child(hc.getTableID()).child("tableDescription").setValue(hc.getName());
                                myRef.child("allTables").child(hc.getTableID()).child("tableName").setValue(hc.getTableID());
                                myRef.child("allTables").child(hc.getTableID()).child("cerrado").setValue(hc.isCerrado());
                                HCardDB.addExpense(String.valueOf(newID), hc);
                                actualFragment.addHCards(0,hc);
                                Toast.makeText(MainActivityBackup.this, "¡El expense ha sido creado con éxito!", Toast.LENGTH_SHORT).show();
                                Log.d("firebase", String.valueOf(task.getResult().getValue()));
                            }
                        }
                    });


                }
            }
        });
        return super.onOptionsItemSelected(item);
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