package com.example.uos_lms.core.data.remote.api;

import com.example.uos_lms.core.data.remote.api.dto.MediaEnvelopeDto;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface MediaApi {

    /** relatedType/relatedId are optional (Retrofit silently omits a null @Part) - Cloudinary
     * folder organization only, the backend falls back to grouping by uploader if absent. */
    @Multipart
    @POST("media/upload")
    Call<MediaEnvelopeDto> upload(@Part("category") RequestBody category,
                                   @Part("relatedType") RequestBody relatedType,
                                   @Part("relatedId") RequestBody relatedId,
                                   @Part MultipartBody.Part file);

    @DELETE("media/profile-photo")
    Call<Void> deleteMyProfilePhoto();

    @DELETE("media/{id}")
    Call<Void> delete(@Path("id") String id);
}
