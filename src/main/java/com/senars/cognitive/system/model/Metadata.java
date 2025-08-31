package com.senars.cognitive.system.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Date;
import java.util.List;

public class Metadata {
    @JsonProperty("type")
    private ThoughtType type;

    @JsonProperty("origin")
    private Origin origin;

    @JsonProperty("trace")
    private List<String> trace;

    @JsonProperty("timestamp")
    private Date timestamp;

    // Getters and Setters
    public ThoughtType getType() {
        return type;
    }

    public void setType(ThoughtType type) {
        this.type = type;
    }

    public Origin getOrigin() {
        return origin;
    }

    public void setOrigin(Origin origin) {
        this.origin = origin;
    }

    public List<String> getTrace() {
        return trace;
    }

    public void setTrace(List<String> trace) {
        this.trace = trace;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}
