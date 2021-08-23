package com.example.expensetracker;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.sql.Connection;
import java.sql.SQLException;

public class ItemDragSwipeCallback extends ItemTouchHelper.Callback {

    CallBackItemTouch callBackItemTouch;

    public ItemDragSwipeCallback(CallBackItemTouch callBackItemTouch) {
        this.callBackItemTouch = callBackItemTouch;
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        final int swipeFlags = ItemTouchHelper.START;
        return makeMovementFlags(0,swipeFlags);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        callBackItemTouch.itemTuchOnMove(viewHolder.getOldPosition(),target.getAdapterPosition());
        return true;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        callBackItemTouch.onSwiped(viewHolder,viewHolder.getAdapterPosition());
    }

    /*@Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

        View itemView = viewHolder.itemView;
        int backgroundCornerOffset = 25; //so mBackground is behind the rounded corners of itemView

        int iconMargin = (itemView.getHeight() - mIcon.getIntrinsicHeight()) / 2;
        int iconTop = itemView.getTop() + (itemView.getHeight() - mIcon.getIntrinsicHeight()) / 2;
        int iconBottom = iconTop + mIcon.getIntrinsicHeight();

        if (dX > 0) { // Swiping to the right
            int iconLeft = itemView.getLeft() + iconMargin;
            int iconRight = iconLeft + mIcon.getIntrinsicWidth();
            mIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);

            mBackground.setBounds(itemView.getLeft(), itemView.getTop(),
                    itemView.getLeft() + ((int) dX) + backgroundCornerOffset, itemView.getBottom());
        }
        else if (dX < 0) { // Swiping to the left
            int iconLeft = itemView.getRight() - iconMargin - mIcon.getIntrinsicWidth();
            int iconRight = itemView.getRight() - iconMargin;
            mIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);

            mBackground.setBounds(itemView.getRight() + ((int) dX) - backgroundCornerOffset,
                    itemView.getTop(), itemView.getRight(), itemView.getBottom());
        }
        else { // view is unSwiped
            mIcon.setBounds(0, 0, 0, 0);
            mBackground.setBounds(0, 0, 0, 0);
        }

        mBackground.draw(c);
        mIcon.draw(c);
    }
*/
    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {

        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }

        else {
            final View foregroundView = ((HCardsViewAdapter.ViewHolder)viewHolder).viewB;
            getDefaultUIUtil().onDrawOver(c,recyclerView,foregroundView,dX,dY,actionState,isCurrentlyActive);
        }

        /*View itemView = viewHolder.itemView;
        int backgroundCornerOffset = 25; //so mBackground is behind the rounded corners of itemView

        int iconMargin = (itemView.getHeight() - mIcon.getIntrinsicHeight()) / 2;
        int iconTop = itemView.getTop() + (itemView.getHeight() - mIcon.getIntrinsicHeight()) / 2;
        int iconBottom = iconTop + mIcon.getIntrinsicHeight();

        if (dX > 0) { // Swiping to the right
            int iconLeft = itemView.getLeft() + iconMargin;
            int iconRight = iconLeft + mIcon.getIntrinsicWidth();
            mIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);

            mBackground.setBounds(itemView.getLeft(), itemView.getTop(),
                    itemView.getLeft() + ((int) dX) + backgroundCornerOffset, itemView.getBottom());
        }
        else if (dX < 0) { // Swiping to the left
            int iconLeft = itemView.getRight() - iconMargin - mIcon.getIntrinsicWidth();
            int iconRight = itemView.getRight() - iconMargin;
            mIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);

            mBackground.setBounds(itemView.getRight() + ((int) dX) - backgroundCornerOffset,
                    itemView.getTop(), itemView.getRight(), itemView.getBottom());
        }
        else { // view is unSwiped
            mIcon.setBounds(0, 0, 0, 0);
            mBackground.setBounds(0, 0, 0, 0);
        }

        mBackground.draw(c);
        mIcon.draw(c);*/
    }

    @Override
    public void onChildDrawOver(@NonNull Canvas c, @NonNull RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        //super.onChildDrawOver(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

        if (actionState != ItemTouchHelper.ACTION_STATE_DRAG) {
            final View foregroundView = ((HCardsViewAdapter.ViewHolder)viewHolder).mCardView;
            getDefaultUIUtil().onDraw(c,recyclerView,foregroundView,dX / 3,dY,actionState,isCurrentlyActive);
        }
    }

    @Override
    public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        final View foregroundView = ((HCardsViewAdapter.ViewHolder)viewHolder).mCardView;
        getDefaultUIUtil().clearView(foregroundView);
    }
}
