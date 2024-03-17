package com.example.demo.controller;

import com.example.demo.records.CandleApiResponse;
import com.example.demo.records.CandleDataRequest;
import com.example.demo.services.CandleService;
import com.example.demo.services.PostService;
import com.example.demo.records.ApiResponse;
import com.example.demo.records.User;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;


@RestController
@RequestMapping("/api")
public class AngelController {
    @Value("${vnalytics.nse.equity.tokens}")
    private List<String> nseTokens;
    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd 00:00");

    @Autowired
    private PostService postService;

    @Autowired
    private CandleService candleService;

    Logger logger = LoggerFactory.getLogger(AngelController.class);

    @PostMapping("login")
    public ApiResponse login(String clientcode, String password, int totp){
        return postService.login(new User(clientcode, password, totp));
    }
    @PostMapping("getCandleData")
    public CandleApiResponse getCandleData(String clientcode, String password, int totp, String symboltoken){
        String jwtToken = postService.login(new User(clientcode, password, totp)).data().jwtToken();
        //Max supported Candle is daily frame and 2000 days
        return postService.getDailyCandles(jwtToken, new CandleDataRequest("NSE", symboltoken, "ONE_DAY", LocalDate.now().minusDays(2000).format(FORMATTER), LocalDate.now().format(FORMATTER)));
    }
    @PostMapping("processCandlesForBreakOutFailurePattern")
    public List<String> processCandlesForBreakOutFailurePattern(String clientcode, String password, int totp ){
        List<String> tokensWithPatternMatch = new ArrayList<>();

        ArrayList<Pair<String, List<List<Object>>>> candleDataList = getCandleData(clientcode,password,totp,nseTokens);
        for(Pair<String, List<List<Object>>> candleData : candleDataList) {
            logger.info("Process symbol {}", candleData.getLeft());
            if (candleData.getRight() != null && candleService.processCandlesForBreakOutFailurePattern(candleData.getRight())) {
                logger.info("Matched for symbol {}",candleData.getLeft());
                tokensWithPatternMatch.add(candleData.getLeft());
            }
        }
            return tokensWithPatternMatch;
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

            futures.add((ScheduledFuture<Pair<String, List<List<Object>>>>) service.schedule(() -> get(clientcode,password,totp,symbolToken,jwtToken),delay++,TimeUnit.SECONDS));
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
//        for (String symbolToken : symbolTokens) {
//            service.scheduleWithFixedDelay(() -> {
//                        System.out.println("VVVVVV");
//                        logger.info("VVVVVVVVVVVVVV");
//
//                    }
//            ,1,1,TimeUnit.SECONDS);
//        }
        service.close();
        return symbolTokenToCandleResponse;
    }

    private Pair<String, List<List<Object>>> get(String clientcode, String password, int totp, String symbolToken, String jwtToken) {
        //            Thread.sleep(1000);//FIXME : Change this as rate limit is time based even though results are out API seems to throw exception
        CandleApiResponse candleApiResponse = postService.getDailyCandles(jwtToken, new CandleDataRequest("NSE", symbolToken, "ONE_DAY", LocalDate.now().minusDays(2000).format(FORMATTER), LocalDate.now().format(FORMATTER)));
        return candleService.transform(symbolToken, candleApiResponse);

    }

}
