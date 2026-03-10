package com.example.expensetracker;

import android.annotation.SuppressLint;
import android.content.Context;
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

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActualFragment extends Fragment implements CallBackItemTouch, SwipeRefreshLayout.OnRefreshListener {

    private Context context;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private LinearProgressIndicator progressBar;
    private RecyclerView actualRecycler;
    private ArrayList<HomeCard> hCards;
    private HCardsViewAdapter adapter;
    private MainPastFragment mainPastFragment;
    private final HomeFirebaseV2Repository repository = new HomeFirebaseV2Repository();

    public MainActualFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.main_actual_fragment, container, false);

        mSwipeRefreshLayout = rootView.findViewById(R.id.swipe_containerMain);
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
        } else {
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
        dialog.setPositiveButton("Eliminar", (dialogInterface, which) -> {
            String trackerId = hCards.get(position).getTableID();

            DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
            HashMap<String, Object> updates = new HashMap<>();
            updates.put("home_index/" + trackerId, null);
            updates.put("trackers_v2/" + trackerId, null);

            rootRef.updateChildren(updates)
                    .addOnSuccessListener(unused -> {
                        HCardDB.removeReportFromArrayList(trackerId);
                        SettingsDB.removeReportFromArrayList(trackerId);
                        hCards.remove(position);
                        adapter.notifyItemRemoved(position);
                        adapter.notifyItemRangeChanged(position, hCards.size());
                        Toast.makeText(context, "El reporte se ha eliminado", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        adapter.notifyDataSetChanged();
                        Toast.makeText(context, "No se pudo eliminar el reporte", Toast.LENGTH_SHORT).show();
                        Log.e("firebase", "Error deleting tracker", e);
                    });
        });

        dialog.show();
    }

    @NonNull
    private MaterialAlertDialogBuilder getMaterialAlertDialogBuilder(int position) {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(context);
        dialog.setTitle("¿Eliminar reporte?");
        dialog.setMessage("¿Estás seguro que querés eliminar el reporte " + hCards.get(position).getName() + "?");
        dialog.setIcon(R.drawable.ic_delete);
        dialog.setNegativeButton("Cancelar", (dialog1, which) -> adapter.notifyDataSetChanged());
        return dialog;
    }

    public void addHCards(int position, HomeCard hc) {
        if (hCards == null) {
            hCards = new ArrayList<>();
        }
        if (adapter == null) {
            adapter = new HCardsViewAdapter(context);
            actualRecycler.setLayoutManager(new LinearLayoutManager(context));
            actualRecycler.setAdapter(adapter);
        }
        hCards.add(position, hc);
        adapter.setCards(hCards);
        adapter.notifyItemInserted(position);
        adapter.notifyItemRangeChanged(position, hCards.size());
        actualRecycler.smoothScrollToPosition(0);
        progressBar.hide();
        progressBar.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onRefresh() {
        new Handler().postDelayed(() -> mSwipeRefreshLayout.setRefreshing(false), 50);
        progressBar.show();
        progressBar.setVisibility(View.VISIBLE);
        loadReportsFromFirebase();
    }

    private void loadReportsFromFirebase() {
        repository.loadHomeData(new HomeFirebaseV2Repository.LoadCallback() {
            @Override
            public void onSuccess() {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> loadReportsFromArrayList());
            }

            @Override
            public void onError(@NonNull Exception e) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    progressBar.hide();
                    progressBar.setVisibility(View.INVISIBLE);
                    Toast.makeText(context, "Error cargando trackers_v2", Toast.LENGTH_SHORT).show();
                    Log.e("firebase", "Error getting trackers_v2", e);
                });
            }
        });
    }

    private void loadReportsFromArrayList() {
        progressBar.hide();
        progressBar.setVisibility(View.INVISIBLE);

        hCards = HCardDB.getReportsActuals();
        if (adapter == null) {
            adapter = new HCardsViewAdapter(context);
            actualRecycler.setLayoutManager(new LinearLayoutManager(context));
            actualRecycler.setAdapter(adapter);
        }
        adapter.setCards(hCards);

        if (hCards.isEmpty()) {
            TextView txtEmpty = new TextView(context);
            txtEmpty.setText("There are no reports yet!");
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
        if (adapter != null) {
            hCards = HCardDB.getReportsActuals();
            adapter.setCards(hCards);
            adapter.notifyDataSetChanged();
        }
    }
}
