package com.example.expensetracker;

import org.apache.commons.dbcp2.BasicDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class DBCPDataSource {

    /*private static BasicDataSource ds = new BasicDataSource();

    static {
        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
        ds.setUrl("jdbc:mysql://" + Configs.dbhost + ":" + Configs.dbport + "/" + Configs.dbname);
        ds.setUsername(Configs.dbuser);
        ds.setPassword(Configs.dbpass);
        ds.setMinIdle(5);
        ds.setMaxIdle(10);
        ds.setMaxOpenPreparedStatements(100);
    }

    public static Connection getConnection() throws SQLException {
        return ds.getConnection();
    }*/

    private DBCPDataSource(){ }
}