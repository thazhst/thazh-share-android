package com.thazh.share.network

import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Rough RSSI → distance estimate using the standard log-distance path-loss
 * model: distance = 10 ^ ((measuredPowerAt1m - rssi) / (10 * n)).
 *
 * This is inherently imprecise — multipath reflections, obstructions, and
 * antenna orientation can easily shift the result by 2x or more — but it's
 * the same approach BLE beacon proximity estimation uses, and it gives users
 * a rough "how far apart are we" sense rather than a survey-grade
 * measurement. It's meant to be read as "khoảng 27m" (approximately), not
 * "exactly 27m".
 *
 * Android has no public API that returns a WiFi Direct peer's own RSSI —
 * see [com.thazh.share.network.ConnectionMonitor] for where this
 * reads [android.net.wifi.WifiManager.getConnectionInfo] instead, which in
 * practice reflects the active P2P link's signal on most (not guaranteed
 * all) devices/OEMs once a group has formed.
 */
object DistanceEstimator {
    // Typical WiFi RSSI at 1 meter for a phone-class antenna. Real devices
    // vary roughly -40 to -55 dBm depending on chipset/antenna; -50 is a
    // reasonable midpoint used as a fixed calibration constant here since
    // there's no way to calibrate per-device without a controlled setup.
    private const val MEASURED_POWER_AT_1M = -50.0

    // Path-loss exponent: ~2.0 in free space/open air, 2.5-4 indoors with
    // walls/obstructions in the way. 2.5 splits the difference for a
    // typical room/hallway scenario, the common case for this app.
    private const val PATH_LOSS_EXPONENT = 2.5

    // Below this, RSSI is too weak/noisy for the model to mean anything —
    // better to show nothing than a wildly implausible number.
    private const val MIN_PLAUSIBLE_RSSI = -95

    private const val MAX_METERS = 300

    /** Returns an estimated distance in whole meters, or null if [rssiDbm] is missing/implausible. */
    fun estimateMeters(rssiDbm: Int?): Int? {
        if (rssiDbm == null || rssiDbm == 0 || rssiDbm < MIN_PLAUSIBLE_RSSI) return null
        val exponentTerm = (MEASURED_POWER_AT_1M - rssiDbm) / (10.0 * PATH_LOSS_EXPONENT)
        val meters = 10.0.pow(exponentTerm)
        if (meters.isNaN() || meters.isInfinite()) return null
        return meters.roundToInt().coerceIn(1, MAX_METERS)
    }
}
