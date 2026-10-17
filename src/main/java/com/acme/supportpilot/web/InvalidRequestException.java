package com.acme.supportpilot.web;

/** A problem with the caller's input - mapped to HTTP 400 by ApiExceptionHandler. */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }

}
