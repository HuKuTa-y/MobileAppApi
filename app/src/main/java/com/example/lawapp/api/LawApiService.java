package com.example.lawapp.api;

import com.example.lawapp.models.*;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.*;

public interface LawApiService {
    @GET("/api/codeks")
    Call<List<Codek>> getCodeks();

    @GET("/api/laws")
    Call<List<Law>> getLaws();

    @GET("/api/articles_full")
    Call<List<ArticleFull>> getArticlesFull();

    @GET("/api/articles/by-source")
    Call<List<ArticleFull>> getArticlesBySource(@Query("source_number") String sourceNumber);

    @GET("/api/article/text")
    Call<TextArticle> getArticleText(@Query("article_name") String articleName);

    @GET("/api/search/by-number")
    Call<List<ArticleFull>> searchByNumber(@Query("number") int number);

    @GET("/api/search/by-text")
    Call<List<ArticleFull>> searchByText(@Query("query") String query);

    @GET("/health")
    Call<Void> healthCheck();
}