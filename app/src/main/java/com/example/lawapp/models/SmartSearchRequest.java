package com.example.lawapp.models;

import com.google.gson.annotations.SerializedName;

public class SmartSearchRequest {

    @SerializedName("problem")
    private String problem;

    public SmartSearchRequest(String problem) {
        this.problem = problem;
    }

    public String getProblem() {
        return problem;
    }
}