package com.example.expensetracker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.text.NumberFormat;
import java.util.Locale;

public class SaldosFragment extends Fragment {

    private TextView number_Gastos_GastosTotales;

    private TextView txt_Saldos_TotalGastosPersona1;
    private TextView number_Saldos_TotalGastosPersona1;
    private TextView txt_Saldos_GastosProporcionalesPersona1;
    private TextView number_Saldos_GastosProporcionalesPersona1;
    private TextView txt_Saldos_BalancePersona1;
    private TextView number_Saldos_BalancePersona1;

    private TextView txt_Saldos_TotalGastosPersona2;
    private TextView number_Saldos_TotalGastosPersona2;
    private TextView txt_Saldos_GastosProporcionalesPersona2;
    private TextView number_Saldos_GastosProporcionalesPersona2;
    private TextView txt_Saldos_BalancePersona2;
    private TextView number_Saldos_BalancePersona2;

    private TextView txt_Saldos_Deudor;
    private TextView number_Saldos_Deuda;

    private GastosFragment gastosFragment;

    public SaldosFragment() {
    }

    public void setGastosFragment(GastosFragment fragment) {
        gastosFragment = fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_saldos,container,false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    public void calculate() {

        if (number_Gastos_GastosTotales == null) {
            loadVariables(getView());
        }

        int[] percentages = SettingsDB.getPercentage(HCardDB.getSelected().getTableID());

        double totalGastos = 0;
        for (ExpenseRow row : gastosFragment.getRowsBoth()) {
            totalGastos += row.getValue();
        }

        String COUNTRY = "US";
        String LANGUAGE = "en";
        String str = NumberFormat.getCurrencyInstance(new Locale(LANGUAGE, COUNTRY)).format(totalGastos);
        number_Gastos_GastosTotales.setText(str);

        calculate1(percentages, totalGastos);
        calculate2(percentages, totalGastos);

        calculateDeudaYDeudor(percentages, totalGastos);

    }

    private void calculateDeudaYDeudor(int[] percentages, double totalGastos) {

        double proporcional1 = Math.round(((totalGastos) * percentages[0] / 100)*100.0)/100.0;
        double proporcional2 = Math.round(((totalGastos) * percentages[1] / 100)*100.0)/100.0;

        double totalPersona1 = 0;

        for(ExpenseRow row : gastosFragment.getRows1()) {
            totalPersona1 += row.getValue();
        }

        double totalPersona2 = 0;

        for(ExpenseRow row : gastosFragment.getRows2()) {
            totalPersona2 += row.getValue();
        }

        double balancePersona1 = totalPersona1 - proporcional1;
        double balancePersona2 = totalPersona2 - proporcional2;

        if (balancePersona1 < 0) {
            String COUNTRY = "US";
            String LANGUAGE = "en";
            String str = NumberFormat.getCurrencyInstance(new Locale(LANGUAGE, COUNTRY)).format(balancePersona1*-1);
            txt_Saldos_Deudor.setText(SettingsDB.getSetting(HCardDB.getSelected()).getName1());
            number_Saldos_Deuda.setText(str);
        }

        if (balancePersona2 < 0) {
            String COUNTRY = "US";
            String LANGUAGE = "en";
            String str = NumberFormat.getCurrencyInstance(new Locale(LANGUAGE, COUNTRY)).format(balancePersona2*-1);
            txt_Saldos_Deudor.setText(SettingsDB.getSetting(HCardDB.getSelected()).getName2());
            number_Saldos_Deuda.setText(str);
        }

        if (balancePersona1 == balancePersona2) {
            txt_Saldos_Deudor.setText("Nadie :)");
            number_Saldos_Deuda.setText("$0.00");
        }
    }


    private void calculate1(int[] percentages, double totalGastos) {
        String COUNTRY = "US";
        String LANGUAGE = "en";

        txt_Saldos_TotalGastosPersona1.setText("Total Gastos " + SettingsDB.getSetting(HCardDB.getSelected()).getName1());
        txt_Saldos_GastosProporcionalesPersona1.setText("Proporcional " + SettingsDB.getSetting(HCardDB.getSelected()).getName1() + " (" + String.valueOf(percentages[0]) + "%)");
        txt_Saldos_BalancePersona1.setText("Balance " + SettingsDB.getSetting(HCardDB.getSelected()).getName1());

        double totalPersona1 = 0;

        for(ExpenseRow row : gastosFragment.getRows1()) {
            totalPersona1 += row.getValue();
        }
        String str = NumberFormat.getCurrencyInstance(new Locale(LANGUAGE, COUNTRY)).format(totalPersona1);
        number_Saldos_TotalGastosPersona1.setText(str);

        double proporcional1 = Math.round(((totalGastos) * percentages[0] / 100)*100.0)/100.0;

        str = NumberFormat.getCurrencyInstance(new Locale(LANGUAGE, COUNTRY)).format(proporcional1);
        number_Saldos_GastosProporcionalesPersona1.setText(str);

        double balancePersona1 = totalPersona1 - proporcional1;
        str = NumberFormat.getCurrencyInstance(new Locale(LANGUAGE, COUNTRY)).format(balancePersona1);
        number_Saldos_BalancePersona1.setText(str);

    }

    private void calculate2(int[] percentages, double totalGastos) {

        String COUNTRY = "US";
        String LANGUAGE = "en";

        txt_Saldos_TotalGastosPersona2.setText("Total Gastos " + SettingsDB.getSetting(HCardDB.getSelected()).getName2());
        txt_Saldos_GastosProporcionalesPersona2.setText("Proporcional " + SettingsDB.getSetting(HCardDB.getSelected()).getName2() + " (" + String.valueOf(percentages[1]) + "%)");
        txt_Saldos_BalancePersona2.setText("Balance " + SettingsDB.getSetting(HCardDB.getSelected()).getName2());

        double totalPersona2 = 0;

        for(ExpenseRow row : gastosFragment.getRows2()) {
            totalPersona2 += row.getValue();
        }
        String str = NumberFormat.getCurrencyInstance(new Locale(LANGUAGE, COUNTRY)).format(totalPersona2);
        number_Saldos_TotalGastosPersona2.setText(str);

        double proporcional2 = Math.round(((totalGastos) * percentages[1] / 100)*100.0)/100.0;

        str = NumberFormat.getCurrencyInstance(new Locale(LANGUAGE, COUNTRY)).format(proporcional2);
        number_Saldos_GastosProporcionalesPersona2.setText(str);

        double balancePersona2 = totalPersona2 - proporcional2;
        str = NumberFormat.getCurrencyInstance(new Locale(LANGUAGE, COUNTRY)).format(balancePersona2);
        number_Saldos_BalancePersona2.setText(str);

    }


    public void loadVariables(View view) {
        number_Gastos_GastosTotales = view.findViewById(R.id.number_Gastos_GastosTotales);

        txt_Saldos_TotalGastosPersona1 = view.findViewById(R.id.txt_Saldos_TotalGastosPersona1);
        number_Saldos_TotalGastosPersona1 = view.findViewById(R.id.number_Saldos_TotalGastosPersona1);
        txt_Saldos_GastosProporcionalesPersona1 = view.findViewById(R.id.txt_Saldos_GastosProporcionalesPersona1);
        number_Saldos_GastosProporcionalesPersona1 = view.findViewById(R.id.number_Saldos_GastosProporcionalesPersona1);
        txt_Saldos_BalancePersona1 = view.findViewById(R.id.txt_Saldos_BalancePersona1);
        number_Saldos_BalancePersona1 = view.findViewById(R.id.number_Saldos_BalancePersona1);

        txt_Saldos_TotalGastosPersona2 = view.findViewById(R.id.txt_Saldos_TotalGastosPersona2);
        number_Saldos_TotalGastosPersona2 = view.findViewById(R.id.number_Saldos_TotalGastosPersona2);
        txt_Saldos_GastosProporcionalesPersona2 = view.findViewById(R.id.txt_Saldos_GastosProporcionalesPersona2);
        number_Saldos_GastosProporcionalesPersona2 = view.findViewById(R.id.number_Saldos_GastosProporcionalesPersona2);
        txt_Saldos_BalancePersona2 = view.findViewById(R.id.txt_Saldos_BalancePersona2);
        number_Saldos_BalancePersona2 = view.findViewById(R.id.number_Saldos_BalancePersona2);

        txt_Saldos_Deudor = view.findViewById(R.id.txt_Saldos_Deudor);
        number_Saldos_Deuda = view.findViewById(R.id.number_Saldos_Deuda);
    }


}
