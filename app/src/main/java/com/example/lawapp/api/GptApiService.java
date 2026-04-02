package com.example.lawapp.api;

import com.example.lawapp.models.SmartSearchRequest;
import com.example.lawapp.models.SmartSearchResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface GptApiService {

    @GET("api/health")
    Call<SmartSearchResponse> healthCheck();

    @POST("api/smart-search")
    Call<SmartSearchResponse> smartSearch(@Body SmartSearchRequest request);

    @POST("api/summarize")
    Call<SmartSearchResponse> summarize(@Body SmartSearchRequest request);
}