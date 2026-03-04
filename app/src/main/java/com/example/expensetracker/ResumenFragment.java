package com.example.expensetracker;

import android.graphics.Color;
import android.os.Build;
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
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);

        // Disable all interactions
        pieChart.setTouchEnabled(false);
        pieChart.setHighlightPerTapEnabled(false);

        pieChart.setDrawHoleEnabled(true);

        // 1. SET HOLE COLOR TO WHITE (or your background color)
        // Using Color.TRANSPARENT with SliceSpace often creates "sharp"
        // pixelated edges. A solid color creates a smoother anti-aliased edge.
        pieChart.setHoleColor(Color.TRANSPARENT);

        pieChart.setHoleRadius(75f);
        pieChart.setTransparentCircleRadius(0f);
        pieChart.setDrawCenterText(true);
        pieChart.getLegend().setEnabled(false);
        pieChart.setDrawEntryLabels(true);
        pieChart.setEntryLabelColor(Color.TRANSPARENT);
        pieChart.setRotationEnabled(false);
        pieChart.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        // 2. REMOVE OR COMMENT OUT THIS LINE
        // pieChart.setDrawRoundedSlices(true);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ResumenAdapter(analysisData);
        recyclerView.setAdapter(adapter);
    }

    private List<ExpenseRow> pendingRows;
    // At the top of your class add a Tag for logs
    private static final String TAG = "ResumenFragment";

    public void updateData(List<ExpenseRow> rows) {
        if (rows == null || rows.isEmpty()) return;

        this.pendingRows = rows;
        if (pieChart == null) return;

        // 1. Group totals by category
        java.util.Map<String, Double> categoryTotals = new java.util.HashMap<>();
        double grandTotal = 0;

        for (ExpenseRow row : rows) {
            String cat = row.getCategory();
            if (cat == null || cat.isEmpty()) cat = "Otros";
            double val = row.getValue();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                categoryTotals.put(cat, (categoryTotals.getOrDefault(cat, 0.0)) + val);
            }
            grandTotal += val;
        }

        // 2. Convert Map to AnalysisCategory objects
        analysisData.clear();

        for (java.util.Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
            AnalysisCategory ac = new AnalysisCategory(entry.getKey(), entry.getValue());
            float actualPercentage = (float) (entry.getValue() * 100 / grandTotal);

            // Apply 1% minimum floor for any category with a value > 0
            float displayPercentage = actualPercentage;
            if (entry.getValue() > 0 && actualPercentage < 1.3f) {
                displayPercentage = 1.3f;
            }

            ac.setPercentage(displayPercentage);
            ac.setColor(getColorForCategory(entry.getKey()));
            analysisData.add(ac);
        }

        // Sort biggest to smallest
        java.util.Collections.sort(analysisData, (o1, o2) -> Double.compare(o2.getTotal(), o1.getTotal()));

        // 3. Update the Slices
        ArrayList<PieEntry> entries = new ArrayList<>();
        ArrayList<Integer> colors = new ArrayList<>();

        for (AnalysisCategory category : analysisData) {
            // We use the "Percentage" as the value for the entry to respect the 1% floor
            // Note: PieChart automatically normalizes these values to 100%
            PieEntry entry = new PieEntry(category.getPercentage(), category.getName());

            if (category.getPercentage() >= 15f && getContext() != null) {
                int iconResId = getIconForCategory(category.getName());
                android.graphics.drawable.Drawable drawable = ContextCompat.getDrawable(getContext(), iconResId);

                if (drawable != null) {
                    drawable = drawable.mutate();
                    drawable.setColorFilter(new android.graphics.PorterDuffColorFilter(
                            Color.WHITE, android.graphics.PorterDuff.Mode.SRC_IN));
                    drawable.setBounds(0, 0, 80, 80);
                    entry.setIcon(drawable);
                }
            }
            entries.add(entry);
            colors.add(category.getColor());
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setSliceSpace(6f);
        dataSet.setSelectionShift(0f);
        dataSet.setDrawValues(false);
        dataSet.setDrawIcons(true);

        // This ensures icons stay centered even with the 1% floor shifts
        dataSet.setIconsOffset(new com.github.mikephil.charting.utils.MPPointF(0, 0));

        PieData data = new PieData(dataSet);
        pieChart.setData(data);

        // 4. Update the Center Text (Total with decimals/commas)
        String amountText = "$" + String.format(Locale.getDefault(), "%,.2f", grandTotal);
        android.text.SpannableString s = new android.text.SpannableString(amountText);

        int textColor = Color.BLACK;
        TypedValue tv = new TypedValue();
        if (getContext() != null && getContext().getTheme().resolveAttribute(R.attr.textColorPrimary, tv, true)) {
            textColor = tv.data;
        }

        s.setSpan(new android.text.style.ForegroundColorSpan(textColor), 0, s.length(), 0);
        s.setSpan(new android.text.style.RelativeSizeSpan(1.8f), 0, s.length(), 0);
        s.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, s.length(), 0);

        pieChart.setCenterText(s);
        pieChart.notifyDataSetChanged();
        pieChart.invalidate();

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
    private int getIconForCategory(String name) {
        if (name == null) return R.drawable.ic_info;

        // We convert to lowercase once here
        String normalizedName = name.toLowerCase().trim();

        switch (normalizedName) {
            case "delivery":
                return R.drawable.delivery;
            case "salidas":
                return R.drawable.salidas;
            case "super":
                return R.drawable.supermercado;
            case "gatitas":
                return R.drawable.gatitas;
            case "servicios":
                return R.drawable.servicios;
            case "nafta / peajes":
                return R.drawable.nafta_peajes;
            case "olga":
                return R.drawable.olga;
            case "auto":
                return R.drawable.auto;
            case "pago casa":
                return R.drawable.pago_casa;
            case "suscripciones":
                return R.drawable.suscripciones;
            case "compras":
                return R.drawable.compras;
            default:
                return R.drawable.ic_info; // Fallback icon
        }
    }

    private int getColorForCategory(String name) {
        String normalized = name.toLowerCase().trim();
        switch (normalized) {
            case "super":
                return Color.parseColor("#A99E00");
            case "salidas":
                return Color.parseColor("#007356");
            case "delivery":
                return Color.parseColor("#004573");
            case "gatitas":
                return Color.parseColor("#5C3FFF");
            case "servicios":
                return Color.parseColor("#9100A4");
            case "nafta / peajes":
                return Color.parseColor("#A4003C");
            case "olga":
                return Color.parseColor("#A40019");
            case "auto":
                return Color.parseColor("#A45D00");
            case "pago casa":
                return Color.parseColor("#978600");
            case "suscripciones":
                return Color.parseColor("#731D00");
            case "compras":
                return Color.parseColor("#006D73");
            case "otros":
                return Color.parseColor("#5C5C5C");
            default:
                return Color.parseColor("#5C5C5C");
        }
    }
}