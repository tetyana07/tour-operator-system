package com.touroperator.service.report;

import java.time.LocalDate;


public class ReportParams {
    private LocalDate from;
    private LocalDate to;

    public ReportParams(LocalDate from, LocalDate to) {
        this.from = from;
        this.to = to;
    }

    public LocalDate getFrom() { return from; }
    public LocalDate getTo() { return to; }
}
