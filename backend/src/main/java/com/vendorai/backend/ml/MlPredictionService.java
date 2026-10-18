package com.vendorai.backend.ml;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class MlPredictionService {

    private final RestClient restClient;

    public MlPredictionService() {
        this.restClient = RestClient.builder()
                .baseUrl("http://127.0.0.1:8000")
                .build();
    }

    public MlPredictionResponse predict(MlPredictionRequest request) {

        return restClient.post()
                .uri("/predict")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(MlPredictionResponse.class);
    }
}