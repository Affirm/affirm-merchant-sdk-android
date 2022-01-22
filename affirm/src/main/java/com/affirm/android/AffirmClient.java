package com.affirm.android;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.affirm.android.exception.APIException;
import com.affirm.android.exception.AffirmException;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static com.affirm.android.AffirmConstants.CONTENT_TYPE;
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
    }

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
                            T model = gson.fromJson(responseBody.string(), resolvedType);
                            new Handler(Looper.getMainLooper()).post(
                                    () -> listener.onSuccess(model)
                            );
                        } catch (JsonSyntaxException | IOException e) {
                            handleErrorResponse(
                                    new APIException("Some error occurred while parsing the "
                                            + "promo response", e), listener
                            );
                        }
                    } else {
                        handleErrorResponse(
                                new APIException("Response was success, but body was null", null),
                                listener);
                    }
                } else {
                    trackNetworkError(call.request());
                    handleErrorResponse(
                            AffirmHttpClient.createExceptionAndTrackFromResponse(
                                    response,
                                    responseBody
                            ),
                            listener);
                }
            }

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                trackNetworkError(call.request());
                handleErrorResponse(e, listener);
            }
        });
        return call;
    }

    private static void trackNetworkError(Request request) {
        if (!request.url().toString().contains(TRACKER_PATH)) {
            AffirmTracker.track(
                    NETWORK_ERROR,
                    ERROR,
                    createTrackingNetworkJsonObj(
                            request,
                            null
                    )
            );
        }
    }

    private static <T> void handleErrorResponse(Exception e, AffirmListener<T> listener) {
        new Handler(Looper.getMainLooper()).post(
                () -> listener.onFailure(new APIException(e.getMessage(), e))
        );
    }
}
