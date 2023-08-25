package com.example.expensetracker;

import android.content.Context;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.sql.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class HCardDB {

    private static LinkedHashMap<String, HomeCard> expensesMap;
    private static HomeCard selected;

    public static void initialize() {
        if (expensesMap == null) {
            expensesMap = new LinkedHashMap<String, HomeCard>();
        }
    }

    public static HomeCard getSelected() {
        return selected;
    }

    public static void setSelected(HomeCard selected) {
        HCardDB.selected = selected;
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void loadDB(Connection conn) {
        if (expensesMap == null) initialize();
        expensesMap.clear();
        String ql = "SELECT * FROM allTables";
        Statement st = null;
        try {
            st = conn.createStatement();
            ResultSet result = st.executeQuery(ql);
            while(result.next()) {
                String id = result.getString("tableName").substring(4);
                LocalDate date = (result.getDate("creationDate")).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                String reportName = result.getString("tableDescription");
                if(!expensesMap.containsKey(id)){
                    expensesMap.put(id, new HomeCard(id, date, reportName));
                }
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        if (st!=null) {
            try {
                st.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
    }

    public static void setDB(LinkedHashMap<String, HomeCard> newMap) {
        if (expensesMap == null) initialize();
        expensesMap = newMap;
    }

    public static void createDBTable(HomeCard hc, Context context) {
        DBHandler handler = new DBHandler();
        System.out.println("Connecting to a selected database...");
        Connection connection = handler.getConnection(context);
        System.out.println("Connected database successfully...");

        int id = getBiggestID() + 1;

        try {
            Statement st = connection.createStatement();
            System.out.println("Creating table in given database...");
            String ql = "CREATE TABLE DATA" + id + " " +
                    "(id INT NOT NULL AUTO_INCREMENT, " +
                    " Description VARCHAR(45) not NULL, " +
                    " Date DATE not NULL, " +
                    " Value DOUBLE not NULL, " +
                    " Who VARCHAR(45) not NULL, " +
                    " primary key (id))";
            st.executeUpdate(ql);
            System.out.println("Created table in given database...");
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        //Adding the data from this table to the allTables table
        try {
            System.out.println("Trying to add table data to allTables");
            String insert = "INSERT INTO allTables(tableName,creationDate,tableDescription)" + "VALUES (?,?,?)";
            PreparedStatement pst = connection.prepareStatement(insert);
            pst.setString(1, hc.getTableID());
            pst.setDate(2, Date.valueOf(String.valueOf(hc.getCreationDate())));
            pst.setString(3,hc.getName());
            pst.executeUpdate();
            System.out.println("Added data to allTables");

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        // close connection
        try {
            connection.close();
            System.out.println("Connection closed");
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public static Boolean containsID(String id) {
        if (expensesMap == null) initialize();
        return expensesMap.containsKey(id);
    }

    public static void addExpense(String id, HomeCard hc) {
        if (expensesMap == null) initialize();
        if(!expensesMap.containsKey(id)) {
            expensesMap.put(id, hc);
        }
    }

    public static int getBiggestID() {
        int biggest = 0;
        if(!expensesMap.isEmpty()) {
            for (String id : expensesMap.keySet()) {
                int intID = Integer.parseInt(id);
                if (intID > biggest) {
                    biggest = intID;
                }
            }
        }
        return biggest;
    }

    public static Boolean isEmpty() {
        if (expensesMap == null) initialize();
        return expensesMap.isEmpty();
    }

    public static Boolean isNull() {
        if(expensesMap == null) return true;
        return false;
    }

    public static HashMap<String, HomeCard> getDB() {
        if (expensesMap == null) initialize();
        return expensesMap;
    }

    public static ArrayList<HomeCard> getReports() {
        ArrayList<HomeCard> answer = new ArrayList<HomeCard>();
        for(String s : expensesMap.keySet()) {
            answer.add(0, expensesMap.get(s));
        }
        return answer;
    }

    public static Boolean containsDescription(String name) {
        if (expensesMap == null) initialize();
        for(String s : expensesMap.keySet()) {
            HomeCard hc = expensesMap.get(s);
            String currName = hc.getName();
            if(currName.equals(name)) {
                return true;
            }
        }
        return false;
    }

    public static void removeReport(Connection connection, String id) {
        //remove table from database
        if (expensesMap == null) initialize();
        String ql = "DROP TABLE DATA" + id + " ;";
        Statement st = null;
        try {
            st = connection.createStatement();
            st.executeUpdate(ql);
            System.out.println("Table deleted");
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        if (st!=null) {
            try {
                st.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }

        //remove row from allTables table
        ql = "DELETE FROM allTables WHERE tableName = ?";
        String name = "DATA" + id;
        PreparedStatement pst = null;
        try {
            pst = connection.prepareStatement(ql);
            pst.setString(1,name);
            pst.executeUpdate();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        if (pst!=null) {
            try {
                pst.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
        //remove expenseReport from hashmap
        expensesMap.remove(id);
    }

}
