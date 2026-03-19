package com.example.lawapp.models;

import com.google.gson.annotations.SerializedName;

public class TextArticle {
    @SerializedName("Название")
    public String название;

    @SerializedName("Контент")
    public String контент;

    public TextArticle() {}

    public String getКонтент() { return контент; }
}