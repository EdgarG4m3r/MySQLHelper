package com.github.edgarg4m3r.mysqlhelper;

import lombok.AllArgsConstructor;
import lombok.Getter;

@SuppressWarnings({"unused"})
@Getter
@AllArgsConstructor
public class SQLBuilder {

    private String host, port, database, username, password;

    /**
     * sets the host
     */
    public SQLBuilder setHost(String host) {
        this.host = host;
        return this;
    }

    /**
     * sets the port
     */
    public SQLBuilder setPort(String port) {
        this.port = port;
        return this;
    }

    /**
     * sets the database
     */
    public SQLBuilder setDatabase(String database) {
        this.database = database;
        return this;
    }

    /**
     * sets the username
     */
    public SQLBuilder setUsername(String username) {
        this.username = username;
        return this;
    }

    /**
     * sets the password
     */
    public SQLBuilder setPassword(String password) {
        this.password = password;
        return this;
    }

    /**
     * builds the SQL instance
     */
    public SQLHelper toSQL() {
        return new SQLHelper(host, port, database, username, password);
    }

}
