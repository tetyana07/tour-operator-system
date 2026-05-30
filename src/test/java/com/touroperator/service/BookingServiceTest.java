package com.touroperator.service;

import com.touroperator.domain.*;
import com.touroperator.dto.BookingRequest;
import com.touroperator.exception.EntityNotFoundException;
import com.touroperator.exception.InvalidBookingStateException;
import com.touroperator.exception.QuotaExceededException;
import com.touroperator.repository.*;
import com.touroperator.uow.UnitOfWork;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit-тести для {@link BookingService}.
 *
 * <p>Покриває:
 * <ul>
 *   <li>Успішне створення бронювання</li>
 *   <li>Перевірку квоти (QuotaExceededException)</li>
 *   <li>Перевірку дублювання бронювання</li>
 *   <li>State-machine: confirm → pay → complete</li>
 *   <li>Скасування і рефанд при PAID</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BookingService — бізнес-логіка бронювань")
class BookingServiceTest {

    @Mock private BookingRepository   bookingRepo;
    @Mock private TourRepository      tourRepo;
    @Mock private ClientRepository    clientRepo;
    @Mock private ExcursionRepository excursionRepo;
    @Mock private InsuranceRepository insuranceRepo;
    @Mock private TransferRepository  transferRepo;
    @Mock private PromoCodeRepository promoRepo;
    @Mock private PaymentRepository   paymentRepo;
    @Mock private PricingService      pricingService;
    @Mock private NotificationService notificationService;
    @Mock private EmailService        emailService;
    @Mock private UnitOfWork          unitOfWork;

    @InjectMocks
    private BookingService bookingService;

    private UUID tourId;
    private UUID clientId;
    private Tour tour;
    private Client client;

    @BeforeEach
    void setUp() {
        tourId   = UUID.randomUUID();
        clientId = UUID.randomUUID();

        tour = new Tour();
        tour.setId(tourId);
        tour.setName("Тур до Туреччини");
        tour.setCountry("Туреччина");
        tour.setCity("Анталія");
        tour.setBasePrice(new BigDecimal("800.00"));
        tour.setQuota(20);
        tour.setBookedSeats(5);
        tour.setStartDate(LocalDate.now().plusDays(30));
        tour.setEndDate(LocalDate.now().plusDays(37));

        client = new Client();
        client.setId(clientId);
        client.setName("Тест Клієнт");
        client.setEmail("test@example.com");
    }

    // ── Створення бронювання ──────────────────────────────────────────────

    @Nested
    @DisplayName("createBooking()")
    class CreateBookingTests {

        @Test
        @DisplayName("Успішне створення: усі дані валідні → бронювання збережено")
        void success_bookingCreated() {
            when(tourRepo.findById(tourId)).thenReturn(Optional.of(tour));
            when(clientRepo.findById(clientId)).thenReturn(Optional.of(client));
            when(bookingRepo.existsActiveByClientAndTour(clientId, tourId)).thenReturn(false);
            when(pricingService.calculate(any(), anyInt(), anyInt(), any(), any(), any(), any(), anyInt()))
                    .thenReturn(fakePriceBreakdown(new BigDecimal("800.00")));

            BookingRequest req = new BookingRequest();
            req.setTourId(tourId);
            req.setClientId(clientId);
            req.setTouristCount(2);

            Booking result = bookingService.createBooking(req);

            assertNotNull(result);
            assertEquals(clientId, result.getClientId());
            assertEquals(tourId,   result.getTourId());
            assertEquals("CREATED", result.getStatus());
            verify(bookingRepo).save(any(Booking.class));
            verify(tourRepo).incrementBookedSeats(eq(tourId), eq(2));
        }

        @Test
        @DisplayName("Тур не знайдено → EntityNotFoundException")
        void tourNotFound_throws() {
            when(tourRepo.findById(tourId)).thenReturn(Optional.empty());

            BookingRequest req = new BookingRequest();
            req.setTourId(tourId);
            req.setClientId(clientId);
            req.setTouristCount(1);

            assertThrows(EntityNotFoundException.class,
                    () -> bookingService.createBooking(req));
        }

