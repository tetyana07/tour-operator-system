package com.touroperator.service;

import com.touroperator.domain.Booking;
import com.touroperator.domain.BookingStatus;
import com.touroperator.domain.Tour;
import com.touroperator.dto.TourReport;
import com.touroperator.repository.BookingRepository;
import com.touroperator.repository.TourRepository;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Сервіс звітності.
 * Підтримує:
 *  - фільтри за діапазоном дат
 *  - генерацію PDF через вбудований HTML→text (без зовнішніх залежностей)
 *  - Excel-звіти (існуюча логіка у ReportsController залишається)
 */
@Service
public class ReportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final TourRepository    tourRepo;
    private final BookingRepository bookingRepo;

    public ReportService(TourRepository tourRepo, BookingRepository bookingRepo) {
        this.tourRepo    = tourRepo;
        this.bookingRepo = bookingRepo;
    }

     

    public List<TourReport> getTourReports() {
        return buildReports(bookingRepo.findAll());
    }

     

    public List<TourReport> getTourReports(LocalDate from, LocalDate to) {
        List<Booking> bookings = bookingRepo.findByDateRange(from, to);
        return buildReports(bookings);
    }

    public BigDecimal getTotalRevenue(LocalDate from, LocalDate to) {
        return bookingRepo.findByDateRange(from, to).stream()
              .filter(b -> BookingStatus.PAID.name().equals(b.getStatus())
                    || BookingStatus.COMPLETED.name().equals(b.getStatus()))
              .map(b -> b.getTotalPrice() != null ? b.getTotalPrice() : BigDecimal.ZERO)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Map<String, Long> getBookingStatusStats(LocalDate from, LocalDate to) {
        return bookingRepo.findByDateRange(from, to).stream()
              .collect(Collectors.groupingBy(Booking::getStatus, Collectors.counting()));
    }

     

    public BigDecimal getTotalRevenue() {
        return bookingRepo.findByStatus(BookingStatus.PAID).stream()
              .map(Booking::getTotalPrice).filter(Objects::nonNull)
              .reduce(BigDecimal.ZERO, BigDecimal::add)
              .add(bookingRepo.findByStatus(BookingStatus.COMPLETED).stream()
                    .map(Booking::getTotalPrice).filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    public List<TourReport> getTopTours(int limit) {
        return getTourReports().stream()
              .sorted(Comparator.comparingInt(TourReport::getTotalBookings).reversed())
              .limit(limit)
              .collect(Collectors.toList());
    }

    public Map<String, Long> getBookingStatusStats() {
        return bookingRepo.findAll().stream()
              .collect(Collectors.groupingBy(Booking::getStatus, Collectors.counting()));
    }

    public long getActiveBookingsCount() {
        return bookingRepo.findByStatus(BookingStatus.CONFIRMED).size()
              + bookingRepo.findByStatus(BookingStatus.CREATED).size();
    }

    public LinkedHashMap<String, Integer> getTopCountries(int limit) {
        List<Booking> allBookings = bookingRepo.findAll().stream()
              .filter(b -> !BookingStatus.CANCELLED.name().equals(b.getStatus()))
              .collect(Collectors.toList());
        if (allBookings.isEmpty()) return new LinkedHashMap<>();

        Map<String, Long> countByCountry = new LinkedHashMap<>();
        for (Booking booking : allBookings) {
            try {
                Tour tour = tourRepo.findById(booking.getTourId()).orElse(null);
                if (tour != null && tour.getCountry() != null && !tour.getCountry().isBlank())
                    countByCountry.merge(tour.getCountry(), 1L, Long::sum);
            } catch (Exception ignored) {}
        }

        long total = countByCountry.values().stream().mapToLong(Long::longValue).sum();
        if (total == 0) return new LinkedHashMap<>();

        List<Map.Entry<String, Long>> sorted = countByCountry.entrySet().stream()
              .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
              .collect(Collectors.toList());

        LinkedHashMap<String, Integer> result = new LinkedHashMap<>();
        long othersCount = 0;
        int i = 0;
        for (Map.Entry<String, Long> e : sorted) {
            if (i < limit) {
                result.put(e.getKey(), (int) Math.round(e.getValue() * 100.0 / total));
                i++;
            } else othersCount += e.getValue();
        }
        if (othersCount > 0)
            result.put("Інші", (int) Math.round(othersCount * 100.0 / total));
        return result;
    }

    public List<Tour> getCriticallyFilledTours() {
        return tourRepo.findActive().stream()
              .filter(t -> t.getFillRate() > 0.80)
              .sorted(Comparator.comparingDouble(Tour::getFillRate).reversed())
              .collect(Collectors.toList());
    }

     

    /**
     * Генерує PDF-звіт як байти.
     * Використовує чистий HTML + CSS → конвертація через Flying Saucer або Apache FOP.
     * Якщо залежності недоступні — повертає HTML як fallback (можна зберегти як .html).
     *
     * Для повноцінного PDF додайте до pom.xml:
     * <dependency>
     *   <groupId>org.xhtmlrenderer</groupId>
     *   <artifactId>flying-saucer-pdf</artifactId>
     *   <version>9.1.22</version>
     * </dependency>
     */
    public byte[] generatePdfReport(LocalDate from, LocalDate to) {
        List<TourReport> reports = getTourReports(from, to);
        BigDecimal totalRevenue = getTotalRevenue(from, to);
        String html = buildReportHtml(reports, totalRevenue, from, to);

        try {
             
            Class<?> rendererClass = Class.forName("org.xhtmlrenderer.pdf.ITextRenderer");
            Object renderer = rendererClass.getConstructor().newInstance();
            rendererClass.getMethod("setDocumentFromString", String.class)
                  .invoke(renderer, html);
            rendererClass.getMethod("layout").invoke(renderer);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            rendererClass.getMethod("createPDF", java.io.OutputStream.class)
                  .invoke(renderer, out);
            return out.toByteArray();
        } catch (ClassNotFoundException e) {
             
            return html.getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Помилка генерації PDF: " + e.getMessage(), e);
        }
    }

    /** Генерація HTML для звіту (також використовується як fallback) */
    public String generateHtmlReport(LocalDate from, LocalDate to) {
        List<TourReport> reports = getTourReports(from, to);
        BigDecimal totalRevenue = getTotalRevenue(from, to);
        return buildReportHtml(reports, totalRevenue, from, to);
    }

     

    private List<TourReport> buildReports(List<Booking> bookings) {
        List<Tour> tours = tourRepo.findAll();
        List<TourReport> reports = new ArrayList<>();

         
        Map<UUID, List<Booking>> byTour = bookings.stream()
              .collect(Collectors.groupingBy(Booking::getTourId));

        for (Tour tour : tours) {
            List<Booking> tourBookings = byTour.getOrDefault(tour.getId(), List.of());

            int totalBookings = tourBookings.size();
            int totalTourists = tourBookings.stream().mapToInt(Booking::getTouristCount).sum();

            BigDecimal revenue = tourBookings.stream()
                  .filter(b -> BookingStatus.PAID.name().equals(b.getStatus())
                        || BookingStatus.COMPLETED.name().equals(b.getStatus()))
                  .map(Booking::getTotalPrice)
                  .filter(Objects::nonNull)
                  .reduce(BigDecimal.ZERO, BigDecimal::add);

            reports.add(new TourReport(
                  tour.getName(), totalBookings, totalTourists, revenue, tour.getFillRate() * 100));
        }

        reports.sort(Comparator.comparing(TourReport::getTotalRevenue).reversed());
        return reports;
    }

    private String buildReportHtml(List<TourReport> reports, BigDecimal totalRevenue,
          LocalDate from, LocalDate to) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                <!DOCTYPE html>
                <html lang="uk">
                <head>
                  <meta charset="UTF-8"/>
                  <style>
                    body { font-family: Arial, sans-serif; margin: 32px; color: #222; }
                    h1   { color: #1a4a2a; }
                    .meta { color: #666; margin-bottom: 24px; }
                    table { width: 100%; border-collapse: collapse; }
                    th { background: #1a4a2a; color: white; padding: 10px; text-align: left; }
                    td { padding: 8px 10px; border-bottom: 1px solid #ddd; }
                    tr:nth-child(even) td { background: #f5f9f5; }
                    .total { font-weight: bold; background: #e8f5e8; }
                    .footer { margin-top: 32px; color: #888; font-size: 12px; }
                  </style>
                </head>
                <body>
                """);

        sb.append("<h1>Звіт туроператора AAYVO</h1>\n");
        sb.append(String.format("<p class='meta'>Період: %s — %s | Згенеровано: %s</p>\n",
              from.format(DATE_FMT), to.format(DATE_FMT), LocalDate.now().format(DATE_FMT)));

        sb.append("""
                <table>
                  <tr>
                    <th>Тур</th>
                    <th>Бронювань</th>
                    <th>Туристів</th>
                    <th>Виручка (USD)</th>
                    <th>Заповненість</th>
                  </tr>
                """);

        for (TourReport r : reports) {
            sb.append(String.format("""
                    <tr>
                      <td>%s</td>
                      <td>%d</td>
                      <td>%d</td>
                      <td>%,.2f</td>
                      <td>%.1f%%</td>
                    </tr>
                    """,
                  r.getTourName(), r.getTotalBookings(), r.getTotalTourists(),
                  r.getTotalRevenue(), r.getFillPercent()));
        }

        sb.append(String.format("""
                <tr class='total'>
                  <td>РАЗОМ</td><td></td><td></td>
                  <td>%,.2f</td><td></td>
                </tr>
                """, totalRevenue));

        sb.append("</table>\n");
        sb.append("<p class='footer'>VoyaGo Tour Operator System</p>\n");
        sb.append("</body></html>");
        return sb.toString();
    }
}
