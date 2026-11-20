package com.thazh.share.network

/**
 * High-precision RSSI → distance estimator backed by [NativeCoreEngine]
 * (C++20 1D Kalman Filter + Log-Distance Path-Loss solver).
 *
 * When a [deviceKey] is supplied, raw wireless RSSI measurements are passed through
 * a per-device 1D Kalman Filter (`Q = 0.18`, `R = 2.60`) to suppress multipath indoor
 * signal reflections and antenna orientation spikes before computing distance in meters.
 */
object DistanceEstimator {
    private const val MEASURED_POWER_AT_1M = -50.0f
    private const val PATH_LOSS_EXPONENT = 2.5f
    private const val MIN_PLAUSIBLE_RSSI = -95

    /** Returns an estimated distance in whole meters, or null if [rssiDbm] is missing/implausible. */
    fun estimateMeters(rssiDbm: Int?, deviceKey: String? = null): Int? {
        if (rssiDbm == null || rssiDbm >= 0 || rssiDbm < MIN_PLAUSIBLE_RSSI) return null
        if (!deviceKey.isNullOrBlank()) {
            return NativeCoreEngine.filterRssiAndEstimateDistance(
                deviceKey = deviceKey,
                rawRssiDbm = rssiDbm,
                txPowerAt1m = MEASURED_POWER_AT_1M,
                pathLossExponent = PATH_LOSS_EXPONENT,
            ).second
        }
        return NativeCoreEngine.estimateDistanceFromFilteredRssi(
            filteredRssiDbm = rssiDbm.toFloat(),
            txPowerAt1m = MEASURED_POWER_AT_1M,
            pathLossExponent = PATH_LOSS_EXPONENT,
        )
    }

    /**
     * Filters [rawRssiDbm] through the 1D Kalman Filter for [deviceKey] and returns
     * `(smoothedRssiDbm, estimatedDistanceMeters)`.
     */
    fun estimateFiltered(deviceKey: String, rawRssiDbm: Int?): Pair<Int?, Int?> {
        if (rawRssiDbm == null || rawRssiDbm >= 0 || rawRssiDbm < MIN_PLAUSIBLE_RSSI) {
            return null to null
        }
        return NativeCoreEngine.filterRssiAndEstimateDistance(
            deviceKey = deviceKey,
            rawRssiDbm = rawRssiDbm,
            txPowerAt1m = MEASURED_POWER_AT_1M,
            pathLossExponent = PATH_LOSS_EXPONENT,
        )
    }
}
