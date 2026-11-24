package com.affirm.android;

import com.affirm.android.exception.APIException;
import com.affirm.android.exception.AffirmException;
import com.affirm.android.exception.InvalidRequestException;
import com.affirm.android.model.AffirmError;
import com.google.common.truth.Truth;
import com.google.gson.JsonParser;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;

@RunWith(RobolectricTestRunner.class)
public class AffirmHttpClientErrorTest {

    @Before
    public void setup() {
        if (AffirmPlugins.get() == null) {
            Affirm.initialize(new Affirm.Configuration.Builder("Y8CQXFF044903JC0", Affirm.Environment.SANDBOX).build());
        }
    }

    @Test
    public void testCheckoutBffError() {
        AffirmException exception = exceptionFor(400,
                "{\"error\":{\"errorCode\":\"INVALID_SUBTOTAL\",\"message\":\"Subtotal is invalid\"}}");

        Truth.assertThat(exception).isInstanceOf(InvalidRequestException.class);
        AffirmError error = exception.getAffirmError();
        Truth.assertThat(error.code()).isEqualTo("INVALID_SUBTOTAL");
        Truth.assertThat(error.message()).isEqualTo("Subtotal is invalid");
    }

    @Test
    public void testCheckoutBffErrorWithoutMessage() {
        AffirmException exception = exceptionFor(400,
                "{\"error\":{\"errorCode\":\"INVALID_SUBTOTAL\"}}");

        AffirmError error = exception.getAffirmError();
        Truth.assertThat(error.code()).isEqualTo("INVALID_SUBTOTAL");
        Truth.assertThat(error.message()).isNull();
    }

    @Test
    public void testCheckoutBffServerError() {
        AffirmException exception = exceptionFor(500,
                "{\"error\":{\"errorCode\":\"INTERNAL_SERVER_ERROR\"}}");

        Truth.assertThat(exception).isInstanceOf(APIException.class);
        Truth.assertThat(exception.getAffirmError().code()).isEqualTo("INTERNAL_SERVER_ERROR");
        Truth.assertThat(exception.toString()).contains("INTERNAL_SERVER_ERROR");
    }

    @Test
    public void testLegacyErrorIsUnchanged() {
        AffirmException exception = exceptionFor(400,
                "{\"message\":\"Invalid field\",\"code\":\"invalid_field\",\"type\":\"invalid_request\",\"field\":\"total\",\"status_code\":400}");

        Truth.assertThat(exception).isInstanceOf(InvalidRequestException.class);
        AffirmError error = exception.getAffirmError();
        Truth.assertThat(error.message()).isEqualTo("Invalid field");
        Truth.assertThat(error.code()).isEqualTo("invalid_field");
        Truth.assertThat(error.type()).isEqualTo("invalid_request");
        Truth.assertThat(error.field()).isEqualTo("total");
        Truth.assertThat(error.status()).isEqualTo(400);
    }

    @Test
    public void testNonObjectErrorFieldIsUnchanged() {
        String body = "{\"error\":\"Something went wrong\",\"message\":\"Bad request\"}";
        Truth.assertThat(AffirmHttpClient.normalizeErrorBody(body))
                .isEqualTo(new JsonParser().parse(body));
    }

    @Test
    public void testErrorObjectWithoutErrorCodeIsUnchanged() {
        String body = "{\"error\":{\"message\":\"Nested\"},\"message\":\"Bad request\",\"status_code\":400}";
        Truth.assertThat(AffirmHttpClient.normalizeErrorBody(body))
                .isEqualTo(new JsonParser().parse(body));
    }

    @Test
    public void testCheckoutBffErrorKeepsTopLevelFields() {
        AffirmException exception = exceptionFor(400,
                "{\"error\":{\"errorCode\":\"INVALID_SUBTOTAL\",\"message\":\"Nested\"},\"status_code\":400}");

        AffirmError error = exception.getAffirmError();
        Truth.assertThat(error.code()).isEqualTo("INVALID_SUBTOTAL");
        Truth.assertThat(error.message()).isEqualTo("Nested");
        Truth.assertThat(error.status()).isEqualTo(400);
    }

    @Test
    public void testNonJsonError() {
        AffirmException exception = exceptionFor(500, "<html>Internal Server Error</html>");

        Truth.assertThat(exception).isInstanceOf(APIException.class);
        Truth.assertThat(exception.getMessage())
                .isEqualTo("Some error occurred while parsing the error response");
    }

    @Test
    public void testErrorWithUnknownLength() {
        ResponseBody responseBody = ResponseBody.create(
                new Buffer().writeUtf8("{\"error\":{\"errorCode\":\"INVALID_SUBTOTAL\"}}"),
                MediaType.get("application/json"), -1);
        Truth.assertThat(responseBody.contentLength()).isEqualTo(-1);

        AffirmException exception = exceptionFor(400, responseBody);

        Truth.assertThat(exception.getAffirmError().code()).isEqualTo("INVALID_SUBTOTAL");
    }

    @Test
    public void testBlankError() {
        for (String body : new String[]{"", "  ", "null"}) {
            AffirmException exception = exceptionFor(500, body);

            Truth.assertThat(exception).isInstanceOf(APIException.class);
            Truth.assertThat(exception.getMessage())
                    .isEqualTo("Error getting exception from response");
        }
    }

    @Test
    public void testDeeplyNestedError() {
        StringBuilder body = new StringBuilder("{\"x\":");
        for (int i = 0; i < 100000; i++) {
            body.append('[');
        }
        for (int i = 0; i < 100000; i++) {
            body.append(']');
        }
        body.append('}');

        AffirmException exception = exceptionFor(500, body.toString());

        Truth.assertThat(exception).isInstanceOf(APIException.class);
        Truth.assertThat(exception.getMessage())
                .isEqualTo("Some error occurred while parsing the error response");
    }

    private static AffirmException exceptionFor(int code, String body) {
        return exceptionFor(code, ResponseBody.create(body, MediaType.get("application/json")));
    }

    private static AffirmException exceptionFor(int code, ResponseBody responseBody) {
        Response response = new Response.Builder()
                .request(new Request.Builder().url("https://api.global-sandbox.affirm.com/").build())
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message("Error")
                .body(responseBody)
                .build();
        return AffirmHttpClient.createExceptionAndTrackFromResponse(response, responseBody);
    }
}
