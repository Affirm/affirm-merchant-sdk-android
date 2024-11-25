Affirm Android SDK
==================

Affirm Android SDK allows you to offer Affirm in your own app.

# Installation

Download via Gradle:
```groovy
implementation "com.affirm:affirm-android-sdk:2.0.28"
```
or Maven:
```xml
<dependency>
  <groupId>com.affirm</groupId>
  <artifactId>affirm-android-sdk</artifactId>
  <version>2.0.28</version>
</dependency>
```
Snapshots of the development version are available in [Sonatype's `snapshots` repository](https://oss.sonatype.org/content/repositories/snapshots/).

# Usage Overview

Before you can start the initialization of Affirm SDK, you must first set the AffirmSDK with your `public API key` from your sandbox [Merchant Dashboard](https://sandbox.affirm.com/dashboard). You must set this key as follows:

```java
Affirm.initialize(new Affirm.Configuration.Builder("public key")
        .setEnvironment(Affirm.Environment.SANDBOX)
        .setCountryCode(Locale.US.getISO3Country())  // Default USA
        .setLocale(Locale.US.toString())    // Default en_US
        .setName("merchant name")
        .setReceiveReasonCodes("true")
        .setLogLevel(Affirm.LOG_LEVEL_DEBUG)
        .setCheckoutRequestCode(8001)
        .setVcnCheckoutRequestCode(8002)
        .setPrequalRequestCode(8003)
        .build())
```
- `environment` can be set to `Affirm.Environment.SANDBOX` for test.
- To prevent conflicts, you can set a custom affirm's request code.

You can also set `public key` and `merchant name` after the `initialize` method
```java
    Affirm.setMerchantName("merchant name")

    Affirm.setPublicKey("public key")

    Affirm.setPublicKeyAndMerchantName("public key", "merchant name")
```

## Checkout

Checkout creation is the process in which a customer uses Affirm to pay for a purchase in your app. You can create a checkout object and launch the affirm checkout using the Checkout function


```java
final Checkout checkout = Checkout.builder()
        .setOrderId("order id")
        .setItems(items)
        .setBilling(shipping)
        .setShipping(shipping)
        .setShippingAmount(BigDecimal.valueOf(0.0))
        .setTaxAmount(BigDecimal.valueOf(100.0))
        .setTotal(BigDecimal.valueOf(1100.0))
        .setMetadata(metadata)
        .build();

Affirm.startCheckout(this, checkout, false);
//It is recommended that you round the total in the checkout request to two decimal places. Affirm SDK converts the float total to integer cents before initiating the checkout, so may round up or down depending on the decimal places. Ensure that the rounding in your app uses the same calculation across your other backend systems, otherwise, it may cause an error of 1 cent or more in the total validation on your end. 
```

- `checkout` object contains details about the order 
- `useVCN` (boolean) determines whether the checkout flow should use virtual card network to handle the checkout
    - if `true`, it will return `card info` from `VcnCheckoutCallbacks`. Be sure to override onActivityResult, then call the `handleVcnCheckoutData` method.  Please check out the example project for more information.
    ```java
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (Affirm.handleVcnCheckoutData(this, requestCode, resultCode, data)) {
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    ```
    
    ```java
    @Override
    public void onAffirmVcnCheckoutCancelled() {
        Toast.makeText(this, "Vcn Checkout Cancelled", Toast.LENGTH_LONG).show();
    }
    
    @Override
    public void onAffirmVcnCheckoutError(@Nullable String message) {
        Toast.makeText(this, "Vcn Checkout Error: " + message, Toast.LENGTH_LONG).show();
    }
    
    @Override
    public void onAffirmVcnCheckoutSuccess(@NonNull CardDetails cardDetails) {
        Toast.makeText(this, "Vcn Checkout Card: " + cardDetails.toString(), Toast.LENGTH_LONG).show();
    }
    ```
    
    - if `false`, it will return checkout `token` from `CheckoutCallbacks`. Be sure to override onActivityResult, then call the `handleCheckoutData` method.  Please refer to the example project for more information.
    ```java
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (Affirm.handleCheckoutData(this, requestCode, resultCode, data)) {
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    ```

    ```java
    @Override
    public void onAffirmCheckoutSuccess(@NonNull String token) {
        Toast.makeText(this, "Checkout token: " + token, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onAffirmCheckoutCancelled() {
        Toast.makeText(this, "Checkout Cancelled", Toast.LENGTH_LONG).show();
    }
    
    @Override
    public void onAffirmCheckoutError(String message) {
        Toast.makeText(this, "Checkout Error: " + message, Toast.LENGTH_LONG).show();
    }
    ```
### Charge authorization

Once the checkout has been successfully confirmed by the user, the AffirmCheckoutDelegate object will receive a checkout token. This token should be forwarded to your server, which should then use the token to authorize a charge on the user's account. For more details about the server integration, see our [API documentation](https://docs.affirm.com/Integrate_Affirm/Direct_API#3._Authorize_the_charge).

Note - For VCN Checkout, all actions should be done using your existing payment gateway and debit card processor using the virtual card number returned after a successful checkout.

## Promotional Messaging

Affirm promotional messaging components—payment messaging and educational modals—show customers how they can use Affirm to finance their purchases. Promos consist of promotional messaging, which appears directly in your app, and a modal, which which offers users an ability to prequalify.

### Show promotional message with `AffirmPromotionButton`

To display promotional messaging, SDK provides a `AffirmPromotionButton` class. `AffirmPromotionButton` is implemented as follows:

```xml
 <com.affirm.android.AffirmPromotionButton
    android:id="@+id/promo"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_below="@id/price"
    android:layout_centerHorizontal="true"
    android:layout_marginTop="20dp"
    app:htmlStyling="false"
    app:affirmTextFont="@font/apercu_bold"
    app:affirmTextColor="@android:color/darker_gray"
    app:affirmTextSize="16sp"
    app:affirmColor="AffirmColorTypeBlue"
    app:affirmLogoType="AffirmDisplayTypeLogo"/>
```
or
```java
// Option1 - Load via findViewById
AffirmPromotionButton affirmPromotionButton1 = findViewById(R.id.promo);
Affirm.configureWithAmount(affirmPromotionButton1, null, PromoPageType.PRODUCT, BigDecimal.valueOf(1100.0), true);
```
or
```java
// Option2 - Initialize by new
AffirmPromotionButton affirmPromotionButton2 = new AffirmPromotionButton(this);
((FrameLayout)findViewById(R.id.promo_container)).addView(affirmPromotionButton2);
Affirm.configureWithAmount(affirmPromotionButton2, null, PromoPageType.PRODUCT, BigDecimal.valueOf(1100.0), true);
```

Configure the style of the AffirmPromotionButton

- `configWithLocalStyling` that will use the local styles. 
```
// You can custom with the AffirmColor, AffirmLogoType, Typeface, TextSize, TextColor
affirmPromotionButton2.configWithLocalStyling(
                AffirmColor.AFFIRM_COLOR_TYPE_BLUE,
                AffirmLogoType.AFFIRM_DISPLAY_TYPE_LOGO,
                ResourcesCompat.getFont(this, R.font.apercu_bold),
                android.R.color.darker_gray,
                R.dimen.affirm_promotion_size);
```

- `configWithHtmlStyling` will use html style from Affirm server. 

You can add fonts by following the steps below, so you can customize the fonts in html

1. Add a font file in the /res/font/ directory. Such as [lacquer_regular.ttf](/samples-java/src/main/res/font/lacquer_regular.ttf).

2. Add a declaration for the font file. You can check the detail in [typeface](/samples-java/src/main/assets/typeface)

3. Use the font in the css file. You can check the detail in [remote_promo.css](/samples-java/src/main/assets/remote_promo.css).

```
// If you want to custom the style of promo message, should pass the local or remote url and the file of typeface declaration
affirmPromotionButton2.configWithHtmlStyling("file:///android_asset/remote_promo.css", typefaceDeclaration);
```


Tapping on the `AffirmPromotionButton` automatically start prequalification flow.

(Optional) If you want to handle errors, override onActivityResult so that affirm can handle the result.

```java
@Override
protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    if (Affirm.handlePrequalData(this, requestCode, resultCode, data)) {
        return;
    }
    super.onActivityResult(requestCode, resultCode, data);
}
```

```java
@Override
public void onAffirmPrequalError(String message) {
    Toast.makeText(this, "Prequal Error: " + message, Toast.LENGTH_LONG).show();
}
```

### Fetch promotional message, then display it with your own `TextView`.
- You can get promotional message via `fetchPromotion`, a `SpannableString` object is returned after the request is successful
- `onPromotionClick` This method handle events that click on the promotional message
```java
    TextView promotionTextView = findViewById(R.id.promotionTextView);
    Affirm.PromoRequestData requestData = new Affirm.PromoRequestData.Builder(PRICE, true)
        .setPageType(null)
        .build();

    promoRequest = Affirm.fetchPromotion(requestData, promotionTextView.getTextSize(), this, new PromotionCallbackV2() {
        @Override
        public void onSuccess(@NonNull Promotion promotion) {
            promotionTextView.setContentDescription(promotion.getDescription());
            promotionTextView.setText(promotion.getSpannableString());
            promotionTextView.setOnClickListener(v -> Affirm.onPromotionClick(MainActivity.this, requestData, promotion.isShowPrequal()));
        }
   
        @Override
        public void onFailure(@NonNull AffirmException exception) {
                   Toast.makeText(getBaseContext(), "Failed to get promo message, reason: " + exception.toString(), Toast.LENGTH_SHORT).show();
            }
        });
```

- Call `create` method will initiate the request, and call `cancel` method to cancel the request.
```java
    @Override
    protected void onStart() {
        super.onStart();
        promoRequest.create();
    }

    @Override
    protected void onStop() {
        promoRequest.cancel();
        super.onStop();
    }
```

## Track Order Confirmed
The trackOrderConfirmed event triggers when a customer completes their purchase. SDK provides `AffirmTrack` object to trigger the tracking.

```java
final AffirmTrack affirmTrack = AffirmTrack.builder()
        .setAffirmTrackOrder(affirmTrackOrder)
        .setAffirmTrackProducts(affirmTrackProducts)
        .build();

Affirm.trackOrderConfirmed(MainActivity.this, trackModel());
```

## Fragment supports
We also support using fragment directly, only need to pass a ViewGroup id, we will put the `AffirmFragment` in this specified view.

- Checkout
```java
    // In your activity/fragment, you need to implement Affirm.CheckoutCallbacks
    Affirm.startCheckout(this, R.id.container, checkoutModel(), null, 10, false);

    // - Affirm.CheckoutCallbacks
    @Override
    public void onAffirmCheckoutSuccess(@NonNull String token) {
        Toast.makeText(this, "Checkout token: " + token, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onAffirmCheckoutCancelled() {
        Toast.makeText(this, "Checkout Cancelled", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onAffirmCheckoutError(@Nullable String message) {
        Toast.makeText(this, "Checkout Error: " + message, Toast.LENGTH_LONG).show();
    }
```

- VCN checkout
```java
    // In your activity/fragment, you need to implement Affirm.VcnCheckoutCallbacks
    Affirm.startCheckout(this, R.id.container, checkoutModel(), null, 10, true);

    // - Affirm.VcnCheckoutCallbacks
    @Override
    public void onAffirmVcnCheckoutCancelled() {
        Toast.makeText(this, "Vcn Checkout Cancelled", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onAffirmVcnCheckoutCancelledReason(@NonNull VcnReason vcnReason) {
        Toast.makeText(this, "Vcn Checkout Cancelled: " + vcnReason.toString(), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onAffirmVcnCheckoutError(@Nullable String message) {
        Toast.makeText(this, "Vcn Checkout Error: " + message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onAffirmVcnCheckoutSuccess(@NonNull CardDetails cardDetails) {
        Toast.makeText(this, "Vcn Checkout Card: " + cardDetails.toString(), Toast.LENGTH_LONG).show();
    }
```

- Promotion
```java
    AffirmPromotionButton affirmPromotionButton = findViewById(R.id.promo);
    Affirm.configureWithAmount(this, R.id.container, affirmPromotionButton, null, PromoPageType.PRODUCT, PRICE, true, null);
```

- Site modal
```java
    // In your activity/fragment, you need to implement Affirm.PrequalCallbacks
    Affirm.showSiteModal(this, R.id.container, null, "5LNMQ33SEUYHLNUC");

    @Override
    public void onAffirmPrequalError(@Nullable String message) {
        Toast.makeText(this, "Prequal Error: " + message, Toast.LENGTH_LONG).show();
    }
```
- Product modal
```java
    // In your activity/fragment, you need to implement Affirm.PrequalCallbacks
    Affirm.showProductModal(this, R.id.container, PRICE, null, PromoPageType.PRODUCT, null)

    @Override
    public void onAffirmPrequalError(@Nullable String message) {
        Toast.makeText(this, "Prequal Error: " + message, Toast.LENGTH_LONG).show();
    }
```

- Since there is no callback, it will return success after 10 seconds timeout
- We will replace using the HTTP API after the API is done

# Example
1. Copy the content of the `gradle.properties.template` to `affirm/gradle.properties`. This step is optional. There is a step inside `affirm/build.gradle` to do this automatically.
2. Run the `samples-java` or `samples-kotlin` within Android Studio.

# Upgrade (from 1.x.x to 2.0.28)
* If you are using an older version of the SDK, you can refer to this [upgrade document](https://github.com/Affirm/affirm-merchant-sdk-android/blob/master/UPGRADE.md). We recommend that you install the latest version to access the most up-to-date features and experience. 

# Changelog
* All notable changes to this project will be documented in [changelog document](https://github.com/Affirm/affirm-merchant-sdk-android/blob/master/CHANGELOG.md).
