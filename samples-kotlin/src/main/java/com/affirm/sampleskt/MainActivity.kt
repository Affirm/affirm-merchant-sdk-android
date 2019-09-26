package com.affirm.sampleskt

import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.affirm.android.Affirm
import com.affirm.android.AffirmRequest
import com.affirm.android.CookiesUtil
import com.affirm.android.PromotionCallback
import com.affirm.android.exception.AffirmException
import com.affirm.android.model.*
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.set

class MainActivity : AppCompatActivity(), Affirm.CheckoutCallbacks, Affirm.VcnCheckoutCallbacks, Affirm.PrequalCallbacks {

    companion object {
        private const val TAG = "MainActivity"

        private const val PRICE = 1100f
    }

    private var promoRequest: AffirmRequest? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        price.text = "$$PRICE"
        checkout.setOnClickListener { Affirm.startCheckout(this, checkoutModel(), false) }
        vcnCheckout.setOnClickListener { Affirm.startCheckout(this, checkoutModel(), true) }
        siteModalButton.setOnClickListener { Affirm.showSiteModal(this@MainActivity, "5LNMQ33SEUYHLNUC") }
        productModalButton.setOnClickListener { Affirm.showProductModal(this@MainActivity, PRICE, "0Q97G0Z4Y4TLGHGB") }
        trackOrderConfirmed.setOnClickListener {
            Toast.makeText(this@MainActivity, "Track successfully", Toast.LENGTH_SHORT).show()
            Affirm.trackOrderConfirmed(this@MainActivity, trackModel())
        }
        clearCookies.setOnClickListener {
            CookiesUtil.clearCookies(this@MainActivity)
        }

        Affirm.configureWithAmount(promo, null, PromoPageType.PRODUCT, PRICE, true)

        // Fetch promotion, then use your own TextView to display
        val requestData = Affirm.PromoRequestData.Builder(PRICE, true)
                .setPromoId(null)
                .setPageType(null)
                .build()

        promoRequest = Affirm.fetchPromotion(requestData, promotionTextView.textSize, this, object : PromotionCallback {
            override fun onSuccess(spannableString: SpannableString?, showPrequal: Boolean) {
                promotionTextView.text = spannableString
                promotionTextView.setOnClickListener { Affirm.onPromotionClick(this@MainActivity, requestData, showPrequal) }
            }

            override fun onFailure(exception: AffirmException) {
                Toast.makeText(baseContext, "Failed to get promo message, reason: $exception", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onStart() {
        super.onStart()
        promoRequest?.create()
    }

    override fun onStop() {
        promoRequest?.cancel()
        super.onStop()
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
        val billing = Billing.builder().setAddress(address).setName(name).build()

        return Checkout.builder()
                .setItems(items)
                .setBilling(billing)
                .setShipping(shipping)
                .setShippingAmount(0f)
                .setTaxAmount(100f)
                .setTotal(PRICE)
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

    override fun onAffirmVcnCheckoutCancelledReason(vcnReason: VcnReason) {
        Toast.makeText(this, "Vcn Checkout Cancelled: $vcnReason", Toast.LENGTH_LONG).show()
    }

    override fun onAffirmVcnCheckoutSuccess(cardDetails: CardDetails) {
        Toast.makeText(this, "Vcn Checkout Card: $cardDetails", Toast.LENGTH_LONG).show()
    }

    // - Prequal
    override fun onAffirmPrequalError(message: String?) {
        Toast.makeText(this, "Prequal Error: " + message!!, Toast.LENGTH_LONG).show()
    }
}
