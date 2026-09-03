package com.example.uos_lms.core.data.remote.api;

import androidx.annotation.NonNull;

import com.example.uos_lms.core.session.ServerConfig;

import java.io.IOException;

import javax.inject.Inject;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/** Rewrites every request's scheme/host/port to {@link ServerConfig}'s override (if one is set)
 * before it leaves the device, so changing the backend's LAN IP only needs the Login screen's
 * long-press-logo dialog - no rebuild, since Retrofit's compiled-in BuildConfig.API_BASE_URL
 * can't otherwise change after the app is built. */
public class ServerConfigInterceptor implements Interceptor {

    private final ServerConfig serverConfig;

    @Inject
    public ServerConfigInterceptor(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request original = chain.request();
        String override = serverConfig.getOverrideHostPort();
        if (override == null) {
            return chain.proceed(original);
        }
        HttpUrl overrideUrl = HttpUrl.parse("http://" + override + "/");
        if (overrideUrl == null) {
            return chain.proceed(original);
        }
        HttpUrl newUrl = original.url().newBuilder()
                .scheme(overrideUrl.scheme())
                .host(overrideUrl.host())
                .port(overrideUrl.port())
                .build();
        return chain.proceed(original.newBuilder().url(newUrl).build());
    }
}
