package com.affirm.sampleskt

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.affirm.android.Affirm
import com.affirm.android.model.*
import kotlinx.android.synthetic.main.activity_main.*
import java.util.ArrayList

class MainActivity : AppCompatActivity(), Affirm.CheckoutCallbacks, Affirm.VcnCheckoutCallbacks, Affirm.PrequalCallbacks {

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkout.setOnClickListener { Affirm.startCheckout(this, checkoutModel(), false) }
        vcnCheckout.setOnClickListener { Affirm.startCheckout(this, checkoutModel(), true) }
        siteModalButton.setOnClickListener { Affirm.showSiteModal(this@MainActivity, "5LNMQ33SEUYHLNUC") }
        productModalButton.setOnClickListener { Affirm.showProductModal(this@MainActivity, 1100f, "0Q97G0Z4Y4TLGHGB") }
        trackOrderConfirmed.setOnClickListener { Affirm.trackOrderConfirmed(this@MainActivity, trackModel()) }

        Affirm.configureWithAmount(promo, null, 1100f, true)
    }

    private fun trackModel(): AffirmTrack {
        val affirmTrackOrder = AffirmTrackOrder.builder()
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
                .build()

        val affirmTrackProduct = AffirmTrackProduct.builder()
                .setBrand("Affirm")
                .setCategory("Apparel")
                .setCoupon("SUMMER2018")
                .setName("Affirm T-Shirt")
                .setPrice(730)
                .setProductId("SKU-1234")
                .setQuantity(1)
                .setVariant("Black")
                .build()

        val affirmTrackProducts = ArrayList<AffirmTrackProduct>()
        affirmTrackProducts.add(affirmTrackProduct)

        return AffirmTrack.builder()
                .setAffirmTrackOrder(affirmTrackOrder)
                .setAffirmTrackProducts(affirmTrackProducts)
                .build()
    }

    private fun checkoutModel(): Checkout {
        val item = Item.builder()
                .setDisplayName("Great Deal Wheel")
                .setImageUrl(
                        "http://www.m2motorsportinc.com/media/catalog/product/cache/1/thumbnail" +
                                "/9df78eab33525d08d6e5fb8d27136e95/v/e/velocity-vw125-wheels-rims.jpg")
                .setQty(1)
                .setSku("wheel")
                .setUnitPrice(1000f)
                .setUrl("http://merchant.com/great_deal_wheel")
                .build()
        val items: MutableMap<String, Item> = HashMap()
        items["wheel"] = item
        val name = Name.builder().setFull("John Smith").build()
        val address = Address.builder()
                .setCity("San Francisco")
                .setCountry("USA")
                .setLine1("333 Kansas st")
                .setState("CA")
                .setZipcode("94107")
                .build()
        val shipping = Shipping.builder().setAddress(address).setName(name).build()
        return Checkout.builder()
                .setItems(items)
                .setBilling(shipping)
                .setShipping(shipping)
                .setShippingAmount(0f)
                .setTaxAmount(100f)
                .setTotal(1100f)
                .build()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (Affirm.handleCheckoutData(this, requestCode, resultCode, data)) {
            return
        }

        if (Affirm.handleVcnCheckoutData(this, requestCode, resultCode, data)) {
            return
        }

        if (Affirm.handlePrequalData(this, requestCode, resultCode, data)) {
            return
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    // - Affirm.CheckoutCallbacks
    override fun onAffirmCheckoutError(message: String?) {
        Toast.makeText(this, "Checkout Error: $message", Toast.LENGTH_LONG).show()
    }

    override fun onAffirmCheckoutCancelled() {
        Toast.makeText(this, "Checkout Cancelled", Toast.LENGTH_LONG).show()
    }

    override fun onAffirmCheckoutSuccess(token: String) {
        Toast.makeText(this, "Checkout token: $token", Toast.LENGTH_LONG).show()
    }

    // - Affirm.VcnCheckoutCallbacks
    override fun onAffirmVcnCheckoutError(message: String?) {
        Toast.makeText(this, "Vcn Checkout Error: $message", Toast.LENGTH_LONG).show()
    }

    override fun onAffirmVcnCheckoutCancelled() {
        Toast.makeText(this, "Vcn Checkout Cancelled", Toast.LENGTH_LONG).show()
    }

    override fun onAffirmVcnCheckoutSuccess(cardDetails: CardDetails) {
        Toast.makeText(this, "Vcn Checkout Card: $cardDetails", Toast.LENGTH_LONG).show()
    }

    // - Prequal
    override fun onAffirmPrequalError(message: String?) {
        Toast.makeText(this, "Prequal Error: " + message!!, Toast.LENGTH_LONG).show()
    }
}