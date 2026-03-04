package com.example.expensetracker;

import android.graphics.Color;
import android.graphics.Paint;
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

        pieChart.setExtraOffsets(0, 5f, 0, 5f);

        pieChart.setDrawRoundedSlices(false);

        // Disable all interactions
        pieChart.setTouchEnabled(false);
        pieChart.setHighlightPerTapEnabled(false);

        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.setHoleRadius(75f);
        pieChart.setTransparentCircleRadius(0f);
        pieChart.setDrawCenterText(true);
        pieChart.getLegend().setEnabled(false);
        pieChart.setDrawEntryLabels(true);
        pieChart.setEntryLabelColor(Color.TRANSPARENT);
        pieChart.setRotationEnabled(false);
        pieChart.setLayerType(View.LAYER_TYPE_SOFTWARE, null);



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

            // 1.3% Floor
            float displayPercentage = Math.max(actualPercentage, entry.getValue() > 0 ? 1.3f : 0f);
            ac.setPercentage(displayPercentage);

            // USE THE NEW ENUM
            Category catInfo = Category.fromString(entry.getKey());
            ac.setColor(catInfo.getColor());

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

            if (category.getPercentage() >= 5f && getContext() != null) {
                // Simplified lookup
                int iconResId = Category.fromString(category.getName()).getIconRes();
                android.graphics.drawable.Drawable drawable = ContextCompat.getDrawable(getContext(), iconResId);

                if (drawable != null) {
                    drawable = drawable.mutate();
                    drawable.setColorFilter(new android.graphics.PorterDuffColorFilter(
                            Color.WHITE, android.graphics.PorterDuff.Mode.SRC_IN));
                    drawable.setBounds(0, -30, 80, 80);
                    entry.setIcon(drawable);
                }
            }
            entries.add(entry);
            colors.add(category.getColor());
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setSliceSpace(12f);
        dataSet.setSelectionShift(0f);
        dataSet.setDrawValues(false);
        dataSet.setDrawIcons(true);

        // This ensures icons stay centered even with the 1% floor shifts
        dataSet.setIconsOffset(new com.github.mikephil.charting.utils.MPPointF(8f, 0f));
        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        Paint paint = pieChart.getRenderer().getPaintRender();

        // 1. Set the Join and Cap to Round (This creates the rounded corners)
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);

        // 2. Set style to FILL_AND_STROKE
        paint.setStyle(Paint.Style.FILL_AND_STROKE);

        // 3. Set a Stroke Width.
        // A width of 10-15f will make the rounding very apparent.
        paint.setStrokeWidth(15f);

        // Compensate for the stroke thickness so the chart doesn't
        // grow outside its bounds. We do this by shrinking the Hole slightly
        // and ensuring the view has internal padding.
        //pieChart.setTransparentCircleRadius(95f); // Use this to "push" the chart in
        pieChart.setHoleRadius(77f); // Slightly smaller hole to keep the "meat" of the slice consistent

        // 4. Remove the PathEffect as it causes the "one end smaller" distortion
        paint.setPathEffect(null);
        paint.setAntiAlias(true);

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
}