Affirm Android SDK
==================

Easily integrate Affirm checkouts on merchant's native apps

## Dependency

Download via Gradle:

```
implementation 'com.affirm:affirm-android-sdk:1.0.12.1'
```

or Maven:
```xml
<dependency>
  <groupId>com.affirm</groupId>
  <artifactId>affirm-android-sdk</artifactId>
  <version>1.0.12.1</version>
</dependency>
```

## Usage Overview
Start by initialize Affirm SDK.

```java
Affirm.initialize(new Affirm.Configuration.Builder("Y8CQXFF044903JC0", Affirm.Environment.SANDBOX)
        .setName(null)
        .setLogLevel(Affirm.LOG_LEVEL_DEBUG)
        .build()
```

### Promo Message & Prequal Flow

```xml
<com.affirm.android.AffirmPromotionLabel
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
AffirmPromotionLabel affirmPromotionLabel = new AffirmPromotionLabel(this);
affirmPromotionLabel.setAffirmColor(AffirmColor.AFFIRM_COLOR_TYPE_BLUE);
affirmPromotionLabel.setAffirmLogoType(AffirmLogoType.AFFIRM_DISPLAY_TYPE_LOGO);
```

```java
Affirm.configureWithAmount((AffirmPromotionLabel) findViewById(R.id.promo), null, 1100, true);
```

(Optional) If you want to handle cancellations and errors, you need to follow the steps below 
Override onActivityResult so that affirm can handle the result.
```java
@Override
protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    if (Affirm.handlePrequalData(this, requestCode, resultCode, data)) {
        return;
    }
    super.onActivityResult(requestCode, resultCode, data);
}
```

Implement Prequal callbacks.
```java
@Override
public void onAffirmPrequalCancelled() {
    Toast.makeText(this, "Prequal Cancelled", Toast.LENGTH_LONG).show();
}

@Override
public void onAffirmPrequalError(String message) {
    Toast.makeText(this, "Prequal Error: " + message, Toast.LENGTH_LONG).show();
}
```

### Checkout Flow
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

Override onActivityResult so that affirm can handle the result.

```java
@Override
protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    if (Affirm.handleCheckoutData(this, requestCode, resultCode, data)) {
        return;
    }
    super.onActivityResult(requestCode, resultCode, data);
}
```

Implement Checkout callbacks.

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

### VCN Checkout Flow
When you are ready to VCN checkout with affirm create a checkout object
and launch the affirm VCN checkout.

```java
final Checkout checkout = Checkout.builder()
        .setItems(items)
        .setBilling(shipping)
        .setShipping(shipping)
        .setShippingAmount(0f)
        .setTaxAmount(100f)
        .setTotal(1100f)
        .build();

Affirm.startCheckout(this, checkout, true);
```

Override onActivityResult so that affirm can handle the result.

```java
@Override
protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    if (Affirm.handleVcnCheckoutData(this, requestCode, resultCode, data)) {
        return;
    }
    super.onActivityResult(requestCode, resultCode, data);
}
```

Implement Checkout callbacks.

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