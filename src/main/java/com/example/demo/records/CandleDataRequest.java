package com.example.demo.records;

public record CandleDataRequest(
                                String exchange,
                                String symboltoken,
                                String interval,
                                String fromdate,
                                String todate) {
}
