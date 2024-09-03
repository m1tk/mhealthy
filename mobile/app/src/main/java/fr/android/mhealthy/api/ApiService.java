package fr.android.mhealthy.api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

import java.util.List;

public interface ApiService {
    @POST("/v1/login")
    void login(@Body LoginRequest req);
}

class LoginRequest {
    public String token;
}