package com.affirm.samples;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

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

public class FragmentUsagesFragment extends Fragment implements Affirm.CheckoutCallbacks, Affirm.VcnCheckoutCallbacks, Affirm.PrequalCallbacks {

    private static final BigDecimal PRICE = BigDecimal.valueOf(1100.0);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_fragment_usages, container);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AffirmPromotionButton affirmPromotionButton = view.findViewById(R.id.promo);
        Affirm.configureWithAmount(this, R.id.container, affirmPromotionButton, null, PromoPageType.PRODUCT, PRICE, true, null);

        view.findViewById(R.id.checkout).setOnClickListener(v -> {
            try {
                Affirm.startCheckout(this, R.id.container, checkoutModel(), null, 10, false);
            } catch (Exception e) {
                Toast.makeText(getContext(), "Checkout failed, reason: " + e.toString(), Toast.LENGTH_SHORT).show();
            }
        });

        view.findViewById(R.id.vcnCheckout).setOnClickListener(v -> {
            try {
                Affirm.startCheckout(this, R.id.container, checkoutModel(), null, 10, true);
            } catch (Exception e) {
                Toast.makeText(getContext(), "VCN Checkout failed, reason: " + e.toString(), Toast.LENGTH_SHORT).show();
            }
        });

        view.findViewById(R.id.siteModalButton).setOnClickListener(v -> Affirm.showSiteModal(this, R.id.container, null, Config.MODAL_ID));

        view.findViewById(R.id.productModalButton).setOnClickListener(v -> Affirm.showProductModal(this, R.id.container, PRICE, null, PromoPageType.PRODUCT, null));
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

        final Address address = Address.builder()
                .setCity("San Francisco")
                .setCountry("USA")
                .setStreet1("333 Kansas st")
                .setRegion1Code("CA")
                .setPostalCode("94107")
                .build();

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
                .setCurrency(Currency.USD)
                .setMetadata(metadata)
                .build();
    }

    // - Affirm.CheckoutCallbacks
    @Override
    public void onAffirmCheckoutSuccess(@NonNull String token) {
        Toast.makeText(getContext(), "Checkout token: " + token, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onAffirmCheckoutCancelled() {
        Toast.makeText(getContext(), "Checkout Cancelled", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onAffirmCheckoutError(@Nullable String message) {
        Toast.makeText(getContext(), "Checkout Error: " + message, Toast.LENGTH_LONG).show();
    }

    // - Affirm.VcnCheckoutCallbacks
    @Override
    public void onAffirmVcnCheckoutCancelled() {
        Toast.makeText(getContext(), "Vcn Checkout Cancelled", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onAffirmVcnCheckoutCancelledReason(@NonNull VcnReason vcnReason) {
        Toast.makeText(getContext(), "Vcn Checkout Cancelled: " + vcnReason.toString(), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onAffirmVcnCheckoutError(@Nullable String message) {
        Toast.makeText(getContext(), "Vcn Checkout Error: " + message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onAffirmVcnCheckoutSuccess(@NonNull CardDetails cardDetails) {
        Toast.makeText(getContext(), "Vcn Checkout Card: " + cardDetails.toString(), Toast.LENGTH_LONG).show();
    }

    // - Prequal
    @Override
    public void onAffirmPrequalError(@Nullable String message) {
        Toast.makeText(getContext(), "Prequal Error: " + message, Toast.LENGTH_LONG).show();
    }
}