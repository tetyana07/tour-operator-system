package com.touroperator.service.report;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;
import com.touroperator.domain.Booking;
import com.touroperator.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class RevenuePdfReport extends AbstractReportGenerator {


    private static final Color COLOR_HEADER_BG  = new Color(39,  80, 10);
    private static final Color COLOR_HEADER_FG  = new Color(192, 221, 151);
    private static final Color COLOR_ROW_ALT    = new Color(244, 250, 234);
    private static final Color COLOR_TOTAL_BG   = new Color(99, 153, 34);
    private static final Color COLOR_BORDER     = new Color(192, 221, 151);
    private static final Color COLOR_TEXT_DARK  = new Color(13,  32,  16);
    private static final Color COLOR_TEXT_MUTED = new Color(138, 154, 133);
    private static final String STATUS_CANCELLED = "CANCELLED";

    private static final DateTimeFormatter DATE_FMT =
          DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final BookingRepository bookingRepo;

    @Autowired
    public RevenuePdfReport(BookingRepository bookingRepo) {
        this.bookingRepo = bookingRepo;
    }



    @Override
    protected String getReportName() { return "Виручка за період (PDF)"; }

    @Override
    protected Object fetchData(ReportParams params) {
        return bookingRepo.findAll().stream()
              .filter(b -> !STATUS_CANCELLED.equals(b.getStatus()))
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

        Document doc = new Document(PageSize.A4, 36, 36, 50, 40);
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();

        try {
            PdfWriter writer = PdfWriter.getInstance(doc, bos);
            doc.open();

            addHeader(doc, writer);
            addSummaryLine(doc, bookings);
            addTable(doc, bookings);

            doc.close();
        } catch (DocumentException | java.io.IOException e) {
            throw new RuntimeException("Помилка генерації PDF: " + e.getMessage(), e);
        }


        return bos;
    }

    @Override
    protected void applyStyles(Object report) {

    }

    @Override
    protected void save(Object report, String outputPath) {
        java.io.ByteArrayOutputStream bos = (java.io.ByteArrayOutputStream) report;
        try (FileOutputStream fos = new FileOutputStream(outputPath)) {
            bos.writeTo(fos);
        } catch (Exception e) {
            log.error("Помилка збереження PDF-звіту: {}", e.getMessage());
            throw new RuntimeException("Не вдалося зберегти PDF-звіт", e);
        }
    }



    private void addHeader(Document doc, PdfWriter writer) throws DocumentException, java.io.IOException {
        float pageWidth  = doc.getPageSize().getWidth();
        float pageHeight = doc.getPageSize().getHeight();
        float left       = doc.leftMargin();
        float boxTop     = pageHeight - doc.topMargin() + 14;
        float boxH       = 58;


        PdfContentByte cb = writer.getDirectContentUnder();
        cb.setColorFill(COLOR_HEADER_BG);
        cb.rectangle(left, boxTop - boxH, pageWidth - left - doc.rightMargin(), boxH);
        cb.fill();


        PdfContentByte over = writer.getDirectContent();
        BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, false);
        BaseFont bfNormal = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, false);

        over.beginText();
        over.setColorFill(COLOR_HEADER_FG);
        over.setFontAndSize(bf, 16);
        over.setTextMatrix(left + 10, boxTop - boxH + 22);
        over.showText("AYVO — Звіт про виручку");
        over.endText();

        over.beginText();
        over.setColorFill(COLOR_HEADER_FG);
        over.setFontAndSize(bfNormal, 8);
        over.setTextMatrix(left + 10, boxTop - boxH + 8);
        String generated = "Сформовано: " +
              java.time.LocalDate.now().format(DATE_FMT) +
              "  |  AYVO - Система туроператора";
        over.showText(generated);
        over.endText();


        doc.add(new Paragraph(" "));
        doc.add(new Paragraph(" "));
        doc.add(new Paragraph(" "));
    }

    private void addSummaryLine(Document doc, List<Booking> bookings) throws DocumentException {
        BigDecimal total = bookings.stream()
              .map(b -> b.getTotalPrice() != null ? b.getTotalPrice() : BigDecimal.ZERO)
              .reduce(BigDecimal.ZERO, BigDecimal::add);

        Font boldGreen = new Font(Font.HELVETICA, 11, Font.BOLD, COLOR_HEADER_BG);
        Font normal    = new Font(Font.HELVETICA, 10, Font.NORMAL, COLOR_TEXT_DARK);

        Paragraph p = new Paragraph();
        p.add(new Chunk("Всього бронювань: ", normal));
        p.add(new Chunk(String.valueOf(bookings.size()), boldGreen));
        p.add(new Chunk("     Загальна виручка: ", normal));
        p.add(new Chunk(String.format("₴ %,.2f", total), boldGreen));
        p.setSpacingAfter(12);
        doc.add(p);
    }

    private void addTable(Document doc, List<Booking> bookings) throws DocumentException {

        PdfPTable table = new PdfPTable(new float[]{0.5f, 2.5f, 1.5f, 1f, 2f, 1.5f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(4);

        String[] headers = {"№", "ID бронювання", "Дата", "Туристів", "Вартість (₴)", "Статус"};
        Font hFont = new Font(Font.HELVETICA, 9, Font.BOLD, COLOR_HEADER_FG);
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, hFont));
            cell.setBackgroundColor(COLOR_HEADER_BG);
            cell.setPadding(6);
            cell.setBorderColor(COLOR_BORDER);
            cell.setHorizontalAlignment(Element.ALIGN_LEFT);
            table.addCell(cell);
        }

        Font cellFont  = new Font(Font.HELVETICA, 8, Font.NORMAL, COLOR_TEXT_DARK);
        Font mutedFont = new Font(Font.HELVETICA, 8, Font.NORMAL, COLOR_TEXT_MUTED);
        BigDecimal total = BigDecimal.ZERO;

        for (int i = 0; i < bookings.size(); i++) {
            Booking b = bookings.get(i);
            Color rowBg = (i % 2 == 1) ? COLOR_ROW_ALT : Color.WHITE;

            addCell(table, String.valueOf(i + 1), mutedFont, rowBg);
            addCell(table, b.getId() != null ?
                  b.getId().toString().substring(0, 13) + "…" : "—", mutedFont, rowBg);
            addCell(table, b.getBookingDate() != null ?
                  b.getBookingDate().format(DATE_FMT) : "—", cellFont, rowBg);
            addCell(table, String.valueOf(b.getTouristCount()), cellFont, rowBg);
            addCell(table, b.getTotalPrice() != null ?
                  String.format("%,.2f", b.getTotalPrice()) : "—", cellFont, rowBg);
            addCell(table, localizeStatus(b.getStatus()), statusFont(b.getStatus()), rowBg);

            if (b.getTotalPrice() != null) total = total.add(b.getTotalPrice());
        }


        Font totalLabelFont = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
        Font totalValFont   = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
        PdfPCell totalLabel = new PdfPCell(new Phrase("РАЗОМ:", totalLabelFont));
        totalLabel.setColspan(4);
        totalLabel.setBackgroundColor(COLOR_TOTAL_BG);
        totalLabel.setPadding(7);
        totalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalLabel.setBorderColor(COLOR_BORDER);
        table.addCell(totalLabel);

        PdfPCell totalVal = new PdfPCell(new Phrase(String.format("%,.2f", total), totalValFont));
        totalVal.setBackgroundColor(COLOR_TOTAL_BG);
        totalVal.setPadding(7);
        totalVal.setBorderColor(COLOR_BORDER);
        table.addCell(totalVal);

        PdfPCell totalEmpty = new PdfPCell(new Phrase(""));
        totalEmpty.setBackgroundColor(COLOR_TOTAL_BG);
        totalEmpty.setBorderColor(COLOR_BORDER);
        table.addCell(totalEmpty);

        doc.add(table);
    }



    private void addCell(PdfPTable table, String text, Font font, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setPadding(5);
        cell.setBorderColor(COLOR_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(cell);
    }

    private Font statusFont(String status) {
        Color c = switch (status) {
            case "CONFIRMED" -> new Color(39,  80, 10);
            case "PAID"      -> new Color(10, 110, 86);
            case STATUS_CANCELLED -> new Color(138, 32, 32);
            case "COMPLETED" -> new Color(26,  90, 58);
            default          -> COLOR_TEXT_MUTED;
        };
        return new Font(Font.HELVETICA, 8, Font.BOLD, c);
    }

    private String localizeStatus(String status) {
        return switch (status) {
            case "CREATED"   -> "Нове";
            case "CONFIRMED" -> "Підтверджено";
            case "PAID"      -> "Сплачено";
            case "COMPLETED" -> "Завершено";
            case STATUS_CANCELLED -> "Скасовано";
            default          -> status;
        };
    }
}