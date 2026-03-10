package com.example.expensetracker;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class HCardDB {

    private static LinkedHashMap<String, HomeCard> expensesMap;
    private static HomeCard selected;

    public static void initialize() {
        if (expensesMap == null) {
            expensesMap = new LinkedHashMap<>();
        }
    }

    public static HomeCard getSelected() {
        return selected;
    }

    public static void setCerrado(Boolean cerradoValue) {
        if (selected == null || expensesMap == null) return;

        for (String s : expensesMap.keySet()) {
            HomeCard current = expensesMap.get(s);
            if (current != null && current.getTableID().equals(selected.getTableID())) {
                current.setCerrado(cerradoValue);
            }
        }
        selected.setCerrado(cerradoValue);
    }

    public static void setSelected(HomeCard selected) {
        HCardDB.selected = selected;
    }

    public static void clearMap() {
        if (expensesMap == null) {
            initialize();
        }
        expensesMap.clear();
    }

    public static void addExpense(String key, HomeCard hc) {
        if (expensesMap == null) initialize();
        if (!expensesMap.containsKey(key)) {
            expensesMap.put(key, hc);
        }
    }

    public static int getBiggestID() {
        int biggest = 0;
        if (expensesMap == null) initialize();

        for (String key : expensesMap.keySet()) {
            HomeCard homeCard = expensesMap.get(key);
            if (homeCard == null) continue;

            String rawId = homeCard.getId();
            if (rawId == null || rawId.isEmpty()) {
                rawId = homeCard.getTableID();
            }
            if (rawId == null) continue;

            String numericPart = rawId.startsWith("DATA") ? rawId.substring(4) : rawId;
            try {
                int intID = Integer.parseInt(numericPart);
                if (intID > biggest) {
                    biggest = intID;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return biggest;
    }

    public static Boolean isEmpty() {
        if (expensesMap == null) initialize();
        return expensesMap.isEmpty();
    }

    public static Boolean isNull() {
        return expensesMap == null;
    }


    public static ArrayList<HomeCard> getReportsActuals() {
        ArrayList<HomeCard> answer = new ArrayList<>();
        if (expensesMap == null) initialize();
        for (String s : expensesMap.keySet()) {
            if (!expensesMap.get(s).isCerrado()) answer.add(0, expensesMap.get(s));
        }
        return answer;
    }

    public static ArrayList<HomeCard> getReportsPast() {
        ArrayList<HomeCard> answer = new ArrayList<>();
        if (expensesMap == null) initialize();
        for (String s : expensesMap.keySet()) {
            if (expensesMap.get(s).isCerrado()) answer.add(0, expensesMap.get(s));
        }
        return answer;
    }

    public static void removeReportFromArrayList(String tableID) {
        if (expensesMap == null) initialize();

        String keyToRemove = null;
        for (Map.Entry<String, HomeCard> entry : expensesMap.entrySet()) {
            HomeCard value = entry.getValue();
            if (value != null && value.getTableID().equals(tableID)) {
                keyToRemove = entry.getKey();
                break;
            }
        }

        if (keyToRemove != null) {
            expensesMap.remove(keyToRemove);
        }
    }

    public static Boolean containsDescription(String name) {
        if (expensesMap == null) initialize();
        for (String s : expensesMap.keySet()) {
            HomeCard hc = expensesMap.get(s);
            String currName = hc.getName();
            if (currName.equals(name)) {
                return true;
            }
        }
        return false;
    }
}
