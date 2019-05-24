# Affirm Android SDK Changelog
All notable changes to the SDK are documented in this file.

## Version 2.0.2 (May 23, 2019)
-----------------------
- Add the Order Id to the checkout
- Support `htmlStyling` with AffirmPromotionButton
- request code can be set in the builder in order to prevent conflicts
- Add initialized constructor, only contains publicKey

## Version 2.0.1 (April 29, 2019)
-----------------------
- Added page type
- Added session cookie
- Added method to remove cookies

## Version 2.0.0 (April 19, 2019)
-----------------------

### HTTP Networking
  - Added `AffirmHttpClient` to handle all http requests and responses. Unified error handling.
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
  - All Affirm API requests are in `AffirmApiHandler` now
### Debug logging
```
    public static final int LOG_LEVEL_VERBOSE = Log.VERBOSE;
    public static final int LOG_LEVEL_DEBUG = Log.DEBUG;
    public static final int LOG_LEVEL_INFO = Log.INFO;
    public static final int LOG_LEVEL_WARNING = Log.WARN;
    public static final int LOG_LEVEL_ERROR = Log.ERROR;
    public static final int LOG_LEVEL_NONE = Integer.MAX_VALUE;
```
You can set an appropriate LogLevel for the SDK. Reminder: set this to `LOG_LEVEL_NONE` for the final production build.

### Event tracker
  - Refactor tracker related logic
### SDK configuration and init update
  - `publicKey` and `environment` are required when initializing Affirm.
  - `setMerchantName` is an optional step when initializing Affirm.
  - Added optional `LogLevel` to set log level
  - Example for configuration and init
```
  Affirm.initialize(new Affirm.Configuration.Builder("Y8CQXFF044903JC0", Affirm.Environment.SANDBOX)
                  .setMerchantName(null)
                  .setLogLevel(Affirm.LOG_LEVEL_DEBUG)
                  .build()
          );
```
### Prequalify Flow
  - Added `AffirmPromotionButton` class, you can declare it in an `xml` file or create it directly via `new`. You can then set the `promoId`(optional), `pageType`(optional) and `amount` values with the `configureWithAmount` method.
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
Affirm.configureWithAmount(affirmPromotionButton, null, PromoPageType.PRODUCT, 1100, true);
```

  - Added optional `PrequalCallbacks`, you can catch prequal `failure` through this callback.
    
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

  - Checkout Flow (direct + VCN)
    - Both checkout and VCN checkout now use the unified method `startCheckout`, differentiated by the boolean parameter `useVCN`.
```
    Affirm.startCheckout(this, checkout, false);
```
### Track Order Confirmed
```java
final AffirmTrack affirmTrack = AffirmTrack.builder()
        .setAffirmTrackOrder(affirmTrackOrder)
        .setAffirmTrackProducts(affirmTrackProducts)
        .build();

Affirm.trackOrderConfirmed(MainActivity.this, trackModel());
```
### Exceptions
  - Added custom exceptions, `APIException`, `ConnectionException`, `InvalidRequestException`, `PermissionException` to distinguish between different error types.
### Activity indicator
  - Activity indicator that is automatically added/dismissed to denote loading state when presenting checkout or prequal
### SDK requirement lowered, now is 21
### Added Kotlin example
