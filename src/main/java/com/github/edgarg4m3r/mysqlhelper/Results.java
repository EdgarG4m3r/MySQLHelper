package com.github.edgarg4m3r.mysqlhelper;

import lombok.Getter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@Getter
public class Results implements AutoCloseable {

    private final Connection connection;
    private final PreparedStatement statement;
    private final ResultSet resultSet;

    /**
     * constructs the SQL results
     *
     * @param connection the connection
     * @param statement  the (prepared) statement
     * @param resultSet  the result set
     */
    public Results(Connection connection, PreparedStatement statement, ResultSet resultSet) {
        this.connection = connection;
        this.statement = statement;
        this.resultSet = resultSet;
    }

    /**
     * handles the SQL results resource closing
     */
    @Override
    public void close() {
        try {
            resultSet.close();
            statement.close();
            connection.close();
        } catch (Exception ignored) {
        }

    }

}
