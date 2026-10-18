package com.vendorai.backend.ml;

import java.util.Map;

public class MlPredictionResponse {

    private String delivery_risk;
    private double risk_probability;
    private Map<String, Double> probabilities;

    public MlPredictionResponse() {
    }

    public String getDelivery_risk() {
        return delivery_risk;
    }

    public void setDelivery_risk(String delivery_risk) {
        this.delivery_risk = delivery_risk;
    }

    public double getRisk_probability() {
        return risk_probability;
    }

    public void setRisk_probability(double risk_probability) {
        this.risk_probability = risk_probability;
    }

    public Map<String, Double> getProbabilities() {
        return probabilities;
    }

    public void setProbabilities(Map<String, Double> probabilities) {
        this.probabilities = probabilities;
    }
}