package fr.android.mhealthy.api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {
    @POST("/v1/login")
    void login(@Body LoginRequest req);
}