package com.senars.cognitive.system.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum Origin {
    @JsonProperty("PERCEPTION")
    PERCEPTION,
    @JsonProperty("LLM_INFERENCE")
    LLM_INFERENCE,
    @JsonProperty("USER")
    USER,
    @JsonProperty("SYSTEM")
    SYSTEM;
}
