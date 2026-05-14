package com.touroperator.exception;


public class EntityNotFoundException extends TourOperatorException {
    public EntityNotFoundException(String entity, Object id) {
        super(entity + " не знайдено: " + id);
    }
}
