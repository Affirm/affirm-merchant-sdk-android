package com.affirm.android;

import com.google.common.collect.ImmutableMap;
import com.google.common.truth.Truth;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.Map;

public class AffirmUtilsTest {

    @Test
    public void convertToAffirmAmounts() {
        Truth.assertThat(AffirmUtils.decimalDollarsToIntegerCents(BigDecimal.valueOf(15.5))).isEqualTo(1550);
        Truth.assertThat(AffirmUtils.decimalDollarsToIntegerCents(BigDecimal.valueOf(15.5492))).isEqualTo(1554);
        Truth.assertThat(AffirmUtils.decimalDollarsToIntegerCents(BigDecimal.valueOf(3.0))).isEqualTo(300);
    }

    @Test
    public void replacePlaceHolders() {
        Map<String, String> map = ImmutableMap.of("money", "55", "name", "jan", "day", "monday");
        String text = "I paid {{money}} to {{name}} last {{day}}";

        Truth.assertThat(AffirmUtils.replacePlaceholders(text, map))
                .contains("I paid 55 to jan last monday");
    }
}
