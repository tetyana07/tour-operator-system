package com.touroperator.domain;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public class Client {
    private UUID id;
    private String name;
    private String email;
    private String phone;
    private LocalDate birthDate;
    private String passwordHash;
    private String verifyToken;
    private boolean emailVerified;

    public String getVerifyToken() { return verifyToken; }
    public void setVerifyToken(String verifyToken) { this.verifyToken = verifyToken; }

    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }

    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getPhone() {
        return phone;
    }
    public void setPhone(String phone) {
        this.phone = phone;
    }
    public LocalDate getBirthDate() {
        return birthDate;
    }
    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }
    public UUID getId() {
        return id;
    }
    public void setId(UUID id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getPasswordHash() { return passwordHash; }
    public void   setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public Client(){}
    public Client(String name, String email, String phone, LocalDate birthDate) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.birthDate = birthDate;
    }
    public Client(UUID id, String name, String email, String phone, LocalDate birthDate) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.birthDate = birthDate;
    }
    @Override
    public String toString() {
        return String.format("Client [id=%s, name='%s', email='%s', phone='%s', birthDate='%s']",
              id, name, email, phone, birthDate);
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Client client = (Client) o;
        return Objects.equals(id, client.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }


}