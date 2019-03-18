package com.affirm.samples

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.affirm.android.Affirm
import com.affirm.android.model.*
import kotlinx.android.synthetic.main.activity_main.*

class MainActivityInKotlin : AppCompatActivity(), Affirm.CheckoutCallbacks {

    companion object {
        private const val TAG = "MainActivityInKotlin"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        checkbox.setOnClickListener {
            Affirm.launchCheckout(this,checkoutModel())
        }
        vcn_checkout.setOnClickListener { Affirm.launchVcnCheckout(this,checkoutModel()) }
        Affirm.writePromoToTextView(this, affirm_promo_label,null,1100f,true,object :Affirm.PromoCallback{

            override fun onSuccess(promo: String?) {

            }
            override fun onFailure(throwable: Throwable?) {
                Log.e(TAG,"As low as label failed...", throwable)
                Toast.makeText(this@MainActivityInKotlin, "As low as label : ${throwable?.message}",Toast.LENGTH_SHORT).show()
            }
        })
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
        super.onActivityResult(requestCode, resultCode, data)
        Affirm.handleAffirmData(this,requestCode, resultCode, data)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main,menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item?.itemId?:0
        //noinspection SimplifiableIfStatement
        return if (id == R.id.action_settings) true else super.onOptionsItemSelected(item)
    }

    override fun onAffirmCheckoutError(message: String?) {
        Toast.makeText(this, "Checkout Error: $message", Toast.LENGTH_LONG).show()
    }

    override fun onAffirmCheckoutCancelled() {
        Toast.makeText(this, "Checkout Cancelled", Toast.LENGTH_LONG).show()
    }

    override fun onAffirmCheckoutSuccess(token: String) {
        Toast.makeText(this, "Checkout token: $token", Toast.LENGTH_LONG).show()
    }

    override fun onAffirmVcnCheckoutError(message: String?) {
        Toast.makeText(this, "Vcn Checkout Error: $message", Toast.LENGTH_LONG).show()
    }

    override fun onAffirmVcnCheckoutCancelled() {
        Toast.makeText(this, "Vcn Checkout Cancelled", Toast.LENGTH_LONG).show()
    }

    override fun onAffirmVcnCheckoutSuccess(cardDetails: CardDetails) {
        Toast.makeText(this, "Vcn Checkout Card: $cardDetails", Toast.LENGTH_LONG).show()
    }

}