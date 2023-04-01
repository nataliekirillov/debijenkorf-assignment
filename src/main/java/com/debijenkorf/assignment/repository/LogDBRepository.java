package com.debijenkorf.assignment.repository;

import com.debijenkorf.assignment.app.configuration.LogDBProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * A repository responsible for the logging table
 */
@Repository
public class LogDBRepository {
    private LogDBProperties logDBProperties;

    /**
     * Insert a log message to [db_logs]
     *
     * @param level   Log level
     * @param message Message to log
     */
    public void insert(String level, String message) {
        String query = "insert into [db_logs] (timestamp, level, message) values (?,?,?)";

        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setLong(1, System.currentTimeMillis());
            ps.setString(2, level);
            ps.setString(3, message);

            int row = ps.executeUpdate();
            if (row != 1) {
                // failed to log
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Establish Database connection
     *
     * @return Database connection
     */
    public Connection getConnection() {
        String url = String.join(logDBProperties.getEndpoint(), logDBProperties.getName());

        try {
            return DriverManager.getConnection(url, logDBProperties.getUsername(), logDBProperties.getPassword());
        } catch (SQLException e) {

        }

        return null;
    }

    @Autowired
    public void setLogDBProperties(LogDBProperties logDBProperties) {
        this.logDBProperties = logDBProperties;
    }
}
