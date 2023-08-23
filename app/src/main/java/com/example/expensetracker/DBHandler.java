package com.example.expensetracker;

import android.content.Context;
import android.os.StrictMode;
import android.widget.Toast;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBHandler extends Configs{
    Connection dbconnection;

    public Connection getConnection (Context context) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        if (dbconnection == null)  {
            String urlConexionMySQL = "";
            urlConexionMySQL = "jdbc:mysql://" + Configs.dbhost + ":" + Configs.dbport+ "/" + Configs.dbname;

            try {
                Class.forName("com.mysql.jdbc.Driver");
                System.out.println("Trying to connect");
                dbconnection = DriverManager.getConnection(urlConexionMySQL, Configs.dbuser, Configs.dbpass);
                System.out.println("Connected");
            }
            catch (ClassNotFoundException e) {
                System.out.println("Error");
            }
            catch (SQLException e) {
                System.out.println("Error");
            }

        }
        return dbconnection;
    }

}
