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
    @POST("v1/caregiver/events")
    @Streaming
    Call<ResponseBody> caregiver_events(@Body CaregiverEventsReq req);
    @POST("v1/caregiver/instruction")
    Call<ResponseBody> instruction(@Body InstructionReq req);

    @POST("v1/patient/events")
    @Streaming
    Call<ResponseBody> patient_events(@Body PatientEventReq req);
    @POST("v1/patient/info")
    Call<ResponseBody> patient_info(@Body PatientInfoReq req);
}