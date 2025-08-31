package com.senars.cognitive.system.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ThoughtType {
    @JsonProperty("BELIEF")
    BELIEF,
    @JsonProperty("GOAL")
    GOAL,
    @JsonProperty("SCHEMA")
    SCHEMA,
    @JsonProperty("ACTION_PLAN")
    ACTION_PLAN,
    @JsonProperty("REPORT")
    REPORT,
    @JsonProperty("QUESTION")
    QUESTION;
}
