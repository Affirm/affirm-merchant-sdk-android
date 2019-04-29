UPGRADE GUIDE (upgrade from v1 to v2)
==================

These are the steps to upgrade from SDK v1 to v2


## Updating Dependencies
- In your `build.gradle` file, update the version number of Affirm SDK to the latest version (2.x.x)
- Sync gradle

## Make code changes against the new SDK API
- Initialize the SDK
  - `publicKey` and `environment` are required.
  - `setMerchantName` is an optional step.
  - Added optional `LogLevel`
  
  Before:
  ```java
   Affirm.builder()
          .setEnvironment(Affirm.Environment.SANDBOX)
          .setMerchantPublicKey("public key")
          .build();
  ```
  
  Now:
  ```java
  Affirm.initialize(new Affirm.Configuration.Builder("public key", Affirm.Environment.SANDBOX)
                  .setMerchantName(null)
                  .setLogLevel(Affirm.LOG_LEVEL_DEBUG)
                  .build()
          );
  ```
  
- Promotional Messaging
  - Added `AffirmPromotionButton` class, you can declare it in an `xml` file or create it directly via `new`. You can then set the `promoId`(optional), `pageType`(optional) and `amount` values with the `configureWithAmount` method. 
  
  Before
  ```java
  CancellableRequest aslowasPromo = affirm.writePromoToTextView(promo, null, 1100, AffirmDisplayTypeLogo, AffirmColorTypeBlue, true, new PromoCallback() {
        @Override public void onPromoWritten(TextView textView) {
          aslowasPromo = null;
        }
  
        @Override public void onFailure(TextView textView, final Throwable throwable) {
          runOnUiThread(new Runnable() {
            @Override public void run() {
              Toast.makeText(MainActivity.this, "As low as label : " + throwable.getMessage(),
                  Toast.LENGTH_LONG).shsysow();
              aslowasPromo = null;
            }
          });
        }
      });
 
   @Override protected void onPause() {
      super.onPause();
      if (aslowasPromo != null) {
          aslowasPromo.cancelRequest();
      }
   }
  ```
  
  Now
  ```java
  AffirmPromotionButton affirmPromotionButton = findViewById(R.id.promo);
  Affirm.configureWithAmount(affirmPromotionButton, null, PromoPageType.PRODUCT, 1100, true);
  ```
  
  - Added optional `PrequalCallbacks`, you can catch prequal `failure` through this callback.

  ```java
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


- Checkout
  - Both checkout and VCN checkout now use the unified method `startCheckout`, differentiated by the boolean parameter `useVCN`.
  
  Take vcn checkout as an example, checkout is also similar

  Before
  ```java
  final Checkout checkout = Checkout.builder()
          .setItems(items)
          .setBilling(shipping)
          .setShipping(shipping)
          .setShippingAmount(0f)
          .setTaxAmount(100f)
          .setTotal(1100f)
          .build();
  
  affirm.launchVcnCheckout(this, checkout);
  
  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (affirm.onVcnCheckoutActivityResult(this, requestCode, resultCode, data)) {
      return;
    }
    super.onActivityResult(requestCode, resultCode, data);
  }
  ```
  
  Now
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
  
  @Override
  protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (Affirm.handleVcnCheckoutData(this, requestCode, resultCode, data)) {
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
  }
  ```
- Track Order Confirmed
  ```java
  final AffirmTrack affirmTrack = AffirmTrack.builder()
      .setAffirmTrackOrder(affirmTrackOrder)
      .setAffirmTrackProducts(affirmTrackProducts)
      .build();

  Affirm.trackOrderConfirmed(MainActivity.this, trackModel());
  ```
  
## Rebuild and ship
Rebuild you project, if there is no compile error and after testing everything is working as you expected. **Congratulations!** you can ship it now :)
