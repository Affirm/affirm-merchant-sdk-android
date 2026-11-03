package com.affirm.android;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class PromoReconfigureTest {

    @Test
    public void currentRequestFollowsViewTag() {
        AffirmPromotionButton button = newButton();
        PromoRequest first = mock(PromoRequest.class);
        PromoRequest second = mock(PromoRequest.class);

        button.setTag(R.id.affirm_promo_request, first);
        assertTrue(button.isCurrentPromoRequest(first));

        button.setTag(R.id.affirm_promo_request, second);
        assertFalse(button.isCurrentPromoRequest(first));
        assertTrue(button.isCurrentPromoRequest(second));
        assertFalse(button.isCurrentPromoRequest(null));
    }

    @Test
    public void destroyCancelsAndClearsCurrentRequest() {
        AffirmPromotionButton button = newButton();
        PromoRequest request = mock(PromoRequest.class);
        button.setTag(R.id.affirm_promo_request, request);

        button.destroy();

        verify(request).cancel();
        assertNull(button.getTag(R.id.affirm_promo_request));
        assertFalse(button.isCurrentPromoRequest(request));
    }

    @Test
    public void isShowingTracksCurrentLabel() {
        AffirmPromotionButton button = newButton();
        assertFalse(button.hasLabel("msg"));

        ReflectionHelpers.setField(button, "message", "msg");
        assertTrue(button.hasLabel("msg"));
        assertFalse(button.hasLabel("other"));
    }

    private static AffirmPromotionButton newButton() {
        return mock(AffirmPromotionButton.class, Mockito.CALLS_REAL_METHODS);
    }
}
