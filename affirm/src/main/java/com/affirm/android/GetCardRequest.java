package com.affirm.android;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.affirm.android.exception.APIException;
import com.affirm.android.exception.AffirmException;
import com.affirm.android.exception.ConnectionException;
import com.affirm.android.model.CardDetails;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static com.affirm.android.AffirmConstants.GET_CHECKOUT_CARD_PATH;
import static com.affirm.android.AffirmTracker.TrackingEvent.NETWORK_ERROR;
import static com.affirm.android.AffirmTracker.TrackingLevel.ERROR;
import static com.affirm.android.AffirmTracker.createTrackingNetworkJsonObj;

public class GetCardRequest implements AffirmRequest {

    @NonNull
    private final String checkoutId;

    @NonNull
    private final GetCardRequestCallback callback;

    private Call call;

    public GetCardRequest(@NonNull String checkoutId,
                          @NonNull GetCardRequestCallback callback) {
        this.checkoutId = checkoutId;
        this.callback = callback;
    }

    @Override
    public void create() {
        if (call != null) {
            call.cancel();
        }

        call = AffirmPlugins.get().restClient().getCallForRequest(
                new AffirmHttpRequest.Builder()
                        .setUrl(
                                AffirmHttpClient.getProtocol()
                                        + AffirmPlugins.get().baseUrl()
                                        + String.format(GET_CHECKOUT_CARD_PATH, checkoutId)
                        )
                        .setMethod(AffirmHttpRequest.Method.GET)
                        .build()
        );
        call.enqueue(new Callback() {
            @Override
            public void onResponse(
                    @NotNull Call call,
                    @NotNull Response response
            ) {
                ResponseBody responseBody = response.body();
                Gson gson = AffirmPlugins.get().gson();

                if (response.isSuccessful()) {
                    if (responseBody != null) {
                        try {
                            CardDetails cardDetailsResponse = gson.fromJson(
                                    responseBody.string(),
                                    CardDetails.class
                            );

                            new Handler(Looper.getMainLooper()).post(
                                    () -> callback.onGetCardSuccess(cardDetailsResponse)
                            );
                        } catch (JsonSyntaxException | IOException e) {
                            handleErrorResponse(
                                    new APIException("Some error occurred while parsing the "
                                            + "card response", e)
                            );
                        }
                    } else {
                        handleErrorResponse(new APIException("i/o failure", null));
                    }
                } else {
                    AffirmException affirmException =
                            AffirmHttpClient.createExceptionAndTrackFromResponse(
                                    call.request(),
                                    response,
                                    responseBody
                            );

                    handleErrorResponse(affirmException);
                }
            }

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                AffirmTracker.track(
                        NETWORK_ERROR, ERROR,
                        createTrackingNetworkJsonObj(
                                call.request(),
                                null
                        )
                );
                handleErrorResponse(new ConnectionException("i/o failure", e));
            }
        });
    }

    @Override
    public void cancel() {
        if (call != null) {
            call.cancel();
            call = null;
        }
    }

    private void handleErrorResponse(Exception e) {
        new Handler(Looper.getMainLooper()).post(
                () -> callback.onError(new APIException(e.getMessage(), e))
        );
    }
}
