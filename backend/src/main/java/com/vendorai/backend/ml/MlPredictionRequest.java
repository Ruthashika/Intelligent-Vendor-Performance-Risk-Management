package com.vendorai.backend.ml;

public class MlPredictionRequest {

    private String category;
    private String priority;
    private double quantity;
    private double unit_price;
    private double total_amount;
    private double planned_delivery_days;

    private double previous_po_count;
    private double previous_on_time_rate;
    private double previous_average_delay;
    private double previous_fulfillment_rate;
    private double previous_quality_issue_rate;

    private double recent_on_time_rate;
    private double recent_average_delay;
    private double recent_fulfillment_rate;

    public MlPredictionRequest() {
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public double getUnit_price() {
        return unit_price;
    }

    public void setUnit_price(double unit_price) {
        this.unit_price = unit_price;
    }

    public double getTotal_amount() {
        return total_amount;
    }

    public void setTotal_amount(double total_amount) {
        this.total_amount = total_amount;
    }

    public double getPlanned_delivery_days() {
        return planned_delivery_days;
    }

    public void setPlanned_delivery_days(double planned_delivery_days) {
        this.planned_delivery_days = planned_delivery_days;
    }

    public double getPrevious_po_count() {
        return previous_po_count;
    }

    public void setPrevious_po_count(double previous_po_count) {
        this.previous_po_count = previous_po_count;
    }

    public double getPrevious_on_time_rate() {
        return previous_on_time_rate;
    }

    public void setPrevious_on_time_rate(double previous_on_time_rate) {
        this.previous_on_time_rate = previous_on_time_rate;
    }

    public double getPrevious_average_delay() {
        return previous_average_delay;
    }

    public void setPrevious_average_delay(double previous_average_delay) {
        this.previous_average_delay = previous_average_delay;
    }

    public double getPrevious_fulfillment_rate() {
        return previous_fulfillment_rate;
    }

    public void setPrevious_fulfillment_rate(double previous_fulfillment_rate) {
        this.previous_fulfillment_rate = previous_fulfillment_rate;
    }

    public double getPrevious_quality_issue_rate() {
        return previous_quality_issue_rate;
    }

    public void setPrevious_quality_issue_rate(double previous_quality_issue_rate) {
        this.previous_quality_issue_rate = previous_quality_issue_rate;
    }

    public double getRecent_on_time_rate() {
        return recent_on_time_rate;
    }

    public void setRecent_on_time_rate(double recent_on_time_rate) {
        this.recent_on_time_rate = recent_on_time_rate;
    }

    public double getRecent_average_delay() {
        return recent_average_delay;
    }

    public void setRecent_average_delay(double recent_average_delay) {
        this.recent_average_delay = recent_average_delay;
    }

    public double getRecent_fulfillment_rate() {
        return recent_fulfillment_rate;
    }

    public void setRecent_fulfillment_rate(double recent_fulfillment_rate) {
        this.recent_fulfillment_rate = recent_fulfillment_rate;
    }
}