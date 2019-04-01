# UPGRADE GUIDE (upgrade from 1.13.0 to 2.0.0)

- Updating Dependencies
  - Replace the latest version in your `build.gradle` file
  - Support min sdk as low as 14
 
- Initialize the SDK
  - Now `publicKey` and `environment` are required when initializing Affirm. Current implement is as follows.
  - You can `setMerchantName` when initializing Affirm, it's optional.
  - Add `LogLevel`, it's optional. You can `setLogLevel` when initializing Affirm in debug mode, that can help debug code. But please ensure this is set to `LOG_LEVEL_ERROR` or `LOG_LEVEL_NONE` before deploying your app.

  Before
  ```
   Affirm.builder()
          .setEnvironment(Affirm.Environment.SANDBOX)
          .setMerchantPublicKey("public key")
          .build();
  ```
  
  Now
  ```
  Affirm.initialize(new Affirm.Configuration.Builder("public key", Affirm.Environment.SANDBOX)
                  .setMerchantName(null)
                  .setLogLevel(Affirm.LOG_LEVEL_DEBUG)
                  .build()
          );
  ```
  
- Promotional Messaging
  - Add `AffirmPromotionButton`, you can declare it in an `xml` file or create it directly via `new`. Then with the `configureWithAmount` method, you can set the `promoId` and `amount` values.
  Before
  ```
  CancellableRequest aslowasPromo = affirm.writePromoToTextView(promo, null, 1100, AffirmDisplayTypeLogo, AffirmColorTypeBlue, true, new PromoCallback() {
        @Override public void onPromoWritten(TextView textView) {
          aslowasPromo = null;
        }
  
        @Override public void onFailure(TextView textView, final Throwable throwable) {
          runOnUiThread(new Runnable() {
            @Override public void run() {
              Toast.makeText(MainActivity.this, "As low as label : " + throwable.getMessage(),
                  Toast.LENGTH_LONG).show();
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
  ```
  AffirmPromotionButton affirmPromotionButton = findViewById(R.id.promo);
  Affirm.configureWithAmount(affirmPromotionButton, null, 1100, true);
  ```
  
  - Add `PrequalCallbacks`(it's optional), you can detect prequal `failure` if you want.

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


- Checkout
  - Both checkout and vcn checkout use the unified method `startCheckout`. Only need to pass in different parameters `useVCN`.
  
  Take vcn checkout as an example, checkout is also similar

  Before
  ```
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
  ```
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