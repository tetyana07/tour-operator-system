package com.touroperator.specification;

public class BookingByClientEmailSpec implements Specification {

    private final String email;

    public BookingByClientEmailSpec(String email) {
        this.email = email;
    }

    @Override
    public String toSql() {
        return "client_id IN (SELECT id FROM clients WHERE email = ?)";
    }

    @Override
    public Object[] getParams() {
        return new Object[]{ email };
    }
}