package com.example.uos_lms.core.di;

import com.example.uos_lms.BuildConfig;
import com.example.uos_lms.core.data.remote.api.AssignmentApi;
import com.example.uos_lms.core.data.remote.api.AttendanceApi;
import com.example.uos_lms.core.data.remote.api.AuthApi;
import com.example.uos_lms.core.data.remote.api.AuthInterceptor;
import com.example.uos_lms.core.data.remote.api.ContentApi;
import com.example.uos_lms.core.data.remote.api.GradingApi;
import com.example.uos_lms.core.data.remote.api.LeaveApi;
import com.example.uos_lms.core.data.remote.api.TeacherLeaveApi;
import com.example.uos_lms.core.data.remote.api.MaterialApi;
import com.example.uos_lms.core.data.remote.api.MediaApi;
import com.example.uos_lms.core.data.remote.api.MessagingApi;
import com.example.uos_lms.core.data.remote.api.NotificationApi;
import com.example.uos_lms.core.data.remote.api.PublicApi;
import com.example.uos_lms.core.data.remote.api.ServerConfigInterceptor;
import com.example.uos_lms.core.data.remote.api.TeacherAttendanceApi;
import com.example.uos_lms.core.data.remote.api.QuizApi;
import com.example.uos_lms.core.data.remote.api.SchedulingApi;
import com.example.uos_lms.core.data.remote.api.UniversityApi;
import com.example.uos_lms.core.data.remote.api.UsersApi;

import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@Module
@InstallIn(SingletonComponent.class)
public class NetworkModule {

    @Provides
    @Singleton
    public static OkHttpClient provideOkHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(2, TimeUnit.MINUTES)
                .readTimeout(2, TimeUnit.MINUTES)
                .build();
    }

    /** Backend REST API client (Node/Express/MongoDB migration) — separate from the plain
     * OkHttpClient above, which stays dedicated to Cloudinary's direct upload API. */
    @Provides
    @Singleton
    public static Retrofit provideRetrofit(AuthInterceptor authInterceptor, ServerConfigInterceptor serverConfigInterceptor) {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(BuildConfig.DEBUG ? HttpLoggingInterceptor.Level.BODY : HttpLoggingInterceptor.Level.NONE);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(serverConfigInterceptor)
                .addInterceptor(authInterceptor)
                .addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        return new Retrofit.Builder()
                .baseUrl(BuildConfig.API_BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    @Provides
    @Singleton
    public static AuthApi provideAuthApi(Retrofit retrofit) {
        return retrofit.create(AuthApi.class);
    }

    @Provides
    @Singleton
    public static MediaApi provideMediaApi(Retrofit retrofit) {
        return retrofit.create(MediaApi.class);
    }

    @Provides
    @Singleton
    public static UniversityApi provideUniversityApi(Retrofit retrofit) {
        return retrofit.create(UniversityApi.class);
    }

    @Provides
    @Singleton
    public static PublicApi providePublicApi(Retrofit retrofit) {
        return retrofit.create(PublicApi.class);
    }

    @Provides
    @Singleton
    public static UsersApi provideUsersApi(Retrofit retrofit) {
        return retrofit.create(UsersApi.class);
    }

    @Provides
    @Singleton
    public static GradingApi provideGradingApi(Retrofit retrofit) {
        return retrofit.create(GradingApi.class);
    }

    @Provides
    @Singleton
    public static ContentApi provideContentApi(Retrofit retrofit) {
        return retrofit.create(ContentApi.class);
    }

    @Provides
    @Singleton
    public static AttendanceApi provideAttendanceApi(Retrofit retrofit) {
        return retrofit.create(AttendanceApi.class);
    }

    @Provides
    @Singleton
    public static TeacherAttendanceApi provideTeacherAttendanceApi(Retrofit retrofit) {
        return retrofit.create(TeacherAttendanceApi.class);
    }

    @Provides
    @Singleton
    public static AssignmentApi provideAssignmentApi(Retrofit retrofit) {
        return retrofit.create(AssignmentApi.class);
    }

    @Provides
    @Singleton
    public static QuizApi provideQuizApi(Retrofit retrofit) {
        return retrofit.create(QuizApi.class);
    }

    @Provides
    @Singleton
    public static MaterialApi provideMaterialApi(Retrofit retrofit) {
        return retrofit.create(MaterialApi.class);
    }

    @Provides
    @Singleton
    public static LeaveApi provideLeaveApi(Retrofit retrofit) {
        return retrofit.create(LeaveApi.class);
    }

    @Provides
    @Singleton
    public static TeacherLeaveApi provideTeacherLeaveApi(Retrofit retrofit) {
        return retrofit.create(TeacherLeaveApi.class);
    }

    @Provides
    @Singleton
    public static SchedulingApi provideSchedulingApi(Retrofit retrofit) {
        return retrofit.create(SchedulingApi.class);
    }

    @Provides
    @Singleton
    public static MessagingApi provideMessagingApi(Retrofit retrofit) {
        return retrofit.create(MessagingApi.class);
    }

    @Provides
    @Singleton
    public static NotificationApi provideNotificationApi(Retrofit retrofit) {
        return retrofit.create(NotificationApi.class);
    }
}
