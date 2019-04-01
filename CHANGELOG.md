# Affirm Android SDK Changelog
All notable changes to the SDK will be documented in this file.

## Version 2.0.0 (March 29, 2019)
-----------------------

### HTTP Networking
  - Add `AffirmHttpClient`, used to handle all http requests and responses, unify to handle all errors.
    ```
    AffirmHttpResponse execute(final AffirmHttpRequest request, boolean sendTrackEvent)
            throws APIException, PermissionException, InvalidRequestException, ConnectionException {
        Request okHttpRequest = getRequest(request);
        Call call = mOkHttpClient.newCall(okHttpRequest);
        try {
            Response response = call.execute();

            boolean responseSuccess = response.isSuccessful();
            if (!responseSuccess && sendTrackEvent) {
                AffirmTracker.track(NETWORK_ERROR, ERROR,
                        createTrackingNetworkJsonObj(okHttpRequest, response));
            }

            final Headers headers = response.headers();
            String requestId = headers.get(X_AFFIRM_REQUEST_ID);
            if (response.code() < 200 || response.code() >= 300) {
                ResponseBody responseBody = response.body();
                final AffirmError affirmError = AffirmPlugins.get().gson()
                        .fromJson(responseBody != null
                                ? responseBody.string() : "", AffirmError.class);
                handleAPIError(affirmError, response.code(), requestId);
            }

            return getResponse(response);

        } catch (IOException e) {
            if (sendTrackEvent) {
                AffirmTracker.track(NETWORK_ERROR, ERROR,
                        createTrackingNetworkJsonObj(okHttpRequest, null));
            }

            throw new ConnectionException("i/o failure", e);
        }
    }
    ```
  - Encapsulates all api requests in `AffirmApiHandler`
### Debug logging
```
    public static final int LOG_LEVEL_VERBOSE = Log.VERBOSE;
    public static final int LOG_LEVEL_DEBUG = Log.DEBUG;
    public static final int LOG_LEVEL_INFO = Log.INFO;
    public static final int LOG_LEVEL_WARNING = Log.WARN;
    public static final int LOG_LEVEL_ERROR = Log.ERROR;
    public static final int LOG_LEVEL_NONE = Integer.MAX_VALUE;
```
You can set a different LogLevel so that you can see more SDK logs. But please ensure this is set to `LOG_LEVEL_ERROR` or `LOG_LEVEL_NONE` before deploying your app.

### Event tracker
  - Refactor tracker related logic
### SDK configuration and init
  - Now `publicKey` and `environment` are required when initializing Affirm. Current implement is as follows.
  - You can `setMerchantName` when initializing Affirm, it's optional.
  - Add `LogLevel`, it's optional. You can `setLogLevel` when initializing Affirm in debug mode, that can help debug code. But please ensure this is set to `LOG_LEVEL_ERROR` or `LOG_LEVEL_NONE` before deploying your app.
```
  Affirm.initialize(new Affirm.Configuration.Builder("Y8CQXFF044903JC0", Affirm.Environment.SANDBOX)
                  .setMerchantName(null)
                  .setLogLevel(Affirm.LOG_LEVEL_DEBUG)
                  .build()
          );
```
### Prequalify Flow
  - Add `AffirmPromotionButton`, you can declare it in an `xml` file or create it directly via `new`. Then with the `configureWithAmount` method, you can set the `promoId` and `amount` values.
```xml
<com.affirm.android.AffirmPromotionButton
     android:id="@+id/promo"
     android:layout_width="wrap_content"
     android:layout_height="wrap_content"
     android:layout_below="@id/price"
     android:layout_centerHorizontal="true"
     android:layout_marginTop="20dp"
     android:textSize="16sp"
     app:affirmColor="AffirmColorTypeBlue"
     app:affirmLogoType="AffirmDisplayTypeLogo"/>
```
or
```java
AffirmPromotionButton affirmPromotionButton = new AffirmPromotionButton(this);
AffirmPromotionButton.setAffirmColor(AffirmColor.AFFIRM_COLOR_TYPE_BLUE);
AffirmPromotionButton.setAffirmLogoType(AffirmLogoType.AFFIRM_DISPLAY_TYPE_LOGO);
```

```java
Affirm.configureWithAmount(affirmPromotionButton, null, 1100, true);
```

  - Add `PrequalCallbacks`, of course this is optional, you can detect prequal `failure` if you want.
    
```
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (Affirm.handlePrequalData(this, requestCode, resultCode, data)) {
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    
    @Override
    public void onAffirmPrequalError(String message) {
        Toast.makeText(this, "Prequal Error: " + message, Toast.LENGTH_LONG).show();
    }
```

  - Checkout Flow (direct + vcn)
    - Both checkout and vcn checkout use the unified method `startCheckout`. Only need to pass in different parameters `useVCN`.
```
    Affirm.startCheckout(this, checkout, false);
```
### Exceptional Flows
  - Add some custom exceptions, `APIException`, `ConnectionException`, `InvalidRequestException`, `PermissionException` to distinguish between different error types.
### Activity indicator
  - Package indicator to use
### SDK requirement lowered, now is 14
