package com.example.expensetracker;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HCardsViewAdapter extends RecyclerView.Adapter<HCardsViewAdapter.ViewHolder> {

    private static final int VIEW_TYPE_CARD = 0;
    private static final int VIEW_TYPE_HEADER = 1;
    private static final int VIEW_TYPE_COMPACT_CARD = 2;

    private Context context;
    private ArrayList<HomeCard> hCards= new ArrayList<HomeCard>();
    private final ArrayList<Object> displayItems = new ArrayList<>();
    private String currentMonthlyTrackerId;
    private boolean monthlySectionsEnabled;

    public HCardsViewAdapter(Context context) {
        this.context = context;
    }

    public void setCards(ArrayList<HomeCard> hCards) {
        this.hCards = hCards;
        currentMonthlyTrackerId = null;
        monthlySectionsEnabled = false;
        displayItems.clear();
        if (hCards != null) {
            displayItems.addAll(hCards);
        }
        notifyDataSetChanged();
    }

    public void setSections(List<HomeCard> monthlyCards, List<HomeCard> manualCards) {
        hCards = new ArrayList<>();
        monthlySectionsEnabled = true;
        currentMonthlyTrackerId = monthlyCards != null && !monthlyCards.isEmpty()
                ? monthlyCards.get(0).getTableID()
                : null;
        displayItems.clear();

        if (monthlyCards != null && !monthlyCards.isEmpty()) {
            displayItems.add("Tu tracker mensual");
            displayItems.addAll(monthlyCards);
            hCards.addAll(monthlyCards);
        }

        if (manualCards != null && !manualCards.isEmpty()) {
            displayItems.add("Otros trackers");
            displayItems.addAll(manualCards);
            hCards.addAll(manualCards);
        }

        notifyDataSetChanged();
    }

    public boolean isCardPosition(int position) {
        return position >= 0
                && position < displayItems.size()
                && displayItems.get(position) instanceof HomeCard;
    }

    public HomeCard getCardAtAdapterPosition(int position) {
        if (!isCardPosition(position)) {
            return null;
        }
        return (HomeCard) displayItems.get(position);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout;
        if (viewType == VIEW_TYPE_HEADER) {
            layout = R.layout.item_home_section_header;
        } else if (viewType == VIEW_TYPE_COMPACT_CARD) {
            layout = R.layout.home_cards_compact;
        } else {
            layout = R.layout.home_cards;
        }
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Object item = displayItems.get(position);
        if (item instanceof String) {
            holder.sectionHeader.setText((String) item);
            return;
        }

        HomeCard card = (HomeCard) item;
        boolean currentMonthly = monthlySectionsEnabled
                && card.isMonthly()
                && currentMonthlyTrackerId != null
                && currentMonthlyTrackerId.equals(card.getTableID());
        boolean oldMonthly = monthlySectionsEnabled && card.isMonthly() && !currentMonthly;
        boolean monthlySectionCard = monthlySectionsEnabled && card.isMonthly();
        boolean manualSectionCard = monthlySectionsEnabled && !card.isMonthly();
        boolean firstCardAfterHeader = position > 0 && displayItems.get(position - 1) instanceof String;

        holder.name.setText(card.getName());
        holder.setCardVerticalMargins(firstCardAfterHeader ? 6 : (monthlySectionCard ? 0 : 6), 12);
        LocalDate today = card.getCreationDate();
        String formattedDate = today.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL));
        if (holder.date != null) {
            holder.date.setVisibility(oldMonthly ? View.GONE : View.VISIBLE);
            String amountText = formatCurrency(card.getTotalAmount());
            holder.date.setText(card.isCerrado()
                    ? "Gastos totales: " + amountText
                    : (currentMonthly || manualSectionCard
                            ? "Gastos acumulados: " + amountText
                            : formattedDate));
        }
        holder.openBadge.setVisibility(oldMonthly && !card.isCerrado() ? View.VISIBLE : View.GONE);
        holder.positionClosedIconAfter(holder.openBadge.getVisibility() == View.VISIBLE
                ? R.id.txtOpenBadge
                : R.id.txtName);
        if (!card.isCerrado()) holder.icn_cerrado.setVisibility(View.GONE);
        if (card.isCerrado()) holder.icn_cerrado.setVisibility(View.VISIBLE);

        //Cuando haces click en la card, ir al expense report
        holder.mCardView.setOnClickListener(v -> {
            HCardDB.setSelected(card);
            Intent intent = new Intent(context, ExpenseActivityV2.class);
            intent.putExtra("trackerId", card.getTableID());
            context.startActivity(intent);
        });
    }


    @Override
    public int getItemCount() {
        return displayItems.size();
    }

    @Override
    public int getItemViewType(int position) {
        Object item = displayItems.get(position);
        if (item instanceof String) {
            return VIEW_TYPE_HEADER;
        }

        if (item instanceof HomeCard) {
            HomeCard card = (HomeCard) item;
            boolean oldMonthly = monthlySectionsEnabled
                    && card.isMonthly()
                    && currentMonthlyTrackerId != null
                    && !currentMonthlyTrackerId.equals(card.getTableID());

            if (oldMonthly) {
                return VIEW_TYPE_COMPACT_CARD;
            }
        }

        return VIEW_TYPE_CARD;
    }

    private String formatCurrency(double amount) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
        return format.format(amount);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }


    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView name;
        private TextView date;
        private Button btn;
        private ImageView icn_cerrado;
        private TextView openBadge;
        private TextView sectionHeader;
        MaterialCardView mCardView,viewB;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            sectionHeader = itemView.findViewById(R.id.txtHomeSectionHeader);
            if (sectionHeader != null) {
                return;
            }
            name = itemView.findViewById(R.id.txtName);
            date = itemView.findViewById(R.id.txtDate);
            openBadge = itemView.findViewById(R.id.txtOpenBadge);
            mCardView = itemView.findViewById(R.id.trackerCard);
            icn_cerrado = itemView.findViewById(R.id.icn_cerrado);

        }

        public boolean isSwipeable() {
            return false;
        }

        public void setCardVerticalMargins(int topDp, int bottomDp) {
            if (mCardView == null) {
                return;
            }

            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) mCardView.getLayoutParams();
            params.topMargin = dpToPx(topDp);
            params.bottomMargin = dpToPx(bottomDp);
            mCardView.setLayoutParams(params);
        }

        public void positionClosedIconAfter(int anchorId) {
            if (icn_cerrado == null) {
                return;
            }

            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) icn_cerrado.getLayoutParams();
            params.removeRule(RelativeLayout.RIGHT_OF);
            params.removeRule(RelativeLayout.END_OF);
            params.addRule(RelativeLayout.END_OF, anchorId);
            icn_cerrado.setLayoutParams(params);
        }
    }

}
