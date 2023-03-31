package com.debijenkorf.assignment.repository;

import com.debijenkorf.assignment.app.config.LogDBProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Repository
public class LogRepository {
    private LogDBProperties logDBProperties;

    public void log(String level, String message) {
        String query = "insert into [db_log] (timestamp, level, message) values (?,?,?)";

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

    @Autowired
    public void setLogDBProperties(LogDBProperties logDBProperties) {
        this.logDBProperties = logDBProperties;
    }

    public Connection getConnection() {
        String url = String.join(logDBProperties.getEndpoint(), logDBProperties.getName());

        try {
            return DriverManager.getConnection(url, logDBProperties.getUsername(), logDBProperties.getPassword());
        } catch (SQLException e) {

        }

        return null;
    }
}
