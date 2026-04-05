package com.homy.backend.dto;

public class LoginResponse {
    private String token;
    private Long userId;
    private String email;
    private String name;
    private String phone;
    private String message;

    public LoginResponse(String token, Long userId, String email, String name, String phone) {
        this.token = token;
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.phone = phone;
        this.message = "Login successful";
    }

    public LoginResponse(String message) {
        this.message = message;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
