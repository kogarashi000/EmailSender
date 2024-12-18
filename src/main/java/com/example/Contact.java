package com.example;

public class Contact {
    private String email;
    private String name;

    public Contact(String email, String name) {
        this.email = email;
        this.name = name;
    }

    // Геттеры
    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    // Сеттеры, если необходимы
}
