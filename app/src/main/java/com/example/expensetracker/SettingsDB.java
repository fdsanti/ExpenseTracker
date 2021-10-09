package com.example.expensetracker;

import android.content.Context;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;

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

    public static void loadDB(Connection conn) {
        if (settingsDB == null) initialize();
        String ql = "SELECT * FROM settings";
        Statement st = null;
        try {
            st = conn.createStatement();
            ResultSet result = st.executeQuery(ql);
            while(result.next()) {
                String tableName = result.getString("tableName");
                String name1 = result.getString("name1");
                int sueldo1 = result.getInt("sueldo1");
                String name2 = result.getString("name2");
                int sueldo2 = result.getInt("sueldo2");
                settingsDB.put(tableName, new Settings(tableName, name1, sueldo1, name2, sueldo2));
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
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

    public static void save(Connection connection, Settings set){
        System.out.println("Trying to add table data to settings");
        String replace = "INSERT INTO settings(tableName,name1,sueldo1,name2,sueldo2)" + "VALUES (?,?,?,?,?)"
                + "ON DUPLICATE KEY UPDATE name1 = VALUES(name1), sueldo1 = VALUES(sueldo1), name2 = VALUES(name2), sueldo2 = VALUES(sueldo2)";
        try {
            PreparedStatement pst = connection.prepareStatement(replace);
            pst.setString(1, set.getTableID());
            pst.setString(2, set.getName1());
            pst.setInt(3, set.getIncome1());
            pst.setString(4, set.getName2());
            pst.setInt(5, set.getIncome2());
            pst.executeUpdate();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        System.out.println("Added data to settings");
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

    public static void removeSettings(HomeCard hc, Context context) {
        if (isInDB(hc)) {
            System.out.println("Expense is in db and trying to delete it from settings table");
            DBHandler handler = new DBHandler();
            Connection connection = handler.getConnection(context);
            String id = hc.getId();

            //remove row from settings table
            String ql = "DELETE FROM settings WHERE tableName = ?";
            String name = "DATA" + id;
            try {
                PreparedStatement pst = connection.prepareStatement(ql);
                pst.setString(1, name);
                pst.executeUpdate();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }

            //remove expenseReport from hashmap
            System.out.println("Deleting from Map");
            settingsDB.remove(name);

            //close connection
            try {
                connection.close();
                System.out.println("Connection closed");
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
    }

}
