package com.example.demo.records;

import java.util.List;

public record CandleApiResponse(
                                boolean status,
                                String message,
                                String errorcode,
                                List<List<Object>> data
) {}