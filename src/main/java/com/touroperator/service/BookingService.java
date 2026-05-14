package com.touroperator.service;

import com.touroperator.domain.*;
import com.touroperator.dto.BookingRequest;

import com.touroperator.domain.BookingStatus;
import com.touroperator.exception.*;
import com.touroperator.repository.*;
import com.touroperator.specification.BookingByClientEmailSpec;
import com.touroperator.uow.UnitOfWork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;


@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository    bookingRepo;
    private final TourRepository       tourRepo;
    private final ClientRepository     clientRepo;
    private final ExcursionRepository  excursionRepo;
    private final InsuranceRepository  insuranceRepo;
    private final TransferRepository   transferRepo;
    private final PromoCodeRepository  promoRepo;
    private final PaymentRepository    paymentRepo;
    private final PricingService       pricingService;
    private final NotificationService  notificationService;
    private final EmailService         emailService;
    private final UnitOfWork           unitOfWork;

    public BookingService(BookingRepository bookingRepo,
          TourRepository tourRepo,
          ClientRepository clientRepo,
          ExcursionRepository excursionRepo,
          InsuranceRepository insuranceRepo,
          TransferRepository transferRepo,
          PromoCodeRepository promoRepo,
          PaymentRepository paymentRepo,
          PricingService pricingService,
          NotificationService notificationService,
          EmailService emailService,
          UnitOfWork unitOfWork) {
        this.bookingRepo         = bookingRepo;
        this.tourRepo            = tourRepo;
        this.clientRepo          = clientRepo;
        this.excursionRepo       = excursionRepo;
        this.insuranceRepo       = insuranceRepo;
        this.transferRepo        = transferRepo;
        this.promoRepo           = promoRepo;
        this.paymentRepo         = paymentRepo;
        this.pricingService      = pricingService;
        this.notificationService = notificationService;
        this.emailService        = emailService;
        this.unitOfWork          = unitOfWork;
    }


    public PriceBreakdown previewPrice(BookingRequest req) {
        Tour tour = tourRepo.findById(req.getTourId())
              .orElseThrow(() ->
                    new EntityNotFoundException("Тур", req.getTourId()));

        return pricingService.calculate(
              tour,
              req.getTouristCount(),
              req.getExcursionIds(),
              req.getInsuranceId(),
              req.getTransferId(),
              req.getPromoCode()
        );
    }


    public Booking createBooking(BookingRequest req) {
        log.info("Створення бронювання: тур={}, туристи={}", req.getTourId(), req.getTouristCount());


        Tour tour = tourRepo.findById(req.getTourId())
              .orElseThrow(() ->
                    new EntityNotFoundException("Тур", req.getTourId()));



        int available = tour.getAvailableSeats();
        if (req.getTouristCount() > available) {
            throw new QuotaExceededException(req.getTouristCount(), available);
        }
        if (req.getTouristCount() <= 0) {
            throw new IllegalArgumentException("Кількість туристів має бути більше 0");
        }


        clientRepo.findById(req.getClientId())
              .orElseThrow(() ->
                    new EntityNotFoundException("Клієнт", req.getClientId()));


        if (bookingRepo.existsActiveByClientAndTour(req.getClientId(), req.getTourId())) {
            throw new InvalidBookingStateException(
                  "Клієнт вже має активне бронювання на цей тур. " +
                        "Скасуйте попереднє бронювання перш ніж створювати нове.");
        }


        UUID promoCodeId = null;
        if (req.getPromoCode() != null && !req.getPromoCode().isBlank()) {
            PromoCode pc = promoRepo.findByCode(req.getPromoCode().trim())
                  .orElse(null);
            if (pc != null) {
                if (!pc.isValid()) {
                    throw new PromoCodeExpiredException(
                          "Промокод '" + pc.getCode() + "' прострочений (дійсний до " + pc.getValidUntil() + ")");
                }
                promoCodeId = pc.getId();
            }
        }


        PriceBreakdown breakdown = pricingService.calculate(
              tour,
              req.getTouristCount(),
              req.getChildCount(),
              req.getExcursionIds(),
              req.getInsuranceId(),
              req.getTransferId(),
              req.getPromoCode(),
              req.getExtraDiscountPercent()
        );

        log.info("Розрахунок ціни: {}", breakdown);


        Booking booking = new Booking();
        booking.setId(UUID.randomUUID());
        booking.setClientId(req.getClientId());
        booking.setTourId(req.getTourId());
        booking.setPromoCodeId(promoCodeId);
        booking.setTouristCount(req.getTouristCount());
        booking.setBookingDate(LocalDate.now());
        booking.setStatus(BookingStatus.CREATED.name());
        booking.setTotalPrice(breakdown.getFinalPrice());


        unitOfWork.registerNewBooking(booking);
        unitOfWork.commit();
        tourRepo.incrementBookedSeats(tour.getId(), req.getTouristCount());


        if (req.getExcursionIds() != null && !req.getExcursionIds().isEmpty()) {
            excursionRepo.saveBookingExcursions(booking.getId(), req.getExcursionIds());
        }
        if (req.getInsuranceId() != null) {
            insuranceRepo.saveBookingInsurance(booking.getId(), req.getInsuranceId());
        }
        if (req.getTransferId() != null) {
            transferRepo.saveBookingTransfer(booking.getId(), req.getTransferId());
        }

        log.info("Бронювання створено: {} | Ціна: {} USD", booking.getId(), booking.getTotalPrice());
        try {
            com.touroperator.domain.Client client = clientRepo.findById(req.getClientId()).orElse(null);
            String clientName = client != null ? client.getName() : "клієнт";
            notificationService.notifyBookingCreated(tour.getName(), clientName);
        } catch (Exception ignored) {}
        return booking;
    }

    public Booking confirmBooking(UUID bookingId) {
        Booking booking = getBookingOrThrow(bookingId);
        assertStatus(booking, BookingStatus.CREATED,
              "Можна підтвердити тільки бронювання зі статусом CREATED");
        bookingRepo.confirm(bookingId);
        booking.setStatus(BookingStatus.CONFIRMED.name());
        log.info("Бронювання підтверджено: {}", bookingId);
        try {
            Tour tour = tourRepo.findById(booking.getTourId()).orElse(null);
            String tourName = tour != null ? tour.getName() : "тур";
            com.touroperator.domain.Client client = clientRepo.findById(booking.getClientId()).orElse(null);
            String clientName = client != null ? client.getName() : "клієнт";
            notificationService.notifyBookingConfirmed(tourName, clientName);
            // Клієнтське сповіщення — тур підтверджено
            String dates = tour != null && tour.getStartDate() != null
                  ? tour.getStartDate() + " – " + tour.getEndDate() : "";
            notificationService.notifyClientBookingConfirmed(tourName, dates.isBlank() ? "" : "Дати: " + dates);
            // ── E-mail клієнту ──────────────────────────────────────────────
            if (client != null && client.getEmail() != null) {
                emailService.sendBookingConfirmed(
                      client.getEmail(), clientName, tourName,
                      dates.isBlank() ? "" : dates);
            }
        } catch (Exception ignored) {}
        return booking;
    }


    public Payment payBooking(UUID bookingId, String method) {
        Booking booking = getBookingOrThrow(bookingId);
        assertStatus(booking, BookingStatus.CONFIRMED,
              "Можна оплатити тільки підтверджене бронювання (CONFIRMED)");

        // Створити платіж
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setBookingId(bookingId);
        payment.setAmount(booking.getTotalPrice());
        payment.setPaymentDate(LocalDate.now());
        payment.setStatus("SUCCESS");

        paymentRepo.save(payment);
        bookingRepo.markPaid(bookingId);
        log.info("Оплата успішна: booking={}, amount={}, method={}", bookingId, booking.getTotalPrice(), method);
        try {
            Tour tour = tourRepo.findById(booking.getTourId()).orElse(null);
            String tourName = tour != null ? tour.getName() : "тур";
            notificationService.notifyBookingPaid(tourName, String.format("%,.0f", booking.getTotalPrice()));
            // Клієнтське сповіщення — оплата зарахована
            notificationService.notifyClientPaymentReceived(tourName,
                  "₴" + String.format("%,.0f", booking.getTotalPrice()));
            // ── E-mail клієнту ──────────────────────────────────────────────
            com.touroperator.domain.Client clientForEmail =
                  clientRepo.findById(booking.getClientId()).orElse(null);
            if (clientForEmail != null && clientForEmail.getEmail() != null) {
                emailService.sendPaymentReceived(
                      clientForEmail.getEmail(),
                      clientForEmail.getName(),
                      tourName,
                      "₴" + String.format("%,.0f", booking.getTotalPrice()));
            }
        } catch (Exception ignored) {}
        return payment;
    }

    public void completeBooking(UUID bookingId) {
        Booking booking = getBookingOrThrow(bookingId);
        assertStatus(booking, BookingStatus.PAID,
              "Можна завершити тільки оплачене бронювання (PAID)");
        bookingRepo.markCompleted(bookingId);
        log.info("Бронювання завершено: {}", bookingId);
        try {
            Tour tour = tourRepo.findById(booking.getTourId()).orElse(null);
            if (tour != null) notificationService.notifyBookingCompleted(tour.getName());
        } catch (Exception ignored) {}
    }


    public void cancelBooking(UUID bookingId, String reason) {
        Booking booking = getBookingOrThrow(bookingId);

        String status = booking.getStatus();
        if (BookingStatus.CANCELLED.name().equals(status) ||
              BookingStatus.COMPLETED.name().equals(status)) {
            throw new InvalidBookingStateException(
                  "Не можна скасувати бронювання зі статусом " + status);
        }


        bookingRepo.cancel(bookingId, reason);


        tourRepo.decrementBookedSeats(booking.getTourId(), booking.getTouristCount());

        log.info("Бронювання скасовано: {}, місця повернуто: {}", bookingId, booking.getTouristCount());
        try {
            Tour tour = tourRepo.findById(booking.getTourId()).orElse(null);
            String tourName = tour != null ? tour.getName() : "тур";
            com.touroperator.domain.Client client = clientRepo.findById(booking.getClientId()).orElse(null);
            String clientName = client != null ? client.getName() : "клієнт";
            notificationService.notifyBookingCancelled(tourName, clientName);
            // Клієнтське сповіщення — з різним текстом залежно від статусу до скасування
            String refundNote = BookingStatus.PAID.name().equals(status)
                  ? "Кошти буде повернуто протягом 3–5 робочих днів."
                  : "Якщо є питання — зверніться до підтримки.";
            notificationService.notifyClientBookingCancelled(tourName, refundNote);
            if (client != null && client.getEmail() != null) {
                emailService.sendBookingCancelled(
                      client.getEmail(), clientName, tourName, refundNote);
            }
        } catch (Exception ignored) {}


        if (BookingStatus.PAID.name().equals(status)) {
            log.warn("Бронювання {} було оплачено — потрібен рефанд!", bookingId);
        }
    }



    public Booking getBookingById(UUID id) {
        return getBookingOrThrow(id);
    }

    public List<Booking> getBookingsByClient(UUID clientId) {
        return bookingRepo.findByClientId(clientId);
    }

    public List<Booking> getBookingsByTour(UUID tourId) {
        return bookingRepo.findByTourId(tourId);
    }

    public List<Booking> getAllBookings() {
        return bookingRepo.findAll();
    }



    private Booking getBookingOrThrow(UUID id) {
        return bookingRepo.findById(id)
              .orElseThrow(() ->
                    new EntityNotFoundException("Бронювання", id));
    }

    private void assertStatus(Booking booking, BookingStatus expected, String message) {
        if (!expected.name().equals(booking.getStatus())) {
            throw new InvalidBookingStateException(
                  message + ". Поточний статус: " + booking.getStatus());
        }
    }


    public List<Booking> findAll() {
        return getAllBookings();
    }


    public Booking createBooking(UUID clientId, UUID tourId,
          int adults, int children, String promoCode) {
        com.touroperator.dto.BookingRequest req = new com.touroperator.dto.BookingRequest();
        req.setClientId(clientId);
        req.setTourId(tourId);
        req.setTouristCount(adults + children);
        req.setPromoCode(promoCode);
        return createBooking(req);
    }

    /**
     * Пошук бронювань за email клієнта через Specification (без SQL injection).
     */
    public List<Booking> findByClientEmail(String email) {
        return bookingRepo.findBySpec(new BookingByClientEmailSpec(email));
    }
}