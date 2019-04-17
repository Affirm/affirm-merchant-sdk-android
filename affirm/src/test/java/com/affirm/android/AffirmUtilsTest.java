package com.affirm.android;

import com.google.common.collect.ImmutableMap;
import com.google.common.truth.Truth;

import org.junit.Test;

import java.util.Map;

public class AffirmUtilsTest {

    @Test
    public void convertToAffirmAmounts() {
        Truth.assertThat(AffirmUtils.decimalDollarsToIntegerCents(15.5f)).isEqualTo(1550);
        Truth.assertThat(AffirmUtils.decimalDollarsToIntegerCents(15.5432f)).isEqualTo(1554);
        Truth.assertThat(AffirmUtils.decimalDollarsToIntegerCents(3f)).isEqualTo(300);
    }

    @Test
    public void replacePlaceHolders() {
        Map<String, String> map = ImmutableMap.of("money", "55", "name", "jan", "day", "monday");
        String text = "I paid {{money}} to {{name}} last {{day}}";

        Truth.assertThat(AffirmUtils.replacePlaceholders(text, map))
                .contains("I paid 55 to jan last monday");
    }
}
