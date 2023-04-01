package com.debijenkorf.assignment.service;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

import java.io.IOException;
import java.io.InputStream;

public interface StorageService {
    InputStream download(String path);

    @Retryable(retryFor = IOException.class, maxAttempts = 1, backoff = @Backoff(delay = 200))
    void upload(String path, InputStream is) throws IOException;

    void delete(String path);
}
