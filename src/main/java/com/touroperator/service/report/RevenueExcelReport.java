package com.touroperator.service.report;

import com.touroperator.domain.Booking;
import com.touroperator.repository.BookingRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class RevenueExcelReport extends AbstractReportGenerator {

    private final BookingRepository bookingRepo;

    @Autowired
    public RevenueExcelReport(BookingRepository bookingRepo) {
        this.bookingRepo = bookingRepo;
    }

    @Override
    protected String getReportName() { return "Виручка за період"; }

    @Override
    protected Object fetchData(ReportParams params) {
        return bookingRepo.findAll().stream()
              .filter(b -> !"CANCELLED".equals(b.getStatus()))
              .filter(b -> {
                  if (params.getFrom() == null || b.getBookingDate() == null) return true;
                  return !b.getBookingDate().isBefore(params.getFrom())
                        && !b.getBookingDate().isAfter(params.getTo());
              })
              .collect(Collectors.toList());
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Object buildReport(Object data) {
        List<Booking> bookings = (List<Booking>) data;
        XSSFWorkbook wb = new XSSFWorkbook();
        XSSFSheet sheet = wb.createSheet("Виручка");

        String[] cols = {"№","Клієнт ID","Тур ID","Туристів","Дата","Вартість","Статус"};
        Row header = sheet.createRow(0);
        for (int i = 0; i < cols.length; i++)
            header.createCell(i).setCellValue(cols[i]);

        int rowNum = 1;
        BigDecimal total = BigDecimal.ZERO;
        for (Booking b : bookings) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(rowNum - 1);
            row.createCell(1).setCellValue(b.getClientId() != null ?
                  b.getClientId().toString().substring(0,8)+"…" : "");
            row.createCell(2).setCellValue(b.getTourId() != null ?
                  b.getTourId().toString().substring(0,8)+"…" : "");
            row.createCell(3).setCellValue(b.getTouristCount());
            row.createCell(4).setCellValue(b.getBookingDate() != null ?
                  b.getBookingDate().toString() : "");
            row.createCell(5).setCellValue(b.getTotalPrice() != null ?
                  b.getTotalPrice().doubleValue() : 0);
            row.createCell(6).setCellValue(b.getStatus());
            if (b.getTotalPrice() != null) total = total.add(b.getTotalPrice());
        }

        Row totalRow = sheet.createRow(rowNum + 1);
        totalRow.createCell(4).setCellValue("ВСЬОГО:");
        totalRow.createCell(5).setCellValue(total.doubleValue());

        for (int i = 0; i < cols.length; i++) sheet.autoSizeColumn(i);
        return wb;
    }

    @Override
    protected void applyStyles(Object report) {
        XSSFWorkbook wb = (XSSFWorkbook) report;
        XSSFSheet sheet = wb.getSheetAt(0);
        Row header = sheet.getRow(0);
        XSSFCellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(
              new XSSFColor(new byte[]{(byte)39,(byte)80,(byte)10}, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        XSSFFont font = wb.createFont();
        font.setBold(true);
        font.setColor(new XSSFColor(new byte[]{(byte)192,(byte)221,(byte)151}, null));
        style.setFont(font);
        if (header != null)
            for (Cell cell : header) cell.setCellStyle(style);
    }

    @Override
    protected void save(Object report, String outputPath) {
        try (FileOutputStream fos = new FileOutputStream(outputPath)) {
            ((XSSFWorkbook) report).write(fos);
        } catch (Exception e) {
            log.error("Помилка збереження звіту: {}", e.getMessage());
            throw new RuntimeException("Не вдалося зберегти звіт", e);
        }
    }
}