        @Test
        @DisplayName("Перевищення квоти → QuotaExceededException")
        void quotaExceeded_throws() {
            tour.setBookedSeats(19); // лишилось 1 місце
            when(tourRepo.findById(tourId)).thenReturn(Optional.of(tour));

            BookingRequest req = new BookingRequest();
            req.setTourId(tourId);
            req.setClientId(clientId);
            req.setTouristCount(5); // хочемо 5, а є лише 1

            assertThrows(QuotaExceededException.class,
                    () -> bookingService.createBooking(req));
        }

        @Test
        @DisplayName("Клієнт вже має активне бронювання → InvalidBookingStateException")
        void duplicateBooking_throws() {
            when(tourRepo.findById(tourId)).thenReturn(Optional.of(tour));
            when(clientRepo.findById(clientId)).thenReturn(Optional.of(client));
            when(bookingRepo.existsActiveByClientAndTour(clientId, tourId)).thenReturn(true);

            BookingRequest req = new BookingRequest();
            req.setTourId(tourId);
            req.setClientId(clientId);
            req.setTouristCount(1);

            assertThrows(InvalidBookingStateException.class,
                    () -> bookingService.createBooking(req));
        }
    }

    // ── Підтвердження бронювання ──────────────────────────────────────────

    @Nested
    @DisplayName("confirmBooking()")
    class ConfirmBookingTests {

        @Test
        @DisplayName("CREATED → CONFIRMED: успішно")
        void confirm_createdBooking_success() {
            UUID bookingId = UUID.randomUUID();
            Booking booking = buildBooking(bookingId, "CREATED");
            when(bookingRepo.findById(bookingId)).thenReturn(Optional.of(booking));
            when(tourRepo.findById(any())).thenReturn(Optional.of(tour));
            when(clientRepo.findById(any())).thenReturn(Optional.of(client));

            Booking result = bookingService.confirmBooking(bookingId);

            assertEquals("CONFIRMED", result.getStatus());
            verify(bookingRepo).confirm(bookingId);
        }

        @Test
        @DisplayName("CONFIRMED → confirmBooking() → InvalidBookingStateException")
        void confirm_alreadyConfirmed_throws() {
            UUID bookingId = UUID.randomUUID();
            Booking booking = buildBooking(bookingId, "CONFIRMED");
            when(bookingRepo.findById(bookingId)).thenReturn(Optional.of(booking));

            assertThrows(InvalidBookingStateException.class,
                    () -> bookingService.confirmBooking(bookingId));
        }

        @Test
        @DisplayName("PAID → confirmBooking() → InvalidBookingStateException")
        void confirm_paidBooking_throws() {
            UUID bookingId = UUID.randomUUID();
            Booking booking = buildBooking(bookingId, "PAID");
            when(bookingRepo.findById(bookingId)).thenReturn(Optional.of(booking));

            assertThrows(InvalidBookingStateException.class,
                    () -> bookingService.confirmBooking(bookingId));
        }
    }

    // ── Оплата бронювання ─────────────────────────────────────────────────

    @Nested
    @DisplayName("payBooking()")
    class PayBookingTests {

        @Test
        @DisplayName("CONFIRMED → Payment збережений, статус PAID")
        void pay_confirmedBooking_success() {
            UUID bookingId = UUID.randomUUID();
            Booking booking = buildBooking(bookingId, "CONFIRMED");
            when(bookingRepo.findById(bookingId)).thenReturn(Optional.of(booking));
            when(tourRepo.findById(any())).thenReturn(Optional.of(tour));
            when(clientRepo.findById(any())).thenReturn(Optional.of(client));

            Payment payment = bookingService.payBooking(bookingId, "CARD");

            assertNotNull(payment);
            assertEquals(new BigDecimal("800.00"), payment.getAmount());
            assertEquals("SUCCESS", payment.getStatus());
            verify(paymentRepo).save(any(Payment.class));
            verify(bookingRepo).markPaid(bookingId);
        }

