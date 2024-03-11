package com.example.demo.records;

public record Data(
        String jwtToken,
        String refreshToken,
        String feedToken
) {}
