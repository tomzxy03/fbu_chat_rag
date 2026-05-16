package com.tomzxy.fbu_chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String username;
    private String role;
}
