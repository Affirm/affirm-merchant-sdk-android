package com.affirm.android.model

/** Result of `Affirm.prepareAuth`. */
class AffirmPrepareAuthResult private constructor(
    @get:JvmName("status")
    val status: Status,
    /** Null when [Status.UNAVAILABLE]. */
    @get:JvmName("codeChallenge")
    val codeChallenge: String?
) {
    enum class Status { PENDING, UNAVAILABLE }

    override fun toString(): String =
        "AffirmPrepareAuthResult(status=$status, codeChallenge=$codeChallenge)"

    companion object {
        @JvmStatic
        fun create(status: Status, codeChallenge: String?): AffirmPrepareAuthResult =
            AffirmPrepareAuthResult(status, codeChallenge)
    }
}
