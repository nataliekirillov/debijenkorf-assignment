package com.debijenkorf.assignment.repository;

import com.debijenkorf.assignment.app.configuration.LogDBProperties;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class LogDBRepository {
    private LogDBProperties logDBProperties;

    /**
     * Insert a log message to [db_logs]
     *
     * @param level   Log level
     * @param message Message to log
     */
    public void insert(String level, String message) {
        String query = "insert into db_logs (timestamp, level, message) values (?,?,?)";

        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setLong(1, System.currentTimeMillis());
            ps.setString(2, level);
            ps.setString(3, message);

            int row = ps.executeUpdate();
            if (row != 1) {
                log.error("Failed to insert row to db_logs");
            }
        } catch (SQLException e) {
            log.error("Failed to insert row to db_logs: {}", e.getMessage());
            log.debug("Failed to insert row to db_logs", e);
        }
    }

    /**
     * Establish Database connection
     *
     * @return Database connection
     */
    public Connection getConnection() throws SQLException {
        String url = String.join("/", logDBProperties.getEndpoint(), logDBProperties.getName());

        try {
            return DriverManager.getConnection(url, logDBProperties.getUsername(), logDBProperties.getPassword());
        } catch (SQLException e) {
            log.error("Failed to get DB connection: {}", e.getMessage());
            log.debug("Failed to get DB connection", e);
            throw e;
        }
    }

    @Autowired
    public void setLogDBProperties(LogDBProperties logDBProperties) {
        this.logDBProperties = logDBProperties;
    }
}
