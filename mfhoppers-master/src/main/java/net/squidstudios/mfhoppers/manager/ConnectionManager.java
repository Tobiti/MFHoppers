package net.squidstudios.mfhoppers.manager;


import net.squidstudios.mfhoppers.MFHoppers;
import net.squidstudios.mfhoppers.util.plugin.PluginBuilder;
import org.apache.commons.io.FileUtils;
import org.bukkit.Location;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ConnectionManager {
    private DataManager manager;
    private List<Connection> connections = new ArrayList<>();
    public String url;
    
    public ConnectionManager(DataManager manager){
        this.manager = manager;
        this.url = "jdbc:sqlite:" + MFHoppers.getInstance().getDataFolder().getAbsolutePath() + File.separator + "data.db";
    }

    public Connection getConnection() {
        Connection c = null;

        try {
            Class.forName("org.sqlite.JDBC");
            this.url = "jdbc:sqlite:" + MFHoppers.getInstance().getDataFolder().getAbsolutePath() + File.separator + "data.db";
            c = DriverManager.getConnection(url);
        } catch ( Exception e ) {
            MFHoppers.getInstance().out(e);
        }
        return c;
    }
    public Boolean run(String sql) {
        try {
                Connection conn = getConnection();
                if (conn == null) {
                    return false;
                }
                Statement stmt = conn.createStatement();
                stmt.executeUpdate(sql);
                stmt.close();
                conn.close();
                return true;
        } catch (SQLException e) {
            MFHoppers.getInstance().out("&8(&cSQL&8)&c " + e.getMessage(), PluginBuilder.OutType.ERROR);
            MFHoppers.getInstance().out(" &c&l-> &7Tried to execute: &c" + sql);
        }
        return false;
    }
    public void destroy(String dbName){

        File file = new File(MFHoppers.getInstance().getDataFolder(),dbName);
        for(Connection conn : connections){

            try{
            if(conn != null && !conn.isClosed()) {

                conn.close();

            }
            } catch (Exception ex){
                ex.printStackTrace();
            }

        }
        if(file.exists()){

            try {

                FileUtils.copyFile(file, new File(MFHoppers.getInstance().getDataFolder(), "backup-data.sql"));
                file.delete();

            } catch (Exception ex){

                MFHoppers.getInstance().out(ex);

            }
        }

    }
    public boolean hasColumn(String table, String name){

        Connection conn = getConnection();
        try (PreparedStatement selectStmt = conn.prepareStatement(
                "SELECT * FROM " + table, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
             ResultSet rs = selectStmt.executeQuery()) {
            ResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();
            boolean bool = false;
            for (int i = 1; i <= columnCount; i++ ) {
                if(rsmd.getColumnName(i).equalsIgnoreCase(name)){
                    bool = true;
                }
            }
            if(conn != null && conn.isClosed()){
                selectStmt.close();
                conn.close();
            }
            return bool;


        } catch (Exception ex){
            ex.printStackTrace();
        }
        try{
            if(conn != null && conn.isClosed()){
                conn.close();
            }
        } catch (Exception ex){
            ex.printStackTrace();
        }
        return false;

    }
    public boolean hasColumn(String table, String name, String dbName){

        this.url = "jdbc:sqlite:" + MFHoppers.getInstance().getDataFolder().getAbsolutePath() + File.separator + dbName;
        Connection conn = getConnection();
        try (PreparedStatement selectStmt = conn.prepareStatement(
                "SELECT * FROM " + table, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
             ResultSet rs = selectStmt.executeQuery()) {
            ResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();
            boolean bool = false;
            for (int i = 1; i <= columnCount; i++ ) {
                if(rsmd.getColumnName(i).equalsIgnoreCase(name)){
                    bool = true;
                }
            }
            if(conn != null && conn.isClosed()){
                selectStmt.close();
                conn.close();
            }
            conn.close();
            this.url = "jdbc:sqlite:" + MFHoppers.getInstance().getDataFolder().getAbsolutePath() + File.separator + "data.db";
            return bool;


        } catch (Exception ex){
            ex.printStackTrace();
        }
        try{
            if(conn != null && conn.isClosed()){
                conn.close();
            }
        } catch (Exception ex){
            ex.printStackTrace();
        }
        this.url = "jdbc:sqlite:" + MFHoppers.getInstance().getDataFolder().getAbsolutePath() + File.separator + "data.db";
        return false;

    }
    public void closeAll(){
        for(Connection conn : connections){

            try{
                if(conn != null && !conn.isClosed()) {

                    conn.close();

                }
            } catch (Exception ex){
                ex.printStackTrace();
            }

        }
    }
    public HashMap<Integer, HashMap<String, Object>> getAllRows(String tableName, String dbName) {
        HashMap<Integer, HashMap<String, Object>> ret = new HashMap<>();

        this.url = "jdbc:sqlite:" + MFHoppers.getInstance().getDataFolder().getAbsolutePath() + File.separator + dbName;
        Connection conn = getConnection();

        try {
            try (PreparedStatement selectStmt = conn.prepareStatement(
                    "SELECT * from " + tableName, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
                 ResultSet rs = selectStmt.executeQuery()) {
                if (!rs.isBeforeFirst()) {
                } else {
                    int id = 0;
                    while (rs.next()) {
                        for (int i = 1; i < rs.getMetaData().getColumnCount() + 1; i++) {
                            if (rs.getMetaData().getColumnName(i).equalsIgnoreCase("id")) {

                                id = (int) rs.getObject(i);
                                HashMap<String, Object> data = new HashMap<>();
                                ret.put(id, data);


                            } else {

                                ret.get(id).put(rs.getMetaData().getColumnName(i), rs.getObject(i));

                            }
                        }
                    }
                    rs.close();
                    selectStmt.close();
                    conn.close();
                }
            }
            conn.close();
            this.url = "jdbc:sqlite:" + MFHoppers.getInstance().getDataFolder().getAbsolutePath() + File.separator + "data.db";
        } catch (SQLException e) {
            return ret;
        }

        return ret;
    }
    public static String toString(Location loc){
        return loc.getWorld().getName() + ";" + loc.getBlock().getX() + ";" + loc.getBlock().getY() + ";" + loc.getBlock().getZ();
    }

}