        @Test
        @DisplayName("CREATED → payBooking() → InvalidBookingStateException")
        void pay_notConfirmed_throws() {
            UUID bookingId = UUID.randomUUID();
            Booking booking = buildBooking(bookingId, "CREATED");
            when(bookingRepo.findById(bookingId)).thenReturn(Optional.of(booking));

            assertThrows(InvalidBookingStateException.class,
                    () -> bookingService.payBooking(bookingId, "CASH"));
        }
    }

    // ── Скасування ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("cancelBooking()")
    class CancelBookingTests {

        @Test
        @DisplayName("Скасування CREATED → місця повертаються, рефанду немає")
        void cancel_created_seatsRestored_noRefund() {
            UUID bookingId = UUID.randomUUID();
            Booking booking = buildBooking(bookingId, "CREATED");
            booking.setTouristCount(2);
            when(bookingRepo.findById(bookingId)).thenReturn(Optional.of(booking));
            when(tourRepo.findById(any())).thenReturn(Optional.of(tour));
            when(clientRepo.findById(any())).thenReturn(Optional.of(client));

            bookingService.cancelBooking(bookingId, "Змінились плани");

            verify(bookingRepo).cancel(eq(bookingId), eq("Змінились плани"));
            verify(tourRepo).decrementBookedSeats(any(), eq(2));
            // рефанд НЕ має зберігатись (статус не PAID)
            verify(paymentRepo, never()).save(any());
        }

        @Test
        @DisplayName("Скасування PAID → рефанд зберігається як від'ємний платіж")
        void cancel_paid_refundCreated() {
            UUID bookingId = UUID.randomUUID();
            Booking booking = buildBooking(bookingId, "PAID");
            booking.setTouristCount(1);
            when(bookingRepo.findById(bookingId)).thenReturn(Optional.of(booking));
            when(tourRepo.findById(any())).thenReturn(Optional.of(tour));
            when(clientRepo.findById(any())).thenReturn(Optional.of(client));

            bookingService.cancelBooking(bookingId, "Відмова");

            verify(paymentRepo).save(argThat(p ->
                    "REFUND".equals(p.getStatus()) &&
                    p.getAmount().compareTo(BigDecimal.ZERO) < 0
            ));
        }

        @Test
        @DisplayName("Скасування CANCELLED → InvalidBookingStateException")
        void cancel_alreadyCancelled_throws() {
            UUID bookingId = UUID.randomUUID();
            Booking booking = buildBooking(bookingId, "CANCELLED");
            when(bookingRepo.findById(bookingId)).thenReturn(Optional.of(booking));

            assertThrows(InvalidBookingStateException.class,
                    () -> bookingService.cancelBooking(bookingId, "Повторне скасування"));
        }

        @Test
        @DisplayName("Скасування COMPLETED → InvalidBookingStateException")
        void cancel_completed_throws() {
            UUID bookingId = UUID.randomUUID();
            Booking booking = buildBooking(bookingId, "COMPLETED");
            when(bookingRepo.findById(bookingId)).thenReturn(Optional.of(booking));

            assertThrows(InvalidBookingStateException.class,
                    () -> bookingService.cancelBooking(bookingId, "Завершено"));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private Booking buildBooking(UUID id, String status) {
        Booking b = new Booking();
        b.setId(id);
        b.setClientId(clientId);
        b.setTourId(tourId);
        b.setStatus(status);
        b.setTouristCount(1);
        b.setTotalPrice(new BigDecimal("800.00"));
        b.setBookingDate(LocalDate.now());
        return b;
    }

    private PriceBreakdown fakePriceBreakdown(BigDecimal price) {
        return new PriceBreakdown(price, BigDecimal.ZERO,
                java.util.Collections.emptyList(), BigDecimal.ZERO, price);
    }
}
