package com.debijenkorf.assignment.service;

import com.debijenkorf.assignment.enums.LoggingEnum;
import com.debijenkorf.assignment.repository.LogDBRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DbLogger {
    private LogDBRepository logRepository;

    public void info(String message) {
        log(LoggingEnum.INFO, message);
    }

    public void warn(String message) {
        log(LoggingEnum.WARN, message);
    }

    public void error(String message) {
        log(LoggingEnum.ERROR, message);
    }

    public void debug(String message) {
        log(LoggingEnum.DEBUG, message);
    }

    private void log(LoggingEnum level, String message) {
        logRepository.insert(level.toString(), message);
    }

    @Autowired
    public void setLogRepository(LogDBRepository logRepository) {
        this.logRepository = logRepository;
    }
}
