package com.example.expensetracker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;

public class MainActualFragment extends Fragment implements CallBackItemTouch, SwipeRefreshLayout.OnRefreshListener {

    private Context context;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private LinearProgressIndicator progressBar;
    private RecyclerView actualRecycler;
    private ArrayList<HomeCard> hCards;
    private HCardsViewAdapter adapter;
    private MainPastFragment mainPastFragment;
    public MainActualFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.main_actual_fragment,container,false);

        // SwipeRefreshLayout
        mSwipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_containerMain);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary,
                android.R.color.holo_green_dark,
                android.R.color.holo_orange_dark,
                android.R.color.holo_blue_dark);


        return rootView;
    }

    @SuppressLint({"ClickableViewAccessibility", "RestrictedApi", "ResourceType"})
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadVariables(view);
        if (HCardDB.isNull()) {
            progressBar.show();
            progressBar.setVisibility(View.VISIBLE);
            loadReportsFromFirebase();
        }
        if (!HCardDB.isNull()) {
            loadReportsFromArrayList();
        }
        ItemTouchHelper.Callback callback = new MainItemDragSwipeCallback(this);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(actualRecycler);
    }

    @Override
    public void itemTuchOnMove(int oldPosition, int newPosition) {

    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {

        int position = viewHolder.getAdapterPosition();

        MaterialAlertDialogBuilder dialog = getMaterialAlertDialogBuilder(position);
        //OnClickListener para el boton de Eliminar (dentro del popup)
        dialog.setPositiveButton("Eliminar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                FirebaseDatabase database = FirebaseDatabase.getInstance();
                DatabaseReference myRef = database.getReference();
                myRef.child("allTables").get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DataSnapshot> task) {
                        if (!task.isSuccessful()) {
                            Log.e("firebase", "Error getting data", task.getException());
                        }
                        else {
                            //remove table in "allTables", the table with all the rows & the data in "settings"
                            //then, remove it from the local array & notify the changes
                            myRef.child("allTables").child(hCards.get(position).getTableID()).removeValue();
                            myRef.child(hCards.get(position).getTableID()).removeValue();
                            myRef.child("settings").child(hCards.get(position).getTableID()).removeValue();
                            HCardDB.removeReportFromArrayList(hCards.get(position).getTableID());
                            SettingsDB.removeReportFromArrayList(hCards.get(position).getTableID());
                            hCards.remove(position);
                            adapter.notifyItemRemoved(position);
                            adapter.notifyItemRangeChanged(position,hCards.size());
                            Toast.makeText(context, "El reporte se ha eliminado", Toast.LENGTH_SHORT).show();
                            Log.d("firebase", String.valueOf(task.getResult().getValue()));
                        }
                    }
                });
            }
        });

        dialog.show();
    }

    @NonNull
    private MaterialAlertDialogBuilder getMaterialAlertDialogBuilder(int position) {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(context);
        dialog.setTitle("¿Eliminar reporte?");
        dialog.setMessage("¿Estás seguro que querés eliminar el reporte " + hCards.get(position).getName() + "?");
        dialog.setIcon(R.drawable.ic_delete);


        dialog.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                adapter.notifyDataSetChanged();
            }
        });
        return dialog;
    }

    public void addHCards(int position, HomeCard hc) {
        hCards.add(position,hc);
        adapter.notifyItemInserted(position);
        adapter.notifyItemRangeChanged(position,hCards.size());
        actualRecycler.smoothScrollToPosition(0);
    }
    @Override
    public void onRefresh() {

        new Handler().postDelayed(new Runnable() {
            @Override public void run() {
                mSwipeRefreshLayout.setRefreshing(false);
            }
        }, 50);

        progressBar.show();
        progressBar.setVisibility(View.VISIBLE);
        loadReportsFromFirebase();
    }

    private void loadReportsFromFirebase(){
        ArrayList<HomeCard> tempHCArray = new ArrayList<HomeCard>();
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference();
        //fetch homecards from Firebase db
        myRef.child("allTables").get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (!task.isSuccessful()) {
                    Log.e("firebase", "Error getting data", task.getException());
                }
                else {
                    for (DataSnapshot child : task.getResult().getChildren()) {
                        String id = child.child("tableName").getValue().toString().substring(4);
                        LocalDate date = Date.valueOf(child.child("creationDate").getValue(String.class)).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                        String reportName = child.child("tableDescription").getValue().toString();
                        Boolean cerrado = Boolean.valueOf((Boolean) child.child("cerrado").getValue());
                        HomeCard tempCard = new HomeCard(id, date, reportName, cerrado);
                        tempHCArray.add(tempCard);
                    }
                    //sort the array so that the new hashmap is ordered from newest to oldest
                    Collections.sort(tempHCArray, new HomeCardSortDate());
                    //add expense to the hashmap in HCardDB
                    HCardDB.clearMap();
                    for (HomeCard hc : tempHCArray) {
                        HCardDB.addExpense(hc.getId(), hc);
                    }
                    //load reports in the UI
                    loadReportsFromArrayList();
                    Log.d("firebase", String.valueOf(task.getResult().getValue()));
                }
            }
        });
        //fetch settings from Firebase db
        myRef.child("settings").get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (!task.isSuccessful()) {
                    Log.e("firebase", "Error getting data", task.getException());
                }
                else {
                    for (DataSnapshot child : task.getResult().getChildren()) {
                        String tableName = child.getKey().toString();
                        String name1 = child.child("name1").getValue().toString();
                        String name2 = child.child("name2").getValue().toString();
                        Integer sueldo1 = Integer.parseInt(child.child("sueldo1").getValue().toString());
                        Integer sueldo2 = Integer.parseInt(child.child("sueldo2").getValue().toString());
                        Settings tempSettings = new Settings(tableName, name1, sueldo1, name2, sueldo2);
                        SettingsDB.addToDB(tempSettings);
                    }
                    Log.d("firebase", String.valueOf(task.getResult().getValue()));
                }
            }
        });
    }

    private void loadReportsFromArrayList() {
        if(!HCardDB.isEmpty()) {
            System.out.println("HCardDB not empty");
            //hide progressBar
            progressBar.hide();
            progressBar.setVisibility(View.INVISIBLE);
            hCards = HCardDB.getReportsActuals();
            adapter = new HCardsViewAdapter(context);
            adapter.setCards(hCards);
            actualRecycler.setAdapter(adapter);
            actualRecycler.setLayoutManager(new LinearLayoutManager(context));
        }
        else {
            TextView txtEmpty = new TextView(context);
            txtEmpty.setText("There are no reports yet!");
            System.out.println("HCardDB empty");
        }

    }

    private void loadVariables(View view) {
        context = getContext();
        progressBar = view.findViewById(R.id.actualProgressBar);
        actualRecycler = view.findViewById(R.id.homeActualRecycler);
    }

    public void setPastFragment(MainPastFragment mainPastFragment) {
        this.mainPastFragment = mainPastFragment;
    }

    @Override
    public void onResume() {
        super.onResume();
        if(adapter != null) {
            hCards = HCardDB.getReportsActuals();
            adapter.setCards(hCards);
            adapter.notifyDataSetChanged();

        }
    }
}
