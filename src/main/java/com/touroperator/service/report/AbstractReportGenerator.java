package com.touroperator.service.report;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class AbstractReportGenerator {

    protected final Logger log = LoggerFactory.getLogger(getClass());


    public final void generate(ReportParams params, String outputPath) {
        log.info("Генерація звіту '{}' → {}", getReportName(), outputPath);
        Object data = fetchData(params);
        Object report = buildReport(data);
        applyStyles(report);
        save(report, outputPath);
        log.info("Звіт '{}' збережено: {}", getReportName(), outputPath);
    }


    protected abstract String getReportName();


    protected abstract Object fetchData(ReportParams params);


    protected abstract Object buildReport(Object data);


    protected abstract void applyStyles(Object report);


    protected abstract void save(Object report, String outputPath);
}
