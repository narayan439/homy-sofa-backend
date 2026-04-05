package com.homy.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UpdateProfileRequest {
    private String name;
    private String email;
    private String phone;
    private String address;

    public UpdateProfileRequest(String name, String email, String phone, String address) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.address = address;
    }
}
