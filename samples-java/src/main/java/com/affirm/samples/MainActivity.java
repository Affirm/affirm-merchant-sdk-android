package com.affirm.samples;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.affirm.android.Affirm;
import com.affirm.android.AffirmPromoLabel;
import com.affirm.android.model.Address;
import com.affirm.android.model.CardDetails;
import com.affirm.android.model.Checkout;
import com.affirm.android.model.Item;
import com.affirm.android.model.Name;
import com.affirm.android.model.Shipping;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class MainActivity extends AppCompatActivity implements Affirm.CheckoutCallbacks {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        findViewById(R.id.checkbox).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Affirm.launchCheckout(MainActivity.this, checkoutModel());
            }
        });

        findViewById(R.id.vcn_checkout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Affirm.launchVcnCheckout(MainActivity.this, checkoutModel());
            }
        });

        AffirmPromoLabel label = findViewById(R.id.affirm_promo_label);
        Affirm.writePromoToTextView(this, label, null, 1100, true, new Affirm.PromoCallback() {
            @Override
            public void onSuccess(String promo) {

            }

            @Override
            public void onFailure(Throwable throwable) {
                Log.e(TAG, "As low as label failed...", throwable);
                Toast.makeText(MainActivity.this, "As low as label : " + throwable.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });

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
        super.onActivityResult(requestCode, resultCode, data);
        Affirm.handleAffirmData(this, requestCode, resultCode, data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

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
}
