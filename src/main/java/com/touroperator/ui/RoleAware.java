package com.touroperator.ui;

/**
 * Інтерфейс для контролерів сторінок, яким потрібна поточна роль.
 * MainController викликає setRole() одразу після завантаження FXML.
 */
public interface RoleAware {
    void setRole(UserRole role, String email);
}
