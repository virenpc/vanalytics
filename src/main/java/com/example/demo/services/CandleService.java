package com.example.demo.services;

import com.example.demo.records.CandleApiResponse;
import com.example.demo.records.CandleDataRequest;
import com.example.demo.records.User;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;

@Service
public class CandleService {
    @Autowired
    private PostService postService;
    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd 00:00");
    public boolean processCandlesForBreakOutFailurePattern(List<List<Object>> ohlcData) {
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
            return true;
        } else {
            return false;
        }

    }
    public ArrayList<Pair<String, List<List<Object>>>> getCandleData(String clientcode, String password, int totp, List<String> symbolTokens){
        String jwtToken = postService.login(new User(clientcode, password, totp)).data().jwtToken();
        ScheduledExecutorService service = Executors.newScheduledThreadPool(3);
        List<ScheduledFuture<Pair<String, List<List<Object>>>>> futures = new ArrayList<>();
        var symbolTokenToCandleResponse = new ArrayList<Pair<String, List<List<Object>>>>();
        int delay =0;
        int requestCounter =0;
        //Max supported Candle is daily frame and 2000 days and rate limit is 3 https://smartapi.angelbroking.com/docs/RateLimit
        for (String symbolToken : symbolTokens) {

            futures.add((ScheduledFuture<Pair<String, List<List<Object>>>>) service.schedule(() -> get(clientcode,password,totp,symbolToken,jwtToken),delay++, TimeUnit.SECONDS));
            // Increment the request counter
            requestCounter++;

            // If 3 requests have been scheduled, reset the delay and wait for the next second
            if (requestCounter % 3 == 0) {
                delay = requestCounter / 3; // Reset delay to wait for the next second
            }
        }
        for (Future<Pair<String, List<List<Object>>>> f : futures) {
            try {
                symbolTokenToCandleResponse.add(f.get());
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        service.close();
        return symbolTokenToCandleResponse;
    }

    private Pair<String, List<List<Object>>> get(String clientcode, String password, int totp, String symbolToken, String jwtToken) {
        //            Thread.sleep(1000);//FIXME : Change this as rate limit is time based even though results are out API seems to throw exception
        CandleApiResponse candleApiResponse = postService.getDailyCandles(jwtToken, new CandleDataRequest("NSE", symbolToken, "ONE_DAY", LocalDate.now().minusDays(2000).format(FORMATTER), LocalDate.now().format(FORMATTER)));
        return transform(symbolToken, candleApiResponse);

    }
    public Pair<String, List<List<Object>>> transform(String symbolToken, CandleApiResponse candleApiResponse) {
        return Pair.of(symbolToken,candleApiResponse.data());
    }
}
