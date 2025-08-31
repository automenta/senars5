package com.senars.cognitive.system.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class Content {
    @JsonProperty("text")
    private String text;

    @JsonProperty("symbolic")
    private String symbolic;

    @JsonProperty("embedding")
    private List<Double> embedding;

    @JsonProperty("perceptual")
    private Object perceptual;

    @JsonProperty("procedural")
    private Object procedural;

    // Getters and Setters
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getSymbolic() {
        return symbolic;
    }

    public void setSymbolic(String symbolic) {
        this.symbolic = symbolic;
    }

    public List<Double> getEmbedding() {
        return embedding;
    }

    public void setEmbedding(List<Double> embedding) {
        this.embedding = embedding;
    }

    public Object getPerceptual() {
        return perceptual;
    }

    public void setPerceptual(Object perceptual) {
        this.perceptual = perceptual;
    }

    public Object getProcedural() {
        return procedural;
    }

    public void setProcedural(Object procedural) {
        this.procedural = procedural;
    }
}
