package com.debijenkorf.assignment.service;

import com.debijenkorf.assignment.enums.LogEnum;
import com.debijenkorf.assignment.repository.LogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LogService {
    private LogRepository logRepository;

    public void info(String message) {
        log(LogEnum.INFO, message);
    }

    public void warn(String message) {
        log(LogEnum.WARN, message);
    }

    public void error(String message) {
        log(LogEnum.ERROR, message);
    }

    public void debug(String message) {
        log(LogEnum.DEBUG, message);
    }

    private void log(LogEnum level, String message) {
        logRepository.insert(level.toString(), message);
    }

    @Autowired
    public void setLogRepository(LogRepository logRepository) {
        this.logRepository = logRepository;
    }
}
