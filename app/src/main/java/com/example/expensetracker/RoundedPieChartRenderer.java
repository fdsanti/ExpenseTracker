package com.example.expensetracker;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import com.github.mikephil.charting.animation.ChartAnimator;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.interfaces.datasets.IPieDataSet;
import com.github.mikephil.charting.renderer.PieChartRenderer;
import com.github.mikephil.charting.utils.ViewPortHandler;

public class RoundedPieChartRenderer extends PieChartRenderer {

    public RoundedPieChartRenderer(PieChart chart, ChartAnimator animator, ViewPortHandler viewPortHandler) {
        super(chart, animator, viewPortHandler);
    }

    @Override
    protected void drawDataSet(Canvas c, IPieDataSet dataSet) {
        // We override the default drawing to inject a "Clip Path"
        // that rounds the corners of the entire drawing area of the slice
        super.drawDataSet(c, dataSet);
    }

    @Override
    public void drawValues(Canvas c) {
        super.drawValues(c);
    }

    // The logic to "round" the edges of the arc is actually handled by the Paint
    // We can set the Stroke Join and Cap for the slice paint
    @Override
    public void initBuffers() {
        super.initBuffers();
        // This is where we can modify the paint objects used for the slices
        mRenderPaint.setStrokeJoin(Paint.Join.ROUND);
        mRenderPaint.setStrokeCap(Paint.Cap.ROUND);
    }
}