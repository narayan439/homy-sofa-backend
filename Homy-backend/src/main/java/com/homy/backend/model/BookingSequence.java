package com.homy.backend.model;

// BookingSequence was previously mapped as a JPA entity and caused Hibernate
// to attempt creating a 'booking_sequence' table. The sequence implementation
// was removed in favor of using database-generated IDs. Keep this class as
// a simple POJO if you need to persist a manual sequence externally; otherwise
// it can be deleted.
public class BookingSequence {
    private int id = 1; // singleton row
    private long lastValue = 0;

    public BookingSequence() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public long getLastValue() { return lastValue; }
    public void setLastValue(long lastValue) { this.lastValue = lastValue; }
}
