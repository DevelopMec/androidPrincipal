package com.biometricscan.principal.utils;

import com.biometricscan.principal.models.LoginResponse;
import com.google.gson.JsonObject;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface APIService {

    @FormUrlEncoded
    @POST("services/oauth2/token")
    Call<LoginResponse> getToken(@Field("grant_type") String grantType, @Field("client_id") String clientId, @Field("client_secret") String clientSecret, @Field("username") String username, @Field("password") String password);

    @FormUrlEncoded
    @POST("services/oauth2/token")
    Call<LoginResponse> login(@Field("client_id") String clientId, @Field("grant_type") String grantType, @Field("refresh_token") String refreshToken);

    @POST("FingerprintSF/")
    Call<ResponseBody> sendFingerprinSF (@Body JsonObject json);

}
