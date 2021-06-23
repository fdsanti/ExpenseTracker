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
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.ArrayList;

public class HCardsViewAdapter extends RecyclerView.Adapter<HCardsViewAdapter.ViewHolder> {

    private Context context;
    private ArrayList<HomeCard> hCards= new ArrayList<HomeCard>();

    public HCardsViewAdapter(Context context) {
        this.context = context;
    }

    public void setCards(ArrayList<HomeCard> hCards) {
        this.hCards = hCards;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.home_cards, parent, false);
        ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.name.setText(hCards.get(position).getName());
        LocalDate today = hCards.get(position).getCreationDate();
        String formattedDate = today.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL));
        holder.date.setText(formattedDate);

        //Crear popup al tappear en el boton de Eliminar
        holder.btn.setOnClickListener(v -> {
            MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(context);
            dialog.setTitle("¿Eliminar reporte?");
            dialog.setMessage("¿Estás seguro que querés eliminar el reporte " + holder.name.getText().toString() + "?");
            dialog.setIcon(R.drawable.ic_delete);


            dialog.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });
            //OnClickListener para el boton de Eliminar (dentro del popup)
            dialog.setPositiveButton("Eliminar", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ProgressDialog progressDialog = new ProgressDialog(context);
                    progressDialog.show();
                    progressDialog.setContentView(R.layout.progress_dialog);
                    progressDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

                    Handler handlerUI = new Handler();
                    Runnable runnable = new Runnable() {
                        @Override
                        public void run() {
                            synchronized (this) {
                                DBHandler handler = new DBHandler();
                                Connection connection = handler.getConnection(context);
                                HCardDB.removeReport(connection, hCards.get(position).getId());
                                SettingsDB.removeSettings(hCards.get(position), context);
                                try {
                                    connection.close();
                                } catch (SQLException throwables) {
                                    throwables.printStackTrace();
                                }
                            }

                            handlerUI.post(new Runnable() {
                                @Override
                                public void run() {
                                    hCards.remove(position);
                                    notifyDataSetChanged();
                                    progressDialog.dismiss();
                                    Toast.makeText(context, "El reporte se ha eliminado", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    };
                    Thread thread = new Thread(runnable);
                    thread.start();
                }
            });

            dialog.show();


        });

        //Cuando haces click en la card, ir al expense report
        holder.mCardView.setOnClickListener(v -> {
            HCardDB.setSelected(hCards.get(position));
            if (!SettingsDB.isInDB(hCards.get(position))) {
                Intent intent = new Intent(context, SettingsActivity.class);
                context.startActivity(intent);
            }
            else {
                Intent intent = new Intent(context, ExpenseActivity.class);
                context.startActivity(intent);
            }
        });
    }


    @Override
    public int getItemCount() {
        return hCards.size();
    }

    public void addHCard(HomeCard hc) {
        hCards.add(0,hc);
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView name;
        private TextView date;
        private Button btn;
        private MaterialCardView mCardView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.txtName);
            date = itemView.findViewById(R.id.txtDate);
            btn = itemView.findViewById(R.id.btn_eliminar);
            mCardView = itemView.findViewById(R.id.trackerCard);
        }
    }

}
