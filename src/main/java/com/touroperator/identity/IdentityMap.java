package com.touroperator.identity;

import com.touroperator.domain.Booking;
import com.touroperator.domain.Client;
import com.touroperator.domain.Tour;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;


@Component
public class IdentityMap {

    private final Map<UUID, Client>  clientCache  = new HashMap<>();
    private final Map<UUID, Tour>    tourCache    = new HashMap<>();
    private final Map<UUID, Booking> bookingCache = new HashMap<>();

    public Optional<Client>  getClient(UUID id)  { return Optional.ofNullable(clientCache.get(id)); }
    public Optional<Tour>    getTour(UUID id)     { return Optional.ofNullable(tourCache.get(id)); }
    public Optional<Booking> getBooking(UUID id)  { return Optional.ofNullable(bookingCache.get(id)); }

    public void putClient(Client c)   { if (c != null && c.getId() != null) clientCache.put(c.getId(), c); }
    public void putTour(Tour t)       { if (t != null && t.getId() != null) tourCache.put(t.getId(), t); }
    public void putBooking(Booking b) { if (b != null && b.getId() != null) bookingCache.put(b.getId(), b); }


    public void invalidateClient(UUID id)  { clientCache.remove(id); }
    public void invalidateTour(UUID id)    { tourCache.remove(id); }
    public void invalidateBooking(UUID id) { bookingCache.remove(id); }


    public void clear() {
        clientCache.clear();
        tourCache.clear();
        bookingCache.clear();
    }
}
