package com.example.lawapp.models;

import com.google.gson.annotations.SerializedName;

public class Codek {
    @SerializedName("id")
    public String id;

    @SerializedName("Название")
    public String название;

    @SerializedName("Ссылка")
    public String ссылка;

    @SerializedName("Номер")
    public String номер;

    public Codek() {}

    public String getНазвание() { return название; }
    public String getНомер() { return номер; }
}