package com.example.demo.records;

public record ApiResponse(
        boolean status,
        String message,
        String errorcode, // Assuming "errorcode" is intended as a single word
        Data data
) {}

