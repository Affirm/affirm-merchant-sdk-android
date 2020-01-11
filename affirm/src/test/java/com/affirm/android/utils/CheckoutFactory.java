package com.affirm.android.utils;

import com.affirm.android.model.Address;
import com.affirm.android.model.Billing;
import com.affirm.android.model.Checkout;
import com.affirm.android.model.Currency;
import com.affirm.android.model.Item;
import com.affirm.android.model.Name;
import com.affirm.android.model.Shipping;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class CheckoutFactory {

    public static Checkout create() {
        final Item item = Item.builder()
                .setDisplayName("Great Deal Wheel")
                .setImageUrl("http://www.image.com/111")
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
                .setLine1("333 Kansas st")
                .setState("CA")
                .setZipcode("94103")
                .build();

        final Shipping shipping = Shipping.builder().setAddress(address).setName(name).build();
        final Billing billing = Billing.builder().setAddress(address).setName(name).build();

        return Checkout.builder()
                .setItems(items)
                .setBilling(billing)
                .setShipping(shipping)
                .setShippingAmount(BigDecimal.valueOf(1000.0))
                .setTaxAmount(BigDecimal.valueOf(100.0))
                .setTotal(BigDecimal.valueOf(1100.0))
                .setCurrency(Currency.USD)
                .build();
    }
}
