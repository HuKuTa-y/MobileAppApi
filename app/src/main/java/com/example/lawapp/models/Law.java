package com.example.lawapp.models;

import com.google.gson.annotations.SerializedName;

public class Law {
    @SerializedName("id")
    public String id;

    @SerializedName("Название")
    public String название;

    @SerializedName("Ссылка")
    public String ссылка;

    @SerializedName("Номер")
    public String номер;

    public Law() {}

    public String getНазвание() { return название; }
    public String getНомер() { return номер; }
}