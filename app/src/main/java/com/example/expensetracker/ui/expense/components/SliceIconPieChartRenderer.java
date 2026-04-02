package com.example.expensetracker.ui.expense.components;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import com.github.mikephil.charting.animation.ChartAnimator;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.interfaces.datasets.IPieDataSet;
import com.github.mikephil.charting.renderer.PieChartRenderer;
import com.github.mikephil.charting.utils.MPPointF;
import com.github.mikephil.charting.utils.Utils;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.util.List;

public class SliceIconPieChartRenderer extends PieChartRenderer {

    private final float iconAngleOffsetDegrees;
    private final float iconRadiusOffsetPx;

    public SliceIconPieChartRenderer(
            PieChart chart,
            ChartAnimator animator,
            ViewPortHandler viewPortHandler,
            float iconAngleOffsetDegrees,
            float iconRadiusOffsetDp
    ) {
        super(chart, animator, viewPortHandler);
        this.iconAngleOffsetDegrees = iconAngleOffsetDegrees;
        this.iconRadiusOffsetPx = Utils.convertDpToPixel(iconRadiusOffsetDp);
    }

    @Override
    public void drawValues(Canvas c) {
        PieData data = mChart.getData();
        if (data == null) {
            return;
        }

        MPPointF center = mChart.getCenterCircleBox();

        float radius = mChart.getRadius();
        float rotationAngle = mChart.getRotationAngle();
        float[] drawAngles = mChart.getDrawAngles();
        float[] absoluteAngles = mChart.getAbsoluteAngles();

        float phaseX = mAnimator.getPhaseX();
        float phaseY = mAnimator.getPhaseY();

        final float holeRadiusPercent = mChart.getHoleRadius() / 100f;
        float labelRadiusOffset = radius / 10f * 3.6f;

        if (mChart.isDrawHoleEnabled()) {
            labelRadiusOffset = (radius - (radius * holeRadiusPercent)) / 2f;
        }

        final float labelRadius = radius - labelRadiusOffset + iconRadiusOffsetPx;
        List<IPieDataSet> dataSets = data.getDataSets();

        int xIndex = 0;

        c.save();

        for (int i = 0; i < dataSets.size(); i++) {
            IPieDataSet dataSet = dataSets.get(i);

            if (!dataSet.isDrawIconsEnabled()) {
                xIndex += dataSet.getEntryCount();
                continue;
            }

            final float sliceSpace = getSliceSpace(dataSet);

            MPPointF iconsOffset = MPPointF.getInstance(dataSet.getIconsOffset());
            iconsOffset.x = Utils.convertDpToPixel(iconsOffset.x);
            iconsOffset.y = Utils.convertDpToPixel(iconsOffset.y);

            for (int j = 0; j < dataSet.getEntryCount(); j++) {
                PieEntry entry = dataSet.getEntryForIndex(j);

                float angle;
                if (xIndex == 0) {
                    angle = 0f;
                } else {
                    angle = absoluteAngles[xIndex - 1] * phaseX;
                }

                final float sliceAngle = drawAngles[xIndex];
                final float sliceSpaceMiddleAngle = sliceSpace / (Utils.FDEG2RAD * labelRadius);
                final float angleOffset = (sliceAngle - sliceSpaceMiddleAngle / 2f) / 2f;

                angle += angleOffset;

                final float transformedAngle =
                        rotationAngle + angle * phaseY + iconAngleOffsetDegrees;

                final float sliceXBase = (float) Math.cos(transformedAngle * Utils.FDEG2RAD);
                final float sliceYBase = (float) Math.sin(transformedAngle * Utils.FDEG2RAD);

                Drawable icon = entry.getIcon();
                if (icon != null) {
                    float x = (labelRadius + iconsOffset.y) * sliceXBase + center.x;
                    float y = (labelRadius + iconsOffset.y) * sliceYBase + center.y;
                    y += iconsOffset.x;

                    Utils.drawImage(
                            c,
                            icon,
                            (int) x,
                            (int) y,
                            icon.getIntrinsicWidth(),
                            icon.getIntrinsicHeight()
                    );
                }

                xIndex++;
            }

            MPPointF.recycleInstance(iconsOffset);
        }

        MPPointF.recycleInstance(center);
        c.restore();
    }
}