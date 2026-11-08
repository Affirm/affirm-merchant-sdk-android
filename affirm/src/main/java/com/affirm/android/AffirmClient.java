package com.affirm.android;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.affirm.android.exception.APIException;
import com.affirm.android.exception.AffirmException;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static com.affirm.android.AffirmConstants.CONTENT_TYPE;
import static com.affirm.android.AffirmConstants.PROMO_PATH;
import static com.affirm.android.AffirmConstants.TRACKER_PATH;
import static com.affirm.android.AffirmTracker.TrackingEvent.NETWORK_ERROR;
import static com.affirm.android.AffirmTracker.TrackingLevel.ERROR;
import static com.affirm.android.AffirmTracker.createTrackingNetworkJsonObj;

public final class AffirmClient {

    private AffirmClient() {
    }

    public interface AffirmApiRequest {

        @NonNull
        String url();

        @NonNull
        AffirmHttpRequest.Method method();

        @Nullable
        JsonObject body();

        @Nullable
        Map<String, String> headers();
    }

    /**
     * Called on the main thread. Neither method is called once the {@link Call} returned by
     * {@code send} has been cancelled on the main thread.
     */
    public interface AffirmListener<T> {

        void onSuccess(T response);

        void onFailure(AffirmException exception);
    }

    public static <T> Call send(@NonNull AffirmApiRequest request,
                                @NonNull AffirmListener<T> listener) {
        return send(null, request, listener);
    }

    public static <T> Call send(@Nullable OkHttpClient okHttpClient,
                                @NonNull AffirmApiRequest request,
                                @NonNull AffirmListener<T> listener) {
        AffirmHttpRequest.Builder builder = new AffirmHttpRequest.Builder()
                .setUrl(request.url())
                .setMethod(request.method());
        Map<String, String> headers = request.headers();
        if (headers != null && !headers.isEmpty()) {
            builder.setHeaders(headers);
        }
        JsonObject requestBody = request.body();
        if (requestBody != null) {
            builder.setBody(new AffirmHttpBody(CONTENT_TYPE, requestBody.toString()));
        }
        AffirmHttpRequest affirmHttpRequest = builder.build();
        Call call = AffirmPlugins.get().restClient()
                .getCallForRequest(okHttpClient, affirmHttpRequest);
        call.enqueue(new Callback() {
            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                ResponseBody responseBody = response.body();
                Gson gson = AffirmPlugins.get().gson();

                if (response.isSuccessful()) {
                    if (responseBody != null) {
                        try {
                            ParameterizedType type = (ParameterizedType) listener.getClass()
                                    .getGenericInterfaces()[0];
                            Type resolvedType = type.getActualTypeArguments()[0];
                            T model = resolvedType == Void.class
                                    ? null
                                    : gson.fromJson(responseBody.string(), resolvedType);
                            deliverOnMainThread(call, () -> listener.onSuccess(model));
                        } catch (JsonSyntaxException | IOException e) {
                            handleErrorResponse(
                                    call,
                                    new APIException("Some error occurred while parsing response",
                                            e),
                                    listener

                            );
                        }
                    } else {
                        handleErrorResponse(
                                call,
                                new APIException("Response was success, but body was null", null),
                                listener);
                    }
                } else {
                    trackNetworkError(call.request());
                    handleErrorResponse(
                            call,
                            AffirmHttpClient.createExceptionAndTrackFromResponse(
                                    response,
                                    responseBody
                            ),
                            listener);
                }
            }

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                if (call.isCanceled()) {
                    return;
                }
                trackNetworkError(call.request());
                handleErrorResponse(call, e, listener);
            }
        });
        return call;
    }

    // Skipped if the call is cancelled before the main thread runs it, since a response may
    // already be queued when cancel() is called.
    private static void deliverOnMainThread(Call call, Runnable runnable) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (!call.isCanceled()) {
                runnable.run();
            }
        });
    }

    private static void trackNetworkError(Request request) {
        final JsonObject data = networkErrorData(request);
        if (data != null) {
            AffirmTracker.track(NETWORK_ERROR, ERROR, data);
        }
    }

    /** Null for tracker requests, which are never reported. */
    @VisibleForTesting
    @Nullable
    static JsonObject networkErrorData(Request request) {
        if (request.url().toString().contains(TRACKER_PATH)) {
            return null;
        }
        final JsonObject data = createTrackingNetworkJsonObj(request, null);
        if (isPromoRequest(request)) {
            // The promo URL holds the amount and items.
            data.remove("url");
        }
        return data;
    }

    @VisibleForTesting
    static boolean isPromoRequest(Request request) {
        final String promoPathPrefix = PROMO_PATH.substring(0, PROMO_PATH.indexOf("%s"));
        return request.url().encodedPath().contains(promoPathPrefix);
    }

    private static <T> void handleErrorResponse(Call call, Exception e,
                                                AffirmListener<T> listener) {
        deliverOnMainThread(call, () -> listener.onFailure(new APIException(e.getMessage(), e)));
    }
}
