package com.example.demo.services;

import com.example.demo.records.ApiResponse;
import com.example.demo.records.CandleApiResponse;
import com.example.demo.records.CandleDataRequest;
import com.example.demo.records.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class PostService {
    private final RestClient restClient;
    Logger logger = LoggerFactory.getLogger(PostService.class);
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
        logger.info("Sending request for symbol :: {}",candleDataRequest.symboltoken());
        return restClient.post()

                .uri("rest/secure/angelbroking/historical/v1/getCandleData")
                .contentType(MediaType.APPLICATION_JSON)

                .header("Authorization","Bearer "+jwt)
                .body(candleDataRequest)
                .retrieve()
                .body(new ParameterizedTypeReference<>(){});
    }



}
