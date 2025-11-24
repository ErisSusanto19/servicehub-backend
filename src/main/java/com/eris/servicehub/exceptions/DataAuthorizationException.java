package com.eris.servicehub.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public class DataAuthorizationException extends RuntimeException {
    public DataAuthorizationException(String message) {
        super(message);
    }
}