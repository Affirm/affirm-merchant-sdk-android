package com.affirm.samples;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.affirm.android.Affirm;
import com.affirm.android.AffirmColor;
import com.affirm.android.AffirmLogoType;
import com.affirm.android.AffirmPromotionButton;
import com.affirm.android.CookiesUtil;
import com.affirm.android.model.Address;
import com.affirm.android.model.AffirmTrack;
import com.affirm.android.model.AffirmTrackOrder;
import com.affirm.android.model.AffirmTrackProduct;
import com.affirm.android.model.CardDetails;
import com.affirm.android.model.Checkout;
import com.affirm.android.model.Item;
import com.affirm.android.model.Name;
import com.affirm.android.model.PromoPageType;
import com.affirm.android.model.Shipping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements Affirm.CheckoutCallbacks,
        Affirm.VcnCheckoutCallbacks, Affirm.PrequalCallbacks {

    private static final float PRICE = 1100f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ((TextView) findViewById(R.id.price)).setText("$" + PRICE);

        findViewById(R.id.checkout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Affirm.startCheckout(MainActivity.this, checkoutModel(), false);
            }
        });

        findViewById(R.id.vcnCheckout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Affirm.startCheckout(MainActivity.this, checkoutModel(), true);
            }
        });

        findViewById(R.id.siteModalButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Affirm.showSiteModal(MainActivity.this, "5LNMQ33SEUYHLNUC");
            }
        });

        findViewById(R.id.productModalButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Affirm.showProductModal(MainActivity.this, PRICE, "0Q97G0Z4Y4TLGHGB");
            }
        });

        findViewById(R.id.trackOrderConfirmed).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Track successfully", Toast.LENGTH_SHORT).show();
                Affirm.trackOrderConfirmed(MainActivity.this, trackModel());
            }
        });

        findViewById(R.id.clearCookies).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CookiesUtil.clearCookies(MainActivity.this);
            }
        });

        // Option1 - Load via findViewById
        AffirmPromotionButton affirmPromotionButton1 = findViewById(R.id.promo);
        Affirm.configureWithAmount(affirmPromotionButton1, 1100, true);

        // Option2 - Initialize by new
        AffirmPromotionButton affirmPromotionButton2 = new AffirmPromotionButton(this);
        affirmPromotionButton2.configWithHtmlStyling(true);
        affirmPromotionButton2.configWithLocalStyling(AffirmColor.AFFIRM_COLOR_TYPE_BLUE, AffirmLogoType.AFFIRM_DISPLAY_TYPE_LOGO);

        ((FrameLayout)findViewById(R.id.promo_container)).addView(affirmPromotionButton2);
        Affirm.configureWithAmount(affirmPromotionButton2, 1100, true);
    }

    private AffirmTrack trackModel() {
        final AffirmTrackOrder affirmTrackOrder = AffirmTrackOrder.builder()
                .setStoreName("Affirm Store")
                .setCoupon("SUMMER2018")
                .setCurrency("USD")
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
                .setUnitPrice(1000f)
                .setUrl("http://merchant.com/great_deal_wheel")
                .build();

        final Map<String, Item> items = new HashMap<>();
        items.put("wheel", item);

        final Name name = Name.builder().setFull("John Smith").build();
        final Address address = Address.builder()
                .setCity("San Francisco")
                .setCountry("USA")
                .setLine1("333 Kansas st")
                .setState("CA")
                .setZipcode("94107")
                .build();

        final Shipping shipping = Shipping.builder().setAddress(address).setName(name).build();

        return Checkout.builder()
                .setOrderId("55555")
                .setItems(items)
                .setBilling(shipping)
                .setShipping(shipping)
                .setShippingAmount(0f)
                .setTaxAmount(100f)
                .setTotal(PRICE)
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
}
