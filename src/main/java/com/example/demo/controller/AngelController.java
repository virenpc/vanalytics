package com.example.demo.controller;

import com.example.demo.records.CandleApiResponse;
import com.example.demo.services.PostService;
import com.example.demo.records.ApiResponse;
import com.example.demo.records.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AngelController {

    @Autowired
    private PostService postService;
    @PostMapping("login")
    public ApiResponse login(String clientcode, String password, int totp){
        return postService.login(new User(clientcode, password, totp));
    }
    @PostMapping("getCandleData")
    public CandleApiResponse getCandleData(String clientcode, String password, int totp){
        String jwtToken = postService.login(new User(clientcode, password, totp)).data().jwtToken();
        return postService.getDailyCandles(jwtToken);
    }

}
