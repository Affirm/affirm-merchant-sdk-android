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
        /** @throws IllegalArgumentException unless [codeChallenge] is set exactly when [status] is PENDING. */
        @JvmStatic
        fun create(status: Status, codeChallenge: String?): AffirmPrepareAuthResult {
            require((status == Status.PENDING) == (codeChallenge != null)) {
                "codeChallenge must be set if and only if status is PENDING"
            }
            return AffirmPrepareAuthResult(status, codeChallenge)
        }
    }
}
