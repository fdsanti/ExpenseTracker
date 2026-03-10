package com.example.expensetracker;
import java.util.HashMap;
import java.util.Map;

public class SettingsDB {

    private static HashMap<String, Settings> settingsDB;

    public static void initialize() {
        settingsDB = new HashMap<String, Settings>();
    }

    public static Settings getSetting(HomeCard hr) {
        for (String s : settingsDB.keySet()) {
            if (s.equals(hr.getTableID())) {
                return settingsDB.get(s);
            }
        }
        return null;
    }

    public static void removeReportFromArrayList(String tableID) {
        for (Map.Entry<String, Settings> entry : settingsDB.entrySet()) {
            String key = entry.getKey();
            Settings value = entry.getValue();
            if (value.getTableID().toString().equals(tableID)) {
                settingsDB.remove(key);
                break;
            }
        }
    }

    public static HashMap<String,Settings> getHashMap() {
        if (settingsDB == null) initialize();
        return settingsDB;
    }

    //get an int array for % for each user
    public static int[] getPercentage(String tableID){
        int[] result = new int[2];

        if(settingsDB == null) initialize();
        if(settingsDB.isEmpty()) return new int[]{-1,-1};

        for(String s : settingsDB.keySet()) {
            Settings currSett = settingsDB.get(s);
            if (currSett.getTableID().equals(tableID)) {
                double income1 = currSett.getIncome(1);
                double income2 = currSett.getIncome(2);
                double totalIncome = income1 + income2;
                int income1Avg = (int) Math.round(income1*100/totalIncome);
                int income2Avg = (int) Math.round(income2*100/totalIncome);
                result[0] = income1Avg;
                result[1] = income2Avg;
                return result;
            }
        }
        return new int[]{-1,-1};
    }

    public static Boolean isNull() {
        if (settingsDB == null) return true;
        return false;
    }

    public static Boolean isInDB(HomeCard hc) {
        if (settingsDB != null) {
            for (String s : settingsDB.keySet()) {
                if (hc.getTableID().equals(settingsDB.get(s).getTableID())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void addToDB(Settings set) {
        if (settingsDB == null) initialize();
        settingsDB.put(set.getTableID(), set);
    }

    public static void clear() {
        if (settingsDB == null) initialize();
        settingsDB.clear();
    }

}
