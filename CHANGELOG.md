# Affirm Android SDK Changelog
All notable changes to the SDK will be documented in this file.

## Version 2.0.0 (March 29, 2019)
-----------------------

- Refactor code
  - HTTP Networking
  - Debug logging
  - Event tracker
  - SDK configuration and init
  - Prequalify Flow
  - Checkout Flow (direct + vcn)
  - Exceptional Flows
  - Activity indicator

- Usage Overview
  - Now `publicKey` and `environment` are required when initializing Affirm. Current implement is as follows.
  ```
  Affirm.initialize(new Affirm.Configuration.Builder("public key", Affirm.Environment.SANDBOX).build());
  ```
  - You can `setMerchantName` when initializing Affirm, it's optional.
  - Add `LogLevel`, it's optional. You can `setLogLevel` when initializing Affirm in debug mode, that can help debug code. But please ensure this is set to `LOG_LEVEL_ERROR` or `LOG_LEVEL_NONE` before deploying your app.

- Promotional Messaging
  - Add `AffirmPromotionButton`, you can declare it in an `xml` file or create it directly via `new`. Then with the `configureWithAmount` method, you can set the `promoId` and `amount` values.
  ```
  AffirmPromotionButton affirmPromotionButton = findViewById(R.id.promo);
  Affirm.configureWithAmount(affirmPromotionButton, null, 1100, true);
  ```
  - Add `PrequalCallbacks`, of course this is optional, you can detect prequal `failure` if you want.

- Checkout
  - Both checkout and vcn checkout use the unified method `startCheckout`. Only need to pass in different parameters `useVCN`.

- Others
  - Dependency version updates.