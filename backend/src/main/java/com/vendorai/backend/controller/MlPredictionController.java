package com.vendorai.backend.controller;

import com.vendorai.backend.ml.MlPredictionRequest;
import com.vendorai.backend.ml.MlPredictionResponse;
import com.vendorai.backend.ml.MlPredictionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ml")
@CrossOrigin(origins = "*")
public class MlPredictionController {

    private final MlPredictionService mlPredictionService;

    public MlPredictionController(MlPredictionService mlPredictionService) {
        this.mlPredictionService = mlPredictionService;
    }

    @PostMapping("/predict")
    public ResponseEntity<MlPredictionResponse> predict(
            @RequestBody MlPredictionRequest request) {

        MlPredictionResponse response =
                mlPredictionService.predict(request);

        return ResponseEntity.ok(response);
    }
}