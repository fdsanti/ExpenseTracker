package com.example.expensetracker.ui.expense.dialogs;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensetracker.R;
import com.example.expensetracker.data.DefaultCategories;
import com.example.expensetracker.model.Category;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CategoryEditorAdapter extends RecyclerView.Adapter<CategoryEditorAdapter.ViewHolder> {

    public interface Listener {
        void onEditCategory(Category category);
        void onDeleteCategory(Category category);
        void onStartDrag(ViewHolder viewHolder);
        void onOrderChanged(List<Category> categories);
    }

    private final List<Category> categories;
    private final Listener listener;

    public CategoryEditorAdapter(List<Category> categories, Listener listener) {
        this.categories = new ArrayList<>(categories);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_edit_category, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Category category = categories.get(position);
        holder.txtCategoryName.setText(category.getName());

        boolean isOthers = DefaultCategories.OTHERS_ID.equals(category.getId());
        holder.btnDeleteCategory.setAlpha(isOthers ? 0.35f : 1f);
        holder.btnDeleteCategory.setEnabled(!isOthers);

        holder.btnEditCategory.setOnClickListener(v -> listener.onEditCategory(category));
        holder.btnDeleteCategory.setOnClickListener(v -> listener.onDeleteCategory(category));
        holder.btnDragCategory.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                listener.onStartDrag(holder);
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    public boolean moveItem(int fromPosition, int toPosition) {
        if (fromPosition < 0 || toPosition < 0
                || fromPosition >= categories.size()
                || toPosition >= categories.size()) {
            return false;
        }

        Collections.swap(categories, fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);
        return true;
    }

    public void notifyOrderChanged() {
        listener.onOrderChanged(new ArrayList<>(categories));
    }

    public void updateCategoryName(String categoryId, String name) {
        for (int i = 0; i < categories.size(); i++) {
            Category category = categories.get(i);
            if (category.getId().equals(categoryId)) {
                categories.set(i, new Category(category.getId(), name, category.isActive(), category.getOrder()));
                notifyItemChanged(i);
                return;
            }
        }
    }

    public void addCategory(Category category) {
        categories.add(category);
        notifyItemInserted(categories.size() - 1);
    }

    public void removeCategory(String categoryId) {
        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i).getId().equals(categoryId)) {
                categories.remove(i);
                notifyItemRemoved(i);
                return;
            }
        }
    }

    public List<Category> getCategories() {
        return new ArrayList<>(categories);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final View btnDeleteCategory;
        private final TextView txtCategoryName;
        private final ImageButton btnEditCategory;
        private final ImageButton btnDragCategory;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            btnDeleteCategory = itemView.findViewById(R.id.btnDeleteCategory);
            txtCategoryName = itemView.findViewById(R.id.txtCategoryName);
            btnEditCategory = itemView.findViewById(R.id.btnEditCategory);
            btnDragCategory = itemView.findViewById(R.id.btnDragCategory);
        }
    }
}
