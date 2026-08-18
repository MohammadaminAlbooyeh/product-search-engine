package com.pse.exception;

public class CrawlerFailedException extends RuntimeException {

    public CrawlerFailedException(String message) {
        super(message);
    }

    public CrawlerFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
