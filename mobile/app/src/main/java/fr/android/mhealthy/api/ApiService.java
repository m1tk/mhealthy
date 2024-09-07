package fr.android.mhealthy.api;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Streaming;

public interface ApiService {
    @POST("v1/login")
    Call<LoginResp> login(@Body LoginReq req);
    @GET("v1/caregiver/assigned_events")
    @Streaming
    Call<ResponseBody> assigned_events();
}