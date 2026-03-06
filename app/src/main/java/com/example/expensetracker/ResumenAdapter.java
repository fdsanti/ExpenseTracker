package com.example.expensetracker;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Locale;

public class ResumenAdapter extends RecyclerView.Adapter<ResumenAdapter.ViewHolder> {

    private List<AnalysisCategory> categoryList;
    private Context context;

    public ResumenAdapter(List<AnalysisCategory> categoryList) {
        this.categoryList = categoryList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_resumen_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AnalysisCategory category = categoryList.get(position);

        // Set Category Name
        holder.tvCatName.setText(category.getName());

        // Set Total Value (Formatted as currency or number)
        String formattedValue = "$" + String.format(Locale.getDefault(), "%,.2f", category.getTotal());
        holder.tvCatTotal.setText(formattedValue);

        // Set Percentage Badge
        holder.tvCatPercentage.setText(String.format(Locale.getDefault(), "%.1f%%", category.getPercentage()));

        // Set Icon Background Color (Matches Chart Slice)
        holder.viewIconBg.setBackgroundTintList(ColorStateList.valueOf(category.getColor()));

        // Set Icon (We will implement a helper to get the icon later)
        holder.ivCatIcon.setImageResource(Category.fromString(category.getName()).getIconRes());    }

    @Override
    public int getItemCount() {
        return categoryList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        View viewIconBg;
        ImageView ivCatIcon;
        TextView tvCatName, tvCatPercentage, tvCatTotal;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            viewIconBg = itemView.findViewById(R.id.view_icon_bg);
            ivCatIcon = itemView.findViewById(R.id.iv_cat_icon);
            tvCatName = itemView.findViewById(R.id.tv_cat_name);
            tvCatPercentage = itemView.findViewById(R.id.tv_cat_percentage);
            tvCatTotal = itemView.findViewById(R.id.tv_cat_total);
        }
    }

}