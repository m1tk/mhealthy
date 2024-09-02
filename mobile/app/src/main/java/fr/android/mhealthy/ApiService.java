package fr.android.mhealthy;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

import java.util.List;

public interface ApiService {
    @GET("/items/")
    Call<List<Item>> getItems();

    @POST("/items/")
    Call<Void> addItem(@Body Item item);
}

