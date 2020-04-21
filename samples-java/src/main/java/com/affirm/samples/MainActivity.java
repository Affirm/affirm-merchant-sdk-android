package com.affirm.samples;

import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.affirm.android.Affirm;
import com.affirm.android.AffirmColor;
import com.affirm.android.AffirmLogoType;
import com.affirm.android.AffirmPromotionButton;
import com.affirm.android.AffirmRequest;
import com.affirm.android.CookiesUtil;
import com.affirm.android.HtmlPromotionCallback;
import com.affirm.android.PromotionCallback;
import com.affirm.android.PromotionWebView;
import com.affirm.android.exception.AffirmException;
import com.affirm.android.model.Address;
import com.affirm.android.model.AffirmTrack;
import com.affirm.android.model.AffirmTrackOrder;
import com.affirm.android.model.AffirmTrackProduct;
import com.affirm.android.model.Billing;
import com.affirm.android.model.CardDetails;
import com.affirm.android.model.Checkout;
import com.affirm.android.model.Currency;
import com.affirm.android.model.Item;
import com.affirm.android.model.Name;
import com.affirm.android.model.PromoPageType;
import com.affirm.android.model.Shipping;
import com.affirm.android.model.VcnReason;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

public class MainActivity extends AppCompatActivity implements Affirm.CheckoutCallbacks,
        Affirm.VcnCheckoutCallbacks, Affirm.PrequalCallbacks {

    private static final BigDecimal PRICE = BigDecimal.valueOf(1100.0);
    private AffirmRequest promoRequest;
    private AffirmRequest htmlPromoRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ((TextView) findViewById(R.id.price)).setText("$" + PRICE);

        findViewById(R.id.checkout).setOnClickListener(v -> {
            try {
                Affirm.startCheckout(MainActivity.this, checkoutModel(), false);
            } catch (Exception e) {
                Toast.makeText(getBaseContext(), "Checkout failed, reason: " + e.toString(), Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.vcnCheckout).setOnClickListener(v -> {
            try {
                Affirm.startCheckout(MainActivity.this, checkoutModel(), true);
            } catch (Exception e) {
                Toast.makeText(getBaseContext(), "VCN Checkout failed, reason: " + e.toString(), Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.siteModalButton).setOnClickListener(v -> Affirm.showSiteModal(MainActivity.this, "5LNMQ33SEUYHLNUC"));

        findViewById(R.id.productModalButton).setOnClickListener(v -> Affirm.showProductModal(MainActivity.this, PRICE, null, PromoPageType.PRODUCT, null));

        findViewById(R.id.trackOrderConfirmed).setOnClickListener(v -> {
            Toast.makeText(MainActivity.this, "Track successfully", Toast.LENGTH_SHORT).show();
            Affirm.trackOrderConfirmed(MainActivity.this, trackModel());
        });

        findViewById(R.id.clearCookies).setOnClickListener(v -> CookiesUtil.clearCookies(MainActivity.this));

        // Option1 - Load via findViewById
        AffirmPromotionButton affirmPromotionButton1 = findViewById(R.id.promo);
        // Default use Button to show the promo message with configWithLocalStyling. If you want to use WebView to show the promo message. You should use configWithHtmlStyling
        affirmPromotionButton1.configWithLocalStyling(
                AffirmColor.AFFIRM_COLOR_TYPE_BLUE_BLACK,
                AffirmLogoType.AFFIRM_DISPLAY_TYPE_LOGO,
                ResourcesCompat.getFont(this, R.font.apercu_bold),
                android.R.color.darker_gray,
                R.dimen.affirm_promotion_size);

        Affirm.configureWithAmount(affirmPromotionButton1, PromoPageType.PRODUCT, PRICE, true);

        // Option2 - Initialize by new
        AffirmPromotionButton affirmPromotionButton2 = new AffirmPromotionButton(this);
        // You can use local or remote css file, we suggest you can deploy the css file on the server.
        String typefaceDeclaration = readFileFromAssets("typeface");
        // If you want to use WebView to show the promo message. You should use configWithHtmlStyling
        affirmPromotionButton2.configWithHtmlStyling("file:///android_asset/remote_promo.css", typefaceDeclaration);

        ((FrameLayout) findViewById(R.id.promo_container)).addView(affirmPromotionButton2);
        Affirm.configureWithAmount(affirmPromotionButton2, PRICE, true);


        // Fetch promotion, then use your own TextView to display
        TextView promotionTextView = findViewById(R.id.promotionTextView);
        Affirm.PromoRequestData requestData = new Affirm.PromoRequestData.Builder(PRICE, true)
                .setPageType(null)
                .build();

        promoRequest = Affirm.fetchPromotion(requestData, promotionTextView.getTextSize(), this, new PromotionCallback() {
            @Override
            public void onSuccess(@Nullable SpannableString spannableString, boolean showPrequal) {
                promotionTextView.setText(spannableString);
                promotionTextView.setOnClickListener(v -> Affirm.onPromotionClick(MainActivity.this, requestData, showPrequal));
            }

            @Override
            public void onFailure(@NonNull AffirmException exception) {
                Toast.makeText(getBaseContext(), "Failed to get promo message, reason: " + exception.toString(), Toast.LENGTH_SHORT).show();
            }
        });

        PromotionWebView htmlPromotionWebView = findViewById(R.id.htmlPromotionWebView);
        htmlPromoRequest = Affirm.fetchHtmlPromotion(requestData, new HtmlPromotionCallback() {

            @Override
            public void onSuccess(@Nullable String htmlPromo, boolean showPrequal) {
                htmlPromotionWebView.loadWebData(htmlPromo, "file:///android_asset/remote_promo.css", typefaceDeclaration);
                htmlPromotionWebView.setWebViewClickListener(v -> Affirm.onPromotionClick(MainActivity.this, requestData, showPrequal));
            }

            @Override
            public void onFailure(@NonNull AffirmException exception) {
                Toast.makeText(getBaseContext(), "Failed to get html promo message, reason: " + exception.toString(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        promoRequest.create();
        htmlPromoRequest.create();
    }

    @Override
    protected void onStop() {
        promoRequest.cancel();
        htmlPromoRequest.cancel();
        super.onStop();
    }

    private AffirmTrack trackModel() {
        final AffirmTrackOrder affirmTrackOrder = AffirmTrackOrder.builder()
                .setStoreName("Affirm Store")
                .setCoupon("SUMMER2018")
                .setCurrency(Currency.USD)  // "CAD" for canadian, "USD" for American
                .setDiscount(0)
                .setPaymentMethod("Visa")
                .setRevenue(2920)
                .setShipping(534)
                .setShippingMethod("Fedex")
                .setTax(285)
                .setOrderId("T12345")
                .setTotal(3739)
                .build();

        final AffirmTrackProduct affirmTrackProduct = AffirmTrackProduct.builder()
                .setBrand("Affirm")
                .setCategory("Apparel")
                .setCoupon("SUMMER2018")
                .setName("Affirm T-Shirt")
                .setPrice(730)
                .setProductId("SKU-1234")
                .setQuantity(1)
                .setVariant("Black")
                .build();

        final List<AffirmTrackProduct> affirmTrackProducts = new ArrayList<>();
        affirmTrackProducts.add(affirmTrackProduct);

        return AffirmTrack.builder()
                .setAffirmTrackOrder(affirmTrackOrder)
                .setAffirmTrackProducts(affirmTrackProducts)
                .build();
    }

    private Checkout checkoutModel() {
        final Item item = Item.builder()
                .setDisplayName("Great Deal Wheel")
                .setImageUrl(
                        "http://www.m2motorsportinc.com/media/catalog/product/cache/1/thumbnail" +
                                "/9df78eab33525d08d6e5fb8d27136e95/v/e/velocity-vw125-wheels-rims.jpg")
                .setQty(1)
                .setSku("wheel")
                .setUnitPrice(BigDecimal.valueOf(1000.0))
                .setUrl("http://merchant.com/great_deal_wheel")
                .build();

        final Map<String, Item> items = new HashMap<>();
        items.put("wheel", item);

        final Name name = Name.builder().setFull("John Smith").build();

        //  In US, use Address
        final Address address = Address.builder()
                .setCity("San Francisco")
                .setCountry("USA")
                .setLine1("333 Kansas st")
                .setState("CA")
                .setZipcode("94107")
                .build();

        //  In canadian, use CAAddress
//        final AbstractAddress address = CAAddress.builder()
//                .setStreet1("123 Alder Creek Dr.")
//                .setStreet2("Floor 7")
//                .setCity("Toronto")
//                .setRegion1Code("ON")
//                .setPostalCode("M4B 1B3")
//                .setCountryCode("CA")
//                .build();

        final Shipping shipping = Shipping.builder().setAddress(address).setName(name).build();
        final Billing billing = Billing.builder().setAddress(address).setName(name).build();

        // More details on https://docs.affirm.com/affirm-developers/reference/the-metadata-object
        final Map<String, String> metadata = new HashMap<>();
        metadata.put("webhook_session_id", "ABC123");
        metadata.put("shipping_type", "UPS Ground");
        metadata.put("entity_name", "internal-sub_brand-name");

        return Checkout.builder()
                .setOrderId("55555")
                .setItems(items)
                .setBilling(billing)
                .setShipping(shipping)
                .setShippingAmount(BigDecimal.valueOf(0.0))
                .setTaxAmount(BigDecimal.valueOf(100.0))
                .setTotal(PRICE)
                .setCurrency(Currency.USD) // For Canadian, you must set "CAD"; For American, this is optional, you can set "USD" or not set.
                .setMetadata(metadata)
                .build();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (Affirm.handleCheckoutData(this, requestCode, resultCode, data)) {
            return;
        }

        if (Affirm.handleVcnCheckoutData(this, requestCode, resultCode, data)) {
            return;
        }

        if (Affirm.handlePrequalData(this, requestCode, resultCode, data)) {
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

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
    public void onAffirmCheckoutError(String message) {
        Toast.makeText(this, "Checkout Error: " + message, Toast.LENGTH_LONG).show();
    }

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

    // - Prequal
    @Override
    public void onAffirmPrequalError(@Nullable String message) {
        Toast.makeText(this, "Prequal Error: " + message, Toast.LENGTH_LONG).show();
    }

    private String readFileFromAssets(String fileName) {
        StringBuilder returnString = new StringBuilder();
        InputStream fIn = null;
        InputStreamReader isr = null;
        BufferedReader input = null;
        try {
            fIn = getResources().getAssets().open(fileName);
            isr = new InputStreamReader(fIn);
            input = new BufferedReader(isr);
            String line;
            while ((line = input.readLine()) != null) {
                returnString.append(line);
            }
        } catch (Exception e) {
            e.getMessage();
        } finally {
            try {
                if (isr != null)
                    isr.close();
                if (fIn != null)
                    fIn.close();
                if (input != null)
                    input.close();
            } catch (Exception e2) {
                e2.getMessage();
            }
        }
        return returnString.toString();
    }

}
