## New Checkout Flow

We provide a new vcn checkout process, which will include these two screens at the beginning and the end

<p align="center">
<img src="assets/loan_amount.png" width="30%" alt="LoanAmountActivity" hspace="10">
<img src="assets/vcn_display.png" width="30%" alt="VcnDisplayActivity" hspace="10">
</p>

- Start the new vcn checkout flow. Also you must include a checkout object, just like before
```
    Affirm.startNewVcnCheckoutFlow(MainActivity.this, checkoutModel());
```

- If the checkout has not expired, you can also open the vcn display page by the following method.
Also you must include the checkout object, we can edit the checkout.
```
    if (Affirm.existCachedCard(getApplicationContext())) {
        Affirm.startVcnDisplay(MainActivity.this, checkoutModel());
    }
```