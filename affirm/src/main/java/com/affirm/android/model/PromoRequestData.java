package com.affirm.android.model;

import androidx.annotation.Nullable;

import com.affirm.android.AffirmColor;
import com.affirm.android.AffirmLogoType;

import static com.affirm.android.AffirmColor.AFFIRM_COLOR_TYPE_BLUE;
import static com.affirm.android.AffirmLogoType.AFFIRM_DISPLAY_TYPE_LOGO;

public class PromoRequestData {
    @Nullable
    private String promoId;

    @Nullable
    private PromoPageType pageType;

    private float amount;
    private boolean showCta;
    private AffirmColor affirmColor;
    private AffirmLogoType affirmLogoType;

    private PromoRequestData(String promoId, PromoPageType pageType, float amount, boolean showCta, AffirmColor affirmColor, AffirmLogoType affirmLogoType) {
        this.promoId = promoId;
        this.pageType = pageType;
        this.amount = amount;
        this.showCta = showCta;
        this.affirmColor = affirmColor;
        this.affirmLogoType = affirmLogoType;
    }

    @Nullable
    public String getPromoId() {
        return promoId;
    }

    @Nullable
    public PromoPageType getPageType() {
        return pageType;
    }

    public float getAmount() {
        return amount;
    }

    public boolean showCta() {
        return showCta;
    }

    public AffirmColor getAffirmColor() {
        return affirmColor;
    }

    public AffirmLogoType getAffirmLogoType() {
        return affirmLogoType;
    }

    public static final class Builder {
        @Nullable
        private String promoId;

        @Nullable
        private PromoPageType pageType;

        private float amount;
        private boolean showCta;
        private AffirmColor affirmColor;
        private AffirmLogoType affirmLogoType;

        public Builder() {

        }

        /**
         * @param promoId the client's modal id
         */
        public Builder setPromoId(@Nullable String promoId) {
            this.promoId = promoId;
            return this;
        }

        /**
         * @param pageType need to use one of
         *                 "banner, cart, category, homepage, landing, payment, product, search"
         */
        public Builder setPageType(@Nullable PromoPageType pageType) {
            this.pageType = pageType;
            return this;
        }

        /**
         * @param amount a float that represents the amount to retrieve pricing for
         *               eg 112.02 as $112 and 2¢
         */
        public Builder setAmount(float amount) {
            this.amount = amount;
            return this;
        }

        /**
         * @param showCta whether need to show cta
         */
        public Builder setShowCta(boolean showCta) {
            this.showCta = showCta;
            return this;
        }

        /**
         * @param affirmColor the color used for the affirm logo in the response
         */
        public Builder setAffirmColor(AffirmColor affirmColor) {
            this.affirmColor = affirmColor;
            return this;
        }

        /**
         * @param affirmLogoType the type of affirm logo to use in the response
         */
        public Builder setAffirmLogoType(AffirmLogoType affirmLogoType) {
            this.affirmLogoType = affirmLogoType;
            return this;
        }

        public PromoRequestData build() {
            if (affirmLogoType == null) {
                affirmLogoType = AFFIRM_DISPLAY_TYPE_LOGO;
            }

            if (affirmColor == null) {
                affirmColor = AFFIRM_COLOR_TYPE_BLUE;
            }

            return new PromoRequestData(promoId, pageType, amount, showCta, affirmColor, affirmLogoType);
        }
    }
}
