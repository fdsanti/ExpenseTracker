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
                dbconnection = DriverManager.getConnection(urlConexionMySQL, Configs.dbuser, Configs.dbpass);
            }
            catch (ClassNotFoundException e) {
                Toast.makeText(context,
                        "Error: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
            catch (SQLException e) {
                Toast.makeText(context,
                        "Error: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }

        }
        return dbconnection;
    }

}
