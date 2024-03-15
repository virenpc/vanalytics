package com.example.demo.services;

import com.example.demo.records.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@Service
public class PostService {
    private final RestClient restClient;

    public PostService() {
        restClient = RestClient.builder()
                .baseUrl("https://apiconnect.angelbroking.com")
                .defaultHeaders(
                        httpHeaders -> {
                            httpHeaders.set("Content-Type", "application/json");
                            httpHeaders.set("Accept", "application/json");
                            httpHeaders.set("X-UserType", "USER"); // Adjust as needed
                            httpHeaders.set("X-SourceID", "WEB");
                            httpHeaders.set("X-ClientLocalIP", "CLIENT_LOCAL_IP"); // Implement method to get local IP
                            httpHeaders.set("X-ClientPublicIP", "CLIENT_PUBLIC_IP");
                            httpHeaders.set("X-MACAddress", "MAC_ADDRESS");
                            httpHeaders.set("X-PrivateKey", "1SoFDprx") ;// Implement method to retrieve API key securely)
                            // ... add other headers if needed
                        }
                )
                .build();



    }

    public ApiResponse login(User user) {
        return restClient.post()
                .uri("/rest/auth/angelbroking/user/v1/loginByPassword")
                .contentType(MediaType.APPLICATION_JSON)
                .body(user)
                .retrieve()
                .body(ApiResponse.class);
    }
    public CandleApiResponse getDailyCandles(String jwt, CandleDataRequest candleDataRequest)
    {
       return restClient.post()

                .uri("rest/secure/angelbroking/historical/v1/getCandleData")
                .contentType(MediaType.APPLICATION_JSON)

                .header("Authorization","Bearer "+jwt)
                .body(candleDataRequest)
                .retrieve()
                .body(new ParameterizedTypeReference<CandleApiResponse>(){});
    }

    public boolean processCandlesForBreakOutFailurePattern2(List<List<Object>> ohlcData) {
        // Step 1: Sort the list based on "high" attribute
        ohlcData.sort(Comparator.comparingDouble(candle -> (double) candle.get(2)));

        // Step 2: Find the second highest high candle
        List<Object> highestHighCandle = ohlcData.get(ohlcData.size() - 1);
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

    private static ZonedDateTime getDate(List<List<Object>> data, double high) {
        return data.stream()
                .filter(dohlcData -> (double) dohlcData.get(2) == high)
                .map(dohlcData -> ZonedDateTime.parse((CharSequence) dohlcData.get(0),DateTimeFormatter.ISO_OFFSET_DATE_TIME)) // Index 0 represents the date
                .findFirst()
                .orElse(null);
    }
    public void processCandlesForBreakOutFailurePattern(List<List<Object>> ohlcData) {

        ohlcData.stream()
                .filter(firstPeak -> findSecondPeak(ohlcData, ohlcData.indexOf(firstPeak), firstPeak) != -1)
                .filter(firstPeak -> isWithin2Percent(ohlcData.get(ohlcData.indexOf(firstPeak)), ohlcData.get(findSecondPeak(ohlcData, ohlcData.indexOf(firstPeak), firstPeak))))
                .filter(firstPeak -> calculateGapMonths(firstPeak, ohlcData.get(findSecondPeak(ohlcData, ohlcData.indexOf(firstPeak), firstPeak))) >= 6)
                .filter(firstPeak -> {
                    List<Object> secondPeak = ohlcData.get(findSecondPeak(ohlcData, ohlcData.indexOf(firstPeak), firstPeak));
                    int nextMonthIndex = ohlcData.indexOf(secondPeak) + 30; // Assuming 1 month = 30 days
                    if (nextMonthIndex < ohlcData.size()) {
                        List<Object> afterGapData = ohlcData.get(nextMonthIndex);
                        return isDropOfAtLeast10Percent(secondPeak, afterGapData);
                    }
                    return false;
                })
                .filter(firstPeak -> {
                    List<Object> secondPeak = ohlcData.get(findSecondPeak(ohlcData, ohlcData.indexOf(firstPeak), firstPeak));
                    List<Object> lastData = ohlcData.get(ohlcData.size() - 1);
                    return isWithin5PercentRange(secondPeak, lastData);
                })
                .forEach(firstPeak -> System.out.println("Double top failure pattern detected!"));

    }
    private static int findSecondPeak(List<List<Object>> ohlcData, int startIndex, List<Object> firstPeak) {
        return ohlcData.stream()
                .skip(startIndex)
                .filter(secondPeak -> ZonedDateTime.parse((CharSequence) secondPeak.get(0), DateTimeFormatter.ISO_OFFSET_DATE_TIME).isAfter(ZonedDateTime.parse((CharSequence) firstPeak.get(0), DateTimeFormatter.ISO_OFFSET_DATE_TIME)))
                .filter(secondPeak -> (Double) secondPeak.get(2) <= (Double) firstPeak.get(2) * 1.02) // Check if within 2% range
                .mapToInt(ohlcData::indexOf)
                .findFirst()
                .orElse(-1);
    }

    private static boolean isWithin2Percent(List<Object> firstPeak, List<Object> secondPeak) {
        return (Double) secondPeak.get(2) <= (Double)firstPeak.get(2) * 1.02; // Check if within 2% range
    }

    private static int calculateGapMonths(List<Object> firstPeak, List<Object> secondPeak) {
        return (int) ZonedDateTime.parse((CharSequence) firstPeak.get(0), DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDate().until(ZonedDateTime.parse((CharSequence) secondPeak.get(0), DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDate()).toTotalMonths();
    }

    private static boolean isDropOfAtLeast10Percent(List<Object> peak, List<Object> afterGapData) {
        return ((Double) peak.get(2) - (Double) afterGapData.get(3)) / (Double) peak.get(2) >= 0.1; // Check for a drop of at least 10%
    }

    private static boolean isWithin5PercentRange(List<Object> secondPeak, List<Object> lastData) {
        return ((Double)lastData.get(4) >= (Double)secondPeak.get(2) * 0.95) && ((Double)lastData.get(4) <= (Double)secondPeak.get(2) * 1.05); // Check if within a 5% range
//        return ((Double)lastData.get(4) >= (Double)secondPeak.get(2) * 0.95) && ((Double)lastData.get(4) <= (Double)secondPeak.get(2) * 1.9);
    }
}
