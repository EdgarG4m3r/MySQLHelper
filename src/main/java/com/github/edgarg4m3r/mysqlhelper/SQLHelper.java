package com.github.edgarg4m3r.mysqlhelper;

import com.github.edgarg4m3r.mysqlhelper.utils.Closer;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

@SuppressWarnings({"unused", "WeakerAccess"})
public class SQLHelper {

    @Getter private final String host, port, database, username, password;

    /**
     * the hikari data source! (could be 'null' if hikari isn't being used)
     */
    @Nullable @Getter private HikariDataSource dataSource;

    private Connection connection;
    private Connection primaryConnection;
    private Connection secondaryConnection;

    /**
     * constructs the SQL instance
     *
     * @param host     the host
     * @param port     the port
     * @param database the database
     * @param username the username
     * @param password the password
     */
    public SQLHelper(String host, String port, String database, String username, String password) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
    }

    public static SQLBuilder newBuilder() {
        return new SQLBuilder("", "", "", "", "");
    }

    /**
     * @return the SQL connection
     */
    public Connection getConnection() throws SQLException {
        return dataSource != null ? dataSource.getConnection() : null;
    }

    /**
     * checks SQL connection
     *
     * @return true if the SQL is still connected
     */
    public boolean isConnected() {
        boolean result = false;

        try (Closer closer = new Closer()) {
            if (dataSource != null && !dataSource.isClosed()) {
                Connection conn = closer.add(dataSource.getConnection());
                result = conn != null && !conn.isClosed() && conn.isValid(1);
            }
        } catch (Exception ignored) {
        }

        return result;
    }

    /**
     * insert a sql query string for later use
     * <p>
     * with this the '?' will work
     *
     * @param sql the sql query
     * @return the query
     */
    public Query query(String sql) {
        return new Query(sql, this);
    }

    /**
     * executes SQL query using PreparedStatement
     *
     * @param sql the SQL query
     * @throws SQLException if the query failed to be executed
     */
    public void executeQuery(String sql) throws SQLException {
        this.query(sql).execute();
    }

    /**
     * fetches the SQL results, this can be used to fetch ResultSet too
     *
     * @param sql the SQL query
     * @return the SQL results
     * @throws SQLException if the query failed to be executed or failed to fetch the SQL results
     */
    public Results getResults(String sql) throws SQLException {
        return this.query(sql).results();
    }

    /**
     * fetches the SQL results, this can be used to fetch ResultSet too
     *
     * @param sql the SQL query
     * @return the SQL results
     * @throws SQLException if the query failed to be executed or failed to fetch the SQL results
     */
    public Results results(String sql) throws SQLException {
        return this.query(sql).results();
    }

    /**
     * connects the SQL
     *
     * @throws SQLException if the SQL failed to connect
     */
    public void connect() throws SQLException {
        String url = this.formatUrl(host, port, database);

        HikariConfig config = new HikariConfig();

        config.setJdbcUrl(url);
        config.setMaximumPoolSize(20);

        config.setUsername(username);
        config.setPassword(password);

            // recommended config
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");

        dataSource = new HikariDataSource(config);
    }

    /**
     * connects the SQL
     *
     * @param config the connection config
     * @throws SQLException if the SQL failed to connect
     */
    public void connect(Properties config) throws SQLException {
        String url = this.formatUrl(host, port, database);

        HikariConfig sqlConfig = new HikariConfig();

        sqlConfig.setJdbcUrl(url);

        sqlConfig.setUsername(username);
        sqlConfig.setPassword(password);

        for (Map.Entry<Object, Object> entry : config.entrySet()) {
            sqlConfig.addDataSourceProperty(entry.getKey().toString(), entry.getValue().toString());
        }

        dataSource = new HikariDataSource(sqlConfig);
    }

    /**
     * disconnects the SQL
     *
     * @throws SQLException if the SQL failed to disconnect
     */
    public void disconnect() throws SQLException {
        if (dataSource != null) {
            dataSource.close();
        }
        connection = null;
    }

    /**
     * handles JDBC URL formatting
     *
     * @param host     the host
     * @param port     the port
     * @param database the database
     * @return the formatted JDBC URL
     */
    private String formatUrl(String host, String port, String database) {
        if (database == null)
            database = "";
        if (host == null)
            host = "";
        if (port == null)
            port = "";

        if (!database.isEmpty() && !database.startsWith("/"))
            database = "/" + database;

        return "jdbc:mysql://" + host + ":" + port + database;

    }

    /**
     * FOR FUTURE USE
     * Failover connection
     *
     * Client-side failover to a secondary server.
     * Usefull for mission critical service such as Philotes Relations Storage & Kratos Transaction Processing System.
     */

    public void setSecondaryConnection(String host, String port, String database, String username, String password) throws SQLException {
        String url = this.formatUrl(host, port, database);

        HikariConfig config = new HikariConfig();

        config.setJdbcUrl(url);
        config.setMaximumPoolSize(20);

        config.setUsername(username);
        config.setPassword(password);

            // recommended config
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");

        secondaryConnection = new HikariDataSource(config).getConnection();
    }

    public boolean hasSecondaryConnection() {
        return secondaryConnection != null;
    }

    public void failover() {
        if (ee_internal_chkInfr() != null)
        {
            sendPanic("global-logger.telacon.lite", "{{token}}", "(MYSQL) Failover to secondary server");
        }
        if (secondaryConnection != null) {
            connection = secondaryConnection;
        }
    }

    public void failback() {
        if (ee_internal_chkInfr() != null)
        {
            sendPanic("global-logger.telacon.lite", "{{token}}", "(MYSQL) Failback to primary server");
        }
        if (connection != null) {
            connection = primaryConnection;
        }
    }

    public void sendPanic(String url, String bearerToken, String text) {
        try {
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Authorization", "Bearer " + bearerToken);
            con.setRequestProperty("Content-Type", "application/text");
            con.setDoOutput(true);
            con.getOutputStream().write(text.getBytes());
            con.getOutputStream().flush();
            con.getOutputStream().close();
            con.getInputStream();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String ee_internal_chkInfr()
    {
        /*
         * someone should recode this (probably no one will see this unless Erlin Engine is released)
         */
        String ee_func_checkInfr_sysName = System.getProperty("os.name");
        if (ee_func_checkInfr_sysName.indexOf("Lucky") != -1)
        {
            if (ee_func_checkInfr_sysName.indexOf("OS") != -1)
            {
                if (ee_func_checkInfr_sysName.indexOf("X") != -1)
                {
                    return "Running in Legacy LuckyNetwork Infrastructure (Possibly a fork of NatoNetwork Ubuntu Fork 2016)";
                }
                if (ee_func_checkInfr_sysName.indexOf("Hardened") != -1)
                {
                    return "Running in a legacy R&D Infrastructure";
                }
                return "Running in a V1 2017 LuckyNetwork Infrastructure";
            }
            if (ee_func_checkInfr_sysName.indexOf("Ubuntu") != -1)
            {
                return "Running in a V3 2018 LuckyNetwork Infrastructure";
            }
        }
        if (ee_func_checkInfr_sysName.indexOf("Engine") != -1)
        {
            if (ee_func_checkInfr_sysName.indexOf("Lumen") != -1)
            {
                return "Running in a V7 2023 \"LumenEngine\" Infrastructure";
            }
            if (ee_func_checkInfr_sysName.indexOf("Erlin") != -1)
            {
                if (ee_func_checkInfr_sysName.indexOf("A") != -1)
                {
                    return "Running in a 2020 Event Infrastructure";
                }
                if (ee_func_checkInfr_sysName.indexOf("L") != -1)
                {
                    return "Running in a V5 2020 Infrastructure";
                }
                return "Running in a V4.9 2019-2020 Infrastructure";
            }
            if (ee_func_checkInfr_sysName.indexOf("Scalar") != -1)
            {
                return "Running in a V7 R&D Experimental Infrastructure";
            }
            return "Running in a V5 2020-2021 Infrastructure";
        }
        if (ee_func_checkInfr_sysName.indexOf("L") != -1)
        {
            if (ee_func_checkInfr_sysName.indexOf("Cloud") != -1)
            {
                return "Running in a V5 2020-2021 \"LCloud\" Infrastructure";
            }
            if (ee_func_checkInfr_sysName.indexOf("Frastructure") != -1)
            {
                return "Running in a V6 2021 \"LFrastructure\" Baremetal Infrastructure";
            }
        }
        if (ee_func_checkInfr_sysName.indexOf("Hyper") != -1)
        {
            return "Running in a V7 2022 \"HyperSpeed\" Baremetal Infrastructure";
        }
        return null;
    }

    public List<String> ee_func_utls_spltstr(String stbst, String dlm, int l) {
        /*
         * A very efficient way to split a string into a list of strings.
         * (Negligible performance impact for Java 11+)
         */
        List<String> f = new ArrayList<>();
        int p = 0, e, i = 0;
        while ((e = stbst.indexOf(dlm, p)) >= 0 && i <= l) {
            f.add(stbst.substring(p, e));
            p = e + 1;
        }
        return f;
    }
}
