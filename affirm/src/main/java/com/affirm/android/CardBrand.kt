package com.affirm.android

internal enum class CardBrand(
    val type: String,
    private val prefixes: Set<String> = emptySet()
) {
    Visa(
        "visa",
        prefixes = setOf("4")
    ),
    MasterCard(
        "mastercard",
        prefixes = setOf(
            "2221", "2222", "2223", "2224", "2225", "2226", "2227", "2228", "2229", "223", "224",
            "225", "226", "227", "228", "229", "23", "24", "25", "26", "270", "271", "2720",
            "50", "51", "52", "53", "54", "55", "67"
        )
    );

    companion object {
        internal fun fromCardNumber(number: String): CardBrand? {
            return values().firstOrNull { brand ->
                brand.prefixes.takeIf { it.isNotEmpty() }?.any {
                    number.startsWith(
                        it
                    )
                } == true
            }
        }
    }
}
