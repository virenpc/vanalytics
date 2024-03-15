package com.example.demo.services;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class CandleService {
    public boolean processCandlesForBreakOutFailurePattern2(List<List<Object>> ohlcData) {
        // Step 1: Sort the list based on "high" attribute
        ohlcData.sort(Comparator.comparingDouble(candle -> (double) candle.get(2)));

        // Step 2: Find the second highest high candle
        List<Object> highestHighCandle = ohlcData.getLast();
        List<Object> secondHighestPivotHighCandle = null;

        for (int i = ohlcData.size() - 2; i >= 0; i--) {
            List<Object> currentCandle = ohlcData.get(i);
            LocalDate highestDate = ZonedDateTime.parse((CharSequence) highestHighCandle.get(0)).toLocalDate();
            LocalDate currentDate = ZonedDateTime.parse((CharSequence) currentCandle.get(0)).toLocalDate();
            double highestHigh = (double) highestHighCandle.get(2);
            double currentHigh = (double) currentCandle.get(2);

            // Check if the current candle is 6 months before and within 5% below the highest high
            if (currentDate.isBefore(highestDate.minusMonths(6)) &&
                    highestHigh - currentHigh <= highestHigh * 0.05) {
                // Check if the difference in heights is within 2%
                if (highestHigh - currentHigh <= highestHigh * 0.02) {
                    secondHighestPivotHighCandle = currentCandle;
                    break; // No need to iterate further
                }
            } else {
                // Since the list is sorted, if this condition fails, no need to iterate further
                break;
            }
        }

        // Step 3: Print the result
        if (secondHighestPivotHighCandle != null) {
            System.out.println("Second highest high candle meeting criteria: " + secondHighestPivotHighCandle);
            return true;
        } else {
            System.out.println("No second highest high candle meeting criteria found");
            return false;
        }

    }
}
