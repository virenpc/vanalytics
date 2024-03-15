package com.example.demo.controller;

import com.example.demo.records.CandleApiResponse;
import com.example.demo.records.CandleDataRequest;
import com.example.demo.services.CandleService;
import com.example.demo.services.PostService;
import com.example.demo.records.ApiResponse;
import com.example.demo.records.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

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
        CandleApiResponse candleApiResponse;
        List<String> tokensWithPatternMatch = new ArrayList<>();
        for (String nseToken:nseTokens)
        {
            candleApiResponse = getCandleData(clientcode,password,totp,nseToken);
            if (candleService.processCandlesForBreakOutFailurePattern2(candleApiResponse.data())){
                tokensWithPatternMatch.add(nseToken);
            }
        }

        return tokensWithPatternMatch;
    }
}
