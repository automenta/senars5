package com.senars.cognitive.system.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class State {
    @JsonProperty("clarity")
    private double clarity;

    @JsonProperty("salience")
    private double salience;

    @JsonProperty("activation")
    private double activation;

    // Getters and Setters
    public double getClarity() {
        return clarity;
    }

    public void setClarity(double clarity) {
        this.clarity = clarity;
    }

    public double getSalience() {
        return salience;
    }

    public void setSalience(double salience) {
        this.salience = salience;
    }

    public double getActivation() {
        return activation;
    }

    public void setActivation(double activation) {
        this.activation = activation;
    }
}
