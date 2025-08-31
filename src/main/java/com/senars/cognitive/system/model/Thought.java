package com.senars.cognitive.system.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Thought {
    @JsonProperty("id")
    private String id;

    @JsonProperty("content")
    private Content content;

    @JsonProperty("state")
    private State state;

    @JsonProperty("metadata")
    private Metadata metadata;

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Content getContent() {
        return content;
    }

    public void setContent(Content content) {
        this.content = content;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }
}
