Affirm Android SDK
==================

Easily integrate Affirm SDK inside merchant Android apps.

# Installation

Download via Gradle:
```groovy
implementation "com.affirm:affirm-android-sdk:latest.version.here"
```
or Maven:
```xml
<dependency>
  <groupId>com.affirm</groupId>
  <artifactId>affirm-android-sdk</artifactId>
  <version>latest.version.here</version>
</dependency>
```
replacing `latest.version.here` with the latest released version

Snapshots of the development version are available in [Sonatype's `snapshots` repository](https://oss.sonatype.org/content/repositories/snapshots/).

# Usage Overview

Before you can start initialize Affirm SDK, you must first set the AffirmSDK with your `public API key` from your [Merchant Dashboard](https://sandbox.affirm.com/dashboard). You must set this key as follows:

```java
Affirm.initialize(new Affirm.Configuration.Builder("public key", Affirm.Environment.SANDBOX)
        .setName("merchant name")
        .setLogLevel(Affirm.LOG_LEVEL_DEBUG)
        .build()
```

## Checkout

When you are ready to checkout with affirm create a checkout object
and launch the affirm checkout.


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

- `checkout` object contains details about the purchase itself
- `useVCN` (boolean) determines whether the checkout flow should use virtual card network.
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
    
    - if `false`, it will return `token` from `CheckoutCallbacks`. Be sure to override onActivityResult, then call the `handleCheckoutData` method.  Please check out the example project for more information.
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

## Promotional Messaging

Affirm Promotional Messaging allows you to inform customers about the availability of installment financing. Promos consist of promotional message label, which appears directly in your app, and a modal, which is opened when the user clicks on the promotional label.

To display promotional messaging, the SDK provides the `AffirmPromotionButton` class. The `AffirmPromotionButton` is implemented as follows:

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
When you are ready to track with affirm create a `AffirmTrack` object and launch the affirm track.
If you want to show indicator when tracking, need to set `showIndicator` to true.

```java
final AffirmTrack affirmTrack = AffirmTrack.builder()
        .setAffirmTrackOrder(affirmTrackOrder)
        .setAffirmTrackProducts(affirmTrackProducts)
        .build();

Affirm.trackOrderConfirmed(MainActivity.this, trackModel(), false, MainActivity.this);

@Override
public void onAffirmTrackSuccess() {
    Toast.makeText(MainActivity.this, "Track Order Confirmed Success", Toast.LENGTH_LONG).show();
}

@Override
public void onAffirmTrackError(@Nullable String message) {
    Toast.makeText(MainActivity.this, "Track Order Confirmed Failed: " + message, Toast.LENGTH_LONG).show();
}
```

- Since there is no callback, will return success after 10 seconds timeout.
- We will replace using the http api after the api is done.

# Example
1. Copy the content of the `gradle.properties.template` to `affirm/gradle.properties`. This step is optional. There is a step inside `affirm/build.gradle` to do this automatically.
2. Run the `samples-java` or `samples-kotlin` within Android Studio

# Upgrade (from 1.x.x to 2.0.0)
* We recommend you to install the latest version for get better features and experience. if you are using the old sdk, you can refer to this [upgrade document](https://github.com/greycats/affirm-android-sdk-rewrite/blob/master/UPGRADE.md).

# Changelog
* All notable changes to this project will be documented in [changelog document](https://github.com/greycats/affirm-android-sdk-rewrite/blob/master/CHANGELOG.md).