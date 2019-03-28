# Affirm Android SDK Changelog
All notable changes to the SDK will be documented in this file.

## Version 2.0.0 (March 29, 2019)
-----------------------

+ Now `publicKey` and `environment` are required when initializing Affirm.
+ You can `setMerchantName` when initializing Affirm, it's optional.
+ Add `LogLevel`, you can `setLogLevel` when initializing Affirm in debug mode, that can help debug code. But please ensure this is set to `LOG_LEVEL_ERROR` or `LOG_LEVEL_NONE`
before deploying your app.
+ Add `AffirmPromotionButton`, you can declare it in an `xml` file or create it directly via `new`. Then with the `configureWithAmount` method, you can set the `promoId` and `amount` values.
+ Add `PrequalCallbacks`, of course this is optional, you can detect prequal `failure` or `cancel`.
+ Both checkout and vcn checkout use the unified method `startCheckout`. Only need to pass in different parameters `useVcn`.
+ Dependency version updates.