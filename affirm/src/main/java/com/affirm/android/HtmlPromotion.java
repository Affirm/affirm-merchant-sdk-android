package com.affirm.android;

import androidx.annotation.NonNull;

public class HtmlPromotion {
    @NonNull
    private String htmlPromo;
    @NonNull
    private String description;
    private boolean showPrequal;

    public HtmlPromotion(@NonNull String htmlPromo,
                         @NonNull String description,
                         boolean showPrequal) {
        this.htmlPromo = htmlPromo;
        this.description = description;
        this.showPrequal = showPrequal;
    }

    @NonNull
    public String getHtmlPromo() {
        return htmlPromo;
    }

    public void setHtmlPromo(@NonNull String htmlPromo) {
        this.htmlPromo = htmlPromo;
    }

    @NonNull
    public String getDescription() {
        return description;
    }

    public void setDescription(@NonNull String description) {
        this.description = description;
    }

    public boolean isShowPrequal() {
        return showPrequal;
    }

    public void setShowPrequal(boolean showPrequal) {
        this.showPrequal = showPrequal;
    }
}