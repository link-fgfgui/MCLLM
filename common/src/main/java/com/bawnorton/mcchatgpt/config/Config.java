package com.bawnorton.mcchatgpt.config;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Config {
    private static Config INSTANCE;

    @Expose
    @SerializedName("model")
    public String model;
    @Expose
    @SerializedName("is_think_model")
    public Boolean isThinkModel;
    @Expose
    @SerializedName("is_show_cost")
    public Boolean isShowCost;
    @Expose
    @SerializedName("temperature")
    public Double temperature;
    @Expose
    @SerializedName("timeout")
    public Integer timeout;
    @Expose
    @SerializedName("context_level")
    public Integer contextLevel;
    @Expose
    @SerializedName("estimated_cost_per_token")
    public Float estimatedCostPerToken;
    @Expose
    @SerializedName("encrypted_token")
    public String token;
    @Expose
    @SerializedName("secret")
    public String secret;
    @Expose
    @SerializedName("baseurl")
    public String baseurl;

    private Config() {
    }

    public static Config getInstance() {
        if (INSTANCE == null) INSTANCE = new Config();
        return INSTANCE;
    }

    public static void update(Config config) {
        INSTANCE = config;
    }
}
