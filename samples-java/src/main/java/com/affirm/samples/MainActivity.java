package com.affirm.samples;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.affirm.android.Affirm;
import com.affirm.android.AffirmPromotionButton;
import com.affirm.android.model.Address;
import com.affirm.android.model.AffirmTrack;
import com.affirm.android.model.AffirmTrackOrder;
import com.affirm.android.model.AffirmTrackProduct;
import com.affirm.android.model.CardDetails;
import com.affirm.android.model.Checkout;
import com.affirm.android.model.Item;
import com.affirm.android.model.Name;
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

    private TextView mMessageLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mMessageLabel = findViewById(R.id.message);

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
                Affirm.showProductModal(MainActivity.this, 1100, "0Q97G0Z4Y4TLGHGB");
            }
        });

        findViewById(R.id.trackOrderConfirmed).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Track successfully", Toast.LENGTH_SHORT).show();
                Affirm.trackOrderConfirmed(MainActivity.this, trackModel());
            }
        });


        AffirmPromotionButton affirmPromotionButton = findViewById(R.id.promo);

//        AffirmPromotionButton affirmPromotionButton = new AffirmPromotionButton(this);
//        affirmPromotionButton.setAffirmColor(AffirmColor.AFFIRM_COLOR_TYPE_BLUE);
//        affirmPromotionButton.setAffirmLogoType(AffirmLogoType.AFFIRM_DISPLAY_TYPE_LOGO);
//
//        ViewGroup viewGroup = getWindow().getDecorView().findViewById(android.R.id.content);
//        viewGroup.addView(affirmPromotionButton);

        Affirm.configureWithAmount(affirmPromotionButton, null, 1100, true);

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
                .setItems(items)
                .setBilling(shipping)
                .setShipping(shipping)
                .setShippingAmount(0f)
                .setTaxAmount(100f)
                .setTotal(1100f)
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
        mMessageLabel.setText("Checkout token: " + token);
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
        mMessageLabel.setText(cardDetails.toString());
    }

    // - Prequal
    @Override
    public void onAffirmPrequalError(@Nullable String message) {
        Toast.makeText(this, "Prequal Error: " + message, Toast.LENGTH_LONG).show();
    }
}
