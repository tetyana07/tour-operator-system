package com.touroperator.specification;

public interface Specification {
    String toSql();
    Object[] getParams();
}