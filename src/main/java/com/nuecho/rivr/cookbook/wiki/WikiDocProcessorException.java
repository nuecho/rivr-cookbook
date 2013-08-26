/*
 * Copyright (c) 2013 Nu Echo Inc. All rights reserved.
 */

package com.nuecho.rivr.cookbook.wiki;

/**
 * @author Nu Echo Inc.
 */
public class WikiDocProcessorException extends Exception {

    private static final long serialVersionUID = 1L;

    public WikiDocProcessorException() {}

    public WikiDocProcessorException(String message) {
        super(message);
    }

    public WikiDocProcessorException(Throwable cause) {
        super(cause);
    }

    public WikiDocProcessorException(String message, Throwable cause) {
        super(message, cause);
    }

}
