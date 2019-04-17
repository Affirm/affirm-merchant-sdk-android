package com.affirm.android.utils;

import com.affirm.android.model.Address;
import com.affirm.android.model.Checkout;
import com.affirm.android.model.Item;
import com.affirm.android.model.Name;
import com.affirm.android.model.Shipping;

import java.util.HashMap;
import java.util.Map;

public class CheckoutFactory {

    public static Checkout create() {
        final Item item = Item.builder()
                .setDisplayName("Great Deal Wheel")
                .setImageUrl("http://www.image.com/111")
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
                .setZipcode("94103")
                .build();

        final Shipping shipping = Shipping.builder().setAddress(address).setName(name).build();

        return Checkout.builder()
                .setItems(items)
                .setBilling(shipping)
                .setShipping(shipping)
                .setShippingAmount(1000f)
                .setTaxAmount(100f)
                .setTotal(1100f)
                .build();
    }
}
