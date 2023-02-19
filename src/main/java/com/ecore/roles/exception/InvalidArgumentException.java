package com.ecore.roles.exception;

import static java.lang.String.format;

public class InvalidArgumentException extends RuntimeException {

    public <T> InvalidArgumentException(Class<T> resource) {
        super(format("Invalid '%s' object", resource.getSimpleName()));
    }

    public <T> InvalidArgumentException(Class<T> resource, String msg) {
        super(format("Invalid '%s' object. %s", resource.getSimpleName(), msg));
    }
}
