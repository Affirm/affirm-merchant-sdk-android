# Affirm Android SDK Changelog
All notable changes to the SDK are documented in this file.

## Version 2.0.30 (March 15, 2024)
- Support UK

## Version 2.0.29 (December 04, 2024)
- Update proguard rules

## Version 2.0.28 (November 25, 2024)
- Remove maximum amount limit

## Version 2.0.27 (September 03, 2024)
- Added categories field to item in checkout request

## Version 2.0.26 (August 14, 2024)
- Bug-fixes

## Version 2.0.25 (May 28, 2024)
- Bug-fixes

## Version 2.0.24 (April 29, 2024)
- Support international integration 

## Version 2.0.23 (March 21, 2024)
- Support description on Accessible mode

## Version 2.0.22 (March 18, 2024)
- Bug-fixes

## Version 2.0.21 (March 06, 2024)
- Bug-fixes

## Version 2.0.20 (January 22, 2022)
- Added logging
- Bug-fixes

## Version 2.0.19 (September 23, 2021)
- Added support for changing public key and merchant name

## Version 2.0.18 (August 23, 2021)
- Updated the Affirm Branding
- Bug-fixes

## Version 2.0.17 (June 29, 2021)
- Start checkout in a Fragment
- Add Client Middleware
- Bug-fixes

## Version 2.0.16 (April 27, 2021)
- Chromium bug fix

## Version 2.0.15 (February 9, 2021)
- Improved critical error handling

## Version 2.0.14 (January 15, 2021)
- Added support for VCN authorization window
- Enhancements and updates to Promotional Messaging
- Bug-fixes

## Version 2.0.13 (September 25, 2020)
- Added support for CaaS identifier in standard and VCN checkout

## Version 2.0.12 (July 21, 2020)
- Improved excpetion handling
- Support for SKU based financing programs

## Version 2.0.11 (April 21, 2020)
- Bug-fix
- Added HTML support to build custom webviews
- Promotional message is called outside of lifecycle
- Metadata is an optional custom object

## Version 2.0.10 (February 17, 2020)
- Bug-fix

## Version 2.0.9 (February 11, 2020)
- Add backend support for VCN

## Version 2.0.8 (January 22, 2020)
- Change promos call to hit base url
- Add metatdata examples
- Remove promo ID from example code
- Fix unnecessary connection exception
- Update logos
- Fix Affirm color type white not working
- Fix drawable colors
- Add support for webhook session IDs
- Add support for Canadian checkouts

## Version 2.0.7 (November 12, 2019)
- Support for Blue and Black logo in text
- Page type for independent modals

## Version 2.0.6 (November 7, 2019)
- Add landscape mode
- Use BigDecimal with currency

## Version 2.0.5 (September 26, 2019)
- Bug Fix for null data exception on back - button
- Convert requests to not use Async Task
- Support type face, text size, and text color for promo messaging
- Support local fonts
- Handle empty ALA messages
- Shipping/Billing consistent with direct API requirements

## Version 2.0.4 (July 12, 2019)
- Add reason codes for VCN checkout cancellation

## Version 2.0.3 (June 20, 2019)
- Updated modal configuration
- Custom CSS support in ALA messaging
- ALA messaging webviews
- ALA messaging logo and color support

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
