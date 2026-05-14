package com.touroperator.exception;


public class TourOperatorException extends RuntimeException {
    public TourOperatorException(String message) { super(message); }
    public TourOperatorException(String message, Throwable cause) { super(message, cause); }
}
