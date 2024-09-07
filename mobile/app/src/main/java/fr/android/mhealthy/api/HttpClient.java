package fr.android.mhealthy.api;

import android.content.Context;

import net.gotev.cookiestore.SharedPreferencesCookieStore;
import net.gotev.cookiestore.okhttp.JavaNetCookieJar;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import fr.android.mhealthy.BuildConfig;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class HttpClient {
    private static ApiService client = null;
    private static CookieManager cookieJar = null;

    public static void init_cookie_jar(Context context) {
        cookieJar = new CookieManager(
                new SharedPreferencesCookieStore(context, "session_cookies"),
                CookiePolicy.ACCEPT_ALL
        );
    }

    public static ApiService getClient() {
        if (client == null) {
            OkHttpClient ok = new OkHttpClient()
                .newBuilder()
                .cookieJar(new JavaNetCookieJar(cookieJar))
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .protocols(Arrays.asList(Protocol.HTTP_2, Protocol.HTTP_1_1))
                .build();

            client = new Retrofit.Builder()
                    .client(ok)
                    .baseUrl(BuildConfig.SERVER_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(ApiService.class);
        }
        return client;
    }
}

