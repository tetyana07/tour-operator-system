package com.touroperator.command;

import com.touroperator.service.BookingService;

import java.util.UUID;


public final class BookingCommands {

    private BookingCommands() {}


    public static class ConfirmBookingCommand implements BookingCommand {

        private final BookingService bookingService;
        private final UUID           bookingId;
        private final String         bookingLabel; // для відображення в UI

        public ConfirmBookingCommand(BookingService bookingService,
                                     UUID bookingId,
                                     String bookingLabel) {
            this.bookingService = bookingService;
            this.bookingId      = bookingId;
            this.bookingLabel   = bookingLabel;
        }

        @Override
        public void execute() {
            bookingService.confirmBooking(bookingId);
        }

        @Override
        public void undo() {

            bookingService.cancelBooking(bookingId, "Скасовано через Undo (відміна підтвердження)");
        }

        @Override
        public String describe() {
            return "Підтвердження бронювання " + shortId();
        }

        @Override
        public boolean isUndoable() {
            return true;
        }

        private String shortId() {
            return bookingLabel != null ? bookingLabel
                    : "#" + bookingId.toString().substring(0, 8);
        }
    }


    public static class CancelBookingCommand implements BookingCommand {

        private final BookingService bookingService;
        private final UUID           bookingId;
        private final String         reason;
        private final String         bookingLabel;
        private final String         previousStatus;

        public CancelBookingCommand(BookingService bookingService,
                                    UUID bookingId,
                                    String reason,
                                    String previousStatus,
                                    String bookingLabel) {
            this.bookingService  = bookingService;
            this.bookingId       = bookingId;
            this.reason          = reason;
            this.previousStatus  = previousStatus;
            this.bookingLabel    = bookingLabel;
        }

        @Override
        public void execute() {
            bookingService.cancelBooking(bookingId, reason);
        }

        @Override
        public void undo() {
            throw new UnsupportedOperationException(
                    "Скасування незворотне: місця вже повернено в квоту туру. " +
                    "Створіть нове бронювання вручну.");
        }

        @Override
        public String describe() {
            return "Скасування бронювання " + shortId()
                    + " (було: " + previousStatus + ")";
        }

        @Override
        public boolean isUndoable() {
            return false;
        }

        private String shortId() {
            return bookingLabel != null ? bookingLabel
                    : "#" + bookingId.toString().substring(0, 8);
        }
    }


    public static class CompleteBookingCommand implements BookingCommand {

        private final BookingService bookingService;
        private final UUID           bookingId;
        private final String         bookingLabel;

        public CompleteBookingCommand(BookingService bookingService,
                                      UUID bookingId,
                                      String bookingLabel) {
            this.bookingService = bookingService;
            this.bookingId      = bookingId;
            this.bookingLabel   = bookingLabel;
        }

        @Override
        public void execute() {
            bookingService.completeBooking(bookingId);
        }

        @Override
        public void undo() {
            throw new UnsupportedOperationException(
                    "Завершення незворотне: тур вже відбувся.");
        }

        @Override
        public String describe() {
            return "Завершення бронювання " + shortId();
        }

        @Override
        public boolean isUndoable() {
            return false;
        }

        private String shortId() {
            return bookingLabel != null ? bookingLabel
                    : "#" + bookingId.toString().substring(0, 8);
        }
    }
}
