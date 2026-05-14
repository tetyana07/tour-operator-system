package com.touroperator.exception;


public class QuotaExceededException extends TourOperatorException {
    private final int requested;
    private final int available;

    public QuotaExceededException(int requested, int available) {
        super(String.format("Недостатньо місць. Запитано: %d, доступно: %d", requested, available));
        this.requested = requested;
        this.available = available;
    }

    public int getRequested() { return requested; }
    public int getAvailable() { return available; }
}
