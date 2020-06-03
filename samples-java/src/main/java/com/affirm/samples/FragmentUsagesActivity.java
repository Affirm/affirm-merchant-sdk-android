package com.affirm.samples;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.affirm.android.Affirm;
import com.affirm.android.AffirmPromotionButton;
import com.affirm.android.model.Address;
import com.affirm.android.model.Billing;
import com.affirm.android.model.CardDetails;
import com.affirm.android.model.Checkout;
import com.affirm.android.model.Currency;
import com.affirm.android.model.Item;
import com.affirm.android.model.Name;
import com.affirm.android.model.PromoPageType;
import com.affirm.android.model.Shipping;
import com.affirm.android.model.VcnReason;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class FragmentUsagesActivity extends AppCompatActivity implements Affirm.CheckoutCallbacks, Affirm.VcnCheckoutCallbacks, Affirm.PrequalCallbacks {

    private static final BigDecimal PRICE = BigDecimal.valueOf(1100.0);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment_usages);

        AffirmPromotionButton affirmPromotionButton = findViewById(R.id.promo);
        Affirm.configureWithAmount(R.id.container, affirmPromotionButton, PromoPageType.PRODUCT, PRICE, true);

        findViewById(R.id.checkout).setOnClickListener(v -> {
            try {
                Affirm.startCheckout(this, R.id.container, checkoutModel(), false);
            } catch (Exception e) {
                Toast.makeText(getBaseContext(), "Checkout failed, reason: " + e.toString(), Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.vcnCheckout).setOnClickListener(v -> {
            try {
                Affirm.startCheckout(this, R.id.container, checkoutModel(), true);
            } catch (Exception e) {
                Toast.makeText(getBaseContext(), "VCN Checkout failed, reason: " + e.toString(), Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.siteModalButton).setOnClickListener(v -> Affirm.showSiteModal(FragmentUsagesActivity.this, R.id.container, "5LNMQ33SEUYHLNUC"));

        findViewById(R.id.productModalButton).setOnClickListener(v -> Affirm.showProductModal(FragmentUsagesActivity.this, R.id.container, PRICE, null, PromoPageType.PRODUCT, null));
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
    public void onAffirmCheckoutError(@Nullable String message) {
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
}
