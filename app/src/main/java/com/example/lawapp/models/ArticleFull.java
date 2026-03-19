package com.example.lawapp.models;

import com.google.gson.annotations.SerializedName;

public class ArticleFull {
    @SerializedName("id")
    public String id;

    @SerializedName("Название")
    public String название;

    @SerializedName("Ссылка")
    public String ссылка;

    @SerializedName("Номер_источника_статьи")
    public String номерИсточника;

    public ArticleFull() {}

    public String getНазвание() { return название; }
    public String getНомерИсточника() { return номерИсточника; }
}