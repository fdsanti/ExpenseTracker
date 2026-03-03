package com.example.expensetracker;

import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.example.expensetracker.ExpenseRow;

public class ResumenFragment extends Fragment {

    private PieChart pieChart;
    private RecyclerView recyclerView;
    private ResumenAdapter adapter;
    private List<AnalysisCategory> analysisData = new ArrayList<>();

    public ResumenFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_resumen, container, false);

        pieChart = view.findViewById(R.id.pieChart);
        recyclerView = view.findViewById(R.id.rv_analysis);

        setupPieChart();
        setupRecyclerView();

        // For now, let's load some dummy data to see how it looks.
        // We will replace this with real Firebase data in the next step.

        return view;
    }

    private void setupPieChart() {
        // Basic config
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);

        // This creates the "Donut" hole
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.setHoleRadius(75f); // Larger hole = Thinner ring
        pieChart.setTransparentCircleRadius(0f);

        pieChart.setDrawCenterText(true);
        pieChart.setCenterTextTypeface(android.graphics.Typeface.DEFAULT_BOLD);

        // Disable the chart's built-in legend because you want a custom list below
        pieChart.getLegend().setEnabled(false);
        pieChart.setDrawEntryLabels(false);
        pieChart.setRotationAngle(0);
        pieChart.setRotationEnabled(true);

        // Rotation and interactivity
        pieChart.setRotationAngle(0);
        pieChart.setRotationEnabled(true);
        pieChart.setHighlightPerTapEnabled(true);

        // Disable text inside the chart slices
        pieChart.setDrawEntryLabels(false);

        pieChart.setNoDataText("No hay datos disponibles");
        pieChart.setNoDataTextColor(Color.GRAY);
        pieChart.setVisibility(View.VISIBLE); // Force visibility

    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ResumenAdapter(analysisData);
        recyclerView.setAdapter(adapter);
    }

    private void loadDummyData() {
        analysisData.clear();

        // Creating some dummy categories to test the UI
        AnalysisCategory cat1 = new AnalysisCategory("Comida", 450.0);
        cat1.setPercentage(45f);
        cat1.setColor(Color.parseColor("#FF7043")); // Orange-ish

        AnalysisCategory cat2 = new AnalysisCategory("Transporte", 200.0);
        cat2.setPercentage(20f);
        cat2.setColor(Color.parseColor("#26A69A")); // Teal-ish

        AnalysisCategory cat3 = new AnalysisCategory("Ocio", 350.0);
        cat3.setPercentage(35f);
        cat3.setColor(Color.parseColor("#5C6BC0")); // Indigo-ish

        analysisData.add(cat1);
        analysisData.add(cat2);
        analysisData.add(cat3);

        updateChart();
        adapter.notifyDataSetChanged();
    }

    private void updateChart() {
        ArrayList<PieEntry> entries = new ArrayList<>();
        ArrayList<Integer> colors = new ArrayList<>();

        for (AnalysisCategory category : analysisData) {
            entries.add(new PieEntry((float) category.getTotal(), category.getName()));
            colors.add(category.getColor());
        }

        PieDataSet dataSet = new PieDataSet(entries, "");

        // SPACE BETWEEN SLICES: This makes it look like separate shapes
        dataSet.setSliceSpace(8f);
        dataSet.setSelectionShift(5f);
        dataSet.setColors(colors);

        // Hide values on the chart to keep it clean
        dataSet.setDrawValues(false);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.invalidate(); // Refresh chart
    }

    private List<ExpenseRow> pendingRows;
    // At the top of your class add a Tag for logs
    private static final String TAG = "ResumenFragment";

    public void updateData(List<ExpenseRow> rows) {

        if (rows == null || rows.isEmpty()) {
            android.util.Log.d(TAG, "Rows are empty or null, returning.");
            return;
        }

        this.pendingRows = rows;

        // CHECK: Is the pieChart actually initialized?
        if (pieChart == null) {
            android.util.Log.d(TAG, "PieChart is null, storing data and returning.");
            return;
        }

        // 1. Group totals by category
        java.util.Map<String, Double> categoryTotals = new java.util.HashMap<>();
        double grandTotal = 0;

        for (ExpenseRow row : rows) {
            String cat = row.getCategory();
            if (cat == null || cat.isEmpty()) cat = "Otros";

            double val = row.getValue();
            double currentTotal = categoryTotals.containsKey(cat) ? categoryTotals.get(cat) : 0;
            categoryTotals.put(cat, currentTotal + val);
            grandTotal += val;
        }

        android.util.Log.d(TAG, "Calculated Grand Total: " + grandTotal);

        // 2. Convert Map to AnalysisCategory objects
        analysisData.clear();
        int[] colorPalette = {
                Color.parseColor("#FF7043"), Color.parseColor("#26A69A"),
                Color.parseColor("#5C6BC0"), Color.parseColor("#FFA726"),
                Color.parseColor("#EC407A"), Color.parseColor("#78909C")
        };

        int colorIndex = 0;
        for (java.util.Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
            AnalysisCategory ac = new AnalysisCategory(entry.getKey(), entry.getValue());
            ac.setPercentage((float) (entry.getValue() * 100 / grandTotal));
            ac.setColor(colorPalette[colorIndex % colorPalette.length]);
            colorIndex++;
            analysisData.add(ac);
        }

        // 3. Force UI Update
        android.util.Log.d(TAG, "Updating UI with " + analysisData.size() + " categories");

        String totalString = "TOTAL\n$" + String.format(Locale.getDefault(), "%.0f", grandTotal);
        pieChart.setCenterText(totalString);

        // We set the size to 24 (from your TotalChart style)
        pieChart.setCenterTextSize(24f);
        // Use the color from your style (White)

        //NEED TO FIX THIS LINE, IT IS MAKING THE TEXT ALWAYS WHITE, DISREGARDING THE THEME
        pieChart.setCenterTextColor(ContextCompat.getColor(getContext(), R.color.white));

        // Set Bold (from your style)
        pieChart.setCenterTextTypeface(android.graphics.Typeface.DEFAULT_BOLD);

        // ... (rest of the update logic) ...
        updateChart();

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Using post ensures the PieChart has dimensions before we draw
        view.post(new Runnable() {
            @Override
            public void run() {
                if (getActivity() instanceof ExpenseActivity) {
                    List<ExpenseRow> rows = ((ExpenseActivity) getActivity()).getAllRows();
                    if (rows != null) {
                        updateData(rows);
                    }
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Check if Activity already has data loaded
        if (getActivity() instanceof ExpenseActivity) {
            List<ExpenseRow> rows = ((ExpenseActivity) getActivity()).getAllRows();
            if (rows != null && !rows.isEmpty()) {
                android.util.Log.d("ResumenFragment", "Data pulled from Activity in onResume");
                updateData(rows);
            }
        }
    }




    // You can remove the setUserVisibleHint method entirely as it is less reliable
}