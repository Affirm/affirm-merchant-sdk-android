Affirm Android SDK
==================

Affirm Android SDK allows you to offer Affirm monthly payments in your own app.

# Installation

Download via Gradle:
```groovy
implementation "com.affirm:affirm-android-sdk:2.0.1"
```
or Maven:
```xml
<dependency>
  <groupId>com.affirm</groupId>
  <artifactId>affirm-android-sdk</artifactId>
  <version>2.0.1</version>
</dependency>
```
Snapshots of the development version are available in [Sonatype's `snapshots` repository](https://oss.sonatype.org/content/repositories/snapshots/).

# Usage Overview

Before you can start the initialization of Affirm SDK, you must first set the AffirmSDK with your `public API key` from your sandbox [Merchant Dashboard](https://sandbox.affirm.com/dashboard). You must set this key as follows:

```java
Affirm.initialize(new Affirm.Configuration.Builder("public key", Affirm.Environment.SANDBOX)
        .setName("merchant name")
        .setLogLevel(Affirm.LOG_LEVEL_DEBUG)
        .build()
```

## Checkout

Checkout creation is the process in which a customer uses Affirm to pay for a purchase in your app. You can create a checkout object and launch the affirm checkout using the Checkout function


```java
final Checkout checkout = Checkout.builder()
        .setItems(items)
        .setBilling(shipping)
        .setShipping(shipping)
        .setShippingAmount(0f)
        .setTaxAmount(100f)
        .setTotal(1100f)
        .build();

Affirm.startCheckout(this, checkout, false);
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

Affirm promotional messaging components—monthly payment messaging and educational modals—show customers how they can use Affirm to finance their purchases. Promos consist of promotional messaging, which appears directly in your app, and a modal, which which offers users an ability to prequalify.

To display promotional messaging, SDK provides a `AffirmPromotionButton` class. `AffirmPromotionButton` is implemented as follows:

```xml
<com.affirm.android.AffirmPromotionButton
     android:id="@+id/promo"
     android:layout_width="wrap_content"
     android:layout_height="wrap_content"
     android:layout_below="@id/price"
     android:layout_centerHorizontal="true"
     android:layout_marginTop="20dp"
     android:textSize="16sp"
     app:htmlStyling="false"
     app:affirmColor="AffirmColorTypeBlue"
     app:affirmLogoType="AffirmDisplayTypeLogo"/>
```
or
```java
// Option1 - Load via findViewById
AffirmPromotionButton affirmPromotionButton1 = findViewById(R.id.promo);
Affirm.configureWithAmount(affirmPromotionButton1, null, PromoPageType.PRODUCT, 1100, true);
```
or
```java
// Option2 - Initialize by new
// - configWithHtmlStyling will use html style from Affirm server
// - configWithLocalStyling that you can set custom styles
AffirmPromotionButton affirmPromotionButton2 = new AffirmPromotionButton(this);
affirmPromotionButton2.configWithHtmlStyling(true);
//affirmPromotionButton2.configWithLocalStyling(AffirmColor.AFFIRM_COLOR_TYPE_BLUE, AffirmLogoType.AFFIRM_DISPLAY_TYPE_LOGO);
((FrameLayout)findViewById(R.id.promo_container)).addView(affirmPromotionButton2);
Affirm.configureWithAmount(affirmPromotionButton2, null, PromoPageType.PRODUCT, 1100, true);
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

## Track Order Confirmed
The trackOrderConfirmed event triggers when a customer completes their purchase. SDK provides `AffirmTrack` object to trigger the tracking.

```java
final AffirmTrack affirmTrack = AffirmTrack.builder()
        .setAffirmTrackOrder(affirmTrackOrder)
        .setAffirmTrackProducts(affirmTrackProducts)
        .build();

Affirm.trackOrderConfirmed(MainActivity.this, trackModel());
```

- Since there is no callback, it will return success after 10 seconds timeout
- We will replace using the HTTP API after the API is done

# Example
1. Copy the content of the `gradle.properties.template` to `affirm/gradle.properties`. This step is optional. There is a step inside `affirm/build.gradle` to do this automatically.
2. Run the `samples-java` or `samples-kotlin` within Android Studio.

# Upgrade (from 1.x.x to 2.0.1)
* If you are using an older version of the SDK, you can refer to this [upgrade document](https://github.com/Affirm/affirm-merchant-sdk-android/blob/master/UPGRADE.md). We recommend that you install the latest version to access the most up-to-date features and experience. 

# Changelog
* All notable changes to this project will be documented in [changelog document](https://github.com/Affirm/affirm-merchant-sdk-android/blob/master/CHANGELOG.md).
