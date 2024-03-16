package com.example.demo.controller;

import com.example.demo.records.CandleApiResponse;
import com.example.demo.records.CandleDataRequest;
import com.example.demo.services.CandleService;
import com.example.demo.services.PostService;
import com.example.demo.records.ApiResponse;
import com.example.demo.records.User;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.StructuredTaskScope;


@RestController
@RequestMapping("/api")
public class AngelController {
    @Value("${vnalytics.nse.equity.tokens}")
    private List<String> nseTokens;
    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd 00:00");

    private static final Semaphore POOL = new Semaphore(2); //Set permit limit
    @Autowired
    private PostService postService;

    @Autowired
    private CandleService candleService;
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
        for(Pair<String, List<List<Object>>> candleData : candleDataList)
            if (candleService.processCandlesForBreakOutFailurePattern(candleData.getRight())){
                tokensWithPatternMatch.add(candleData.getLeft());
            }
        return tokensWithPatternMatch;
        }
    public ArrayList<Pair<String, List<List<Object>>>> getCandleData(String clientcode, String password, int totp, List<String> symbolTokens){
        String jwtToken = postService.login(new User(clientcode, password, totp)).data().jwtToken();
        //Max supported Candle is daily frame and 2000 days and rate limit is 3 https://smartapi.angelbroking.com/docs/RateLimit
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            var symbolTokenToCandleResponse = new ArrayList<Pair<String, List<List<Object>>>>();
            for (String symbolToken : symbolTokens) {
                try{
                    POOL.acquire();//Takes a permit and blocks calls if no permit is available
                } catch(InterruptedException e){
                    System.out.println("handle exception if acquiring a permit fails" + e);
                }

                try {
                    scope.fork(() -> {
                        CandleApiResponse candleApiResponse = postService.getDailyCandles(jwtToken, new CandleDataRequest("NSE", symbolToken, "ONE_DAY", LocalDate.now().minusDays(2000).format(FORMATTER), LocalDate.now().format(FORMATTER)));
                        return symbolTokenToCandleResponse.add(candleService.transform(symbolToken,candleApiResponse));
                    });
                } finally {
                    POOL.release();//Releases a permit
                }

            }

            scope.join();
            scope.throwIfFailed();

            return symbolTokenToCandleResponse;
        } catch (ExecutionException | InterruptedException ex) {
            // this should not happen
            throw new IllegalStateException("Error cases should have been handled during page creation!", ex);
        }
    }

}
