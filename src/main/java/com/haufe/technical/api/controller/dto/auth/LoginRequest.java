package com.haufe.technical.api.controller.dto.auth;

public record LoginRequest(String username, String password, String role) {}
