package com.touroperator.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;


@Service
public class NotificationService {

    public enum EventType {
        BOOKING_CREATED,
        BOOKING_CONFIRMED,
        BOOKING_PAID,
        BOOKING_CANCELLED,
        BOOKING_COMPLETED,
        EXCEL_EXPORTED,
        PDF_EXPORTED,
        CLIENT_BOOKING_CANCELLED,
        CLIENT_BOOKING_CONFIRMED,
        CLIENT_BOOKING_PAID,
        CLIENT_TOUR_REMINDER
    }

    public record AppNotification(
          EventType type,
          String message,
          String icon,
          LocalDateTime at
    ) {
        public String timeAgo() {
            long mins = ChronoUnit.MINUTES.between(at, LocalDateTime.now());
            if (mins < 1)   return "щойно";
            if (mins < 60)  return mins + " хв тому";
            long hrs = ChronoUnit.HOURS.between(at, LocalDateTime.now());
            if (hrs < 24)   return hrs + " год тому";
            return at.format(DateTimeFormatter.ofPattern("dd.MM HH:mm"));
        }
    }

    public interface NotificationListener {
        void onNotification(AppNotification notification);
    }

    private final Deque<AppNotification>         history   = new ArrayDeque<>();
    private final List<NotificationListener>     listeners = new CopyOnWriteArrayList<>();
    private static final int                     MAX_HISTORY = 50;

    public void addListener(NotificationListener l)    { listeners.add(l); }
    public void removeListener(NotificationListener l) { listeners.remove(l); }


    public void notifyBookingCreated(String tourName, String clientName) {
        push(new AppNotification(
              EventType.BOOKING_CREATED,
              "Нове бронювання: " + tourName + " · " + clientName,
              "📋", LocalDateTime.now()
        ));
    }

    public void notifyBookingConfirmed(String tourName, String clientName) {
        push(new AppNotification(
              EventType.BOOKING_CONFIRMED,
              "Підтверджено: " + tourName + " · " + clientName,
              "✅", LocalDateTime.now()
        ));
    }

    public void notifyBookingPaid(String tourName, String amount) {
        push(new AppNotification(
              EventType.BOOKING_PAID,
              "Оплата отримана: " + tourName + " · ₴" + amount,
              "💳", LocalDateTime.now()
        ));
    }

    public void notifyBookingCancelled(String tourName, String clientName) {
        push(new AppNotification(
              EventType.BOOKING_CANCELLED,
              "Бронювання скасовано: " + tourName + " · " + clientName,
              "❌", LocalDateTime.now()
        ));
    }

    public void notifyBookingCompleted(String tourName) {
        push(new AppNotification(
              EventType.BOOKING_COMPLETED,
              "Тур завершено: " + tourName,
              "🏁", LocalDateTime.now()
        ));
    }

    public void notifyExcelExported(String filename) {
        push(new AppNotification(
              EventType.EXCEL_EXPORTED,
              "Звіт збережено: " + filename,
              "📊", LocalDateTime.now()
        ));
    }

    public void notifyPdfExported(String filename) {
        push(new AppNotification(
              EventType.PDF_EXPORTED,
              "PDF звіт збережено: " + filename,
              "📄", LocalDateTime.now()
        ));
    }


    public void notifyClientBookingCancelled(String tourName, String refundNote) {
        push(new AppNotification(
              EventType.CLIENT_BOOKING_CANCELLED,
              "Бронювання «" + tourName + "» скасовано. " + refundNote,
              "🔔", LocalDateTime.now()
        ));
    }

    public void notifyClientBookingConfirmed(String tourName, String dates) {
        push(new AppNotification(
              EventType.CLIENT_BOOKING_CONFIRMED,
              "Ваше бронювання «" + tourName + "» підтверджено! " + dates,
              "✅", LocalDateTime.now()
        ));
    }

    public void notifyClientPaymentReceived(String tourName, String amount) {
        push(new AppNotification(
              EventType.CLIENT_BOOKING_PAID,
              "Оплата зарахована за тур «" + tourName + "» · " + amount,
              "💚", LocalDateTime.now()
        ));
    }

    public void notifyClientTourReminder(String tourName, long daysLeft) {
        String msg = daysLeft == 0
              ? "Сьогодні ваш виліт: «" + tourName + "»! Щасливої подорожі ✈"
              : "До туру «" + tourName + "» залишилося " + daysLeft + " дн.";
        push(new AppNotification(
              EventType.CLIENT_TOUR_REMINDER, msg,
              "🗓", LocalDateTime.now()
        ));
    }


    public List<AppNotification> getRecent(int limit) {
        List<AppNotification> list = new ArrayList<>(history);
        Collections.reverse(list);
        return list.subList(0, Math.min(limit, list.size()));
    }



    private void push(AppNotification n) {
        history.addLast(n);
        if (history.size() > MAX_HISTORY) history.pollFirst();
        for (NotificationListener l : listeners) {
            try { l.onNotification(n); } catch (Exception ignored) {}
        }
    }
}