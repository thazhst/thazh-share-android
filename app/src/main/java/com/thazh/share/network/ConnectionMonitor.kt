package com.thazh.share.network

import android.net.wifi.WifiManager
import com.thazh.share.data.ConnectionQuality
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.collections.ArrayDeque

private const val INITIAL_PING_DELAY_MS = 120L
private const val PING_INTERVAL_MS = 2_200L
private const val PONG_TIMEOUT_MS = 3_500L
private const val HISTORY_SIZE = 8
private const val ACTIVE_WIFI_KALMAN_KEY = "WIFI_P2P_ACTIVE_LINK"

/**
 * Sends periodic [Frame.Ping]s over an active [PeerConnection] and turns the
 * observed round-trip latency + delivery success rate into a
 * [ConnectionQuality] tier for display (Mạnh/Trung bình/Yếu/Kém/Mất kết nối).
 *
 * Also samples [WifiManager.getConnectionInfo] on the same cadence and passes
 * RSSI through [DistanceEstimator] (backed by the C++20 1D Kalman Filter).
 */
class ConnectionMonitor(
    private val connection: PeerConnection,
    private val scope: CoroutineScope,
    private val wifiManager: WifiManager?,
    private val localBatteryProvider: () -> Pair<Int?, Boolean> = { null to false },
) {
    private val _quality = MutableStateFlow(ConnectionQuality.DISCONNECTED)
    val quality: StateFlow<ConnectionQuality> = _quality.asStateFlow()

    private val _estimatedDistanceMeters = MutableStateFlow<Int?>(null)
    val estimatedDistanceMeters: StateFlow<Int?> = _estimatedDistanceMeters.asStateFlow()

    private var monitorJob: Job? = null
    private var sequenceCounter = 0L
    private val pendingPings = mutableMapOf<Long, Long>() // sequence -> sentAtEpochMs
    private val rttHistoryMs = ArrayDeque<Long?>() // null = timed out / lost

    fun start() {
        stop()
        monitorJob = scope.launch {
            var isFirstPing = true
            while (true) {
                delay(if (isFirstPing) INITIAL_PING_DELAY_MS else PING_INTERVAL_MS)
                isFirstPing = false
                if (!connection.isConnected) {
                    recordResult(null)
                    _estimatedDistanceMeters.value = null
                    continue
                }
                sampleDistance()
                val seq = sequenceCounter++
                val sentAt = System.currentTimeMillis()
                pendingPings[seq] = sentAt
                val (batteryPct, charging) = localBatteryProvider()
                val sent = connection.send(
                    Frame.Ping(
                        sequence = seq,
                        sentAtEpochMs = sentAt,
                        batteryPercent = batteryPct,
                        isCharging = charging,
                    ),
                )
                if (!sent) {
                    pendingPings.remove(seq)
                    recordResult(null)
                    continue
                }

                // Check timeout asynchronously so the next ping cycle isn't artificially delayed
                launch {
                    delay(PONG_TIMEOUT_MS)
                    if (pendingPings.remove(seq) != null) {
                        recordResult(null)
                    }
                }
            }
        }
    }

    /** Call when a [Frame.Pong] arrives on the read loop. */
    fun onPongReceived(pong: Frame.Pong) {
        val sentAt = pendingPings.remove(pong.sequence) ?: return
        val rtt = (System.currentTimeMillis() - sentAt).coerceAtLeast(1L)
        recordResult(rtt)
    }

    /** Call immediately when the socket reports connected/disconnected, for instant UI feedback. */
    fun onConnectionStateHint(isConnected: Boolean) {
        if (!isConnected) {
            rttHistoryMs.clear()
            NativeCoreEngine.clearKalmanHistory(ACTIVE_WIFI_KALMAN_KEY)
            _quality.value = ConnectionQuality.DISCONNECTED
            _estimatedDistanceMeters.value = null
        } else if (_quality.value == ConnectionQuality.DISCONNECTED) {
            _quality.value = ConnectionQuality.STRONG
            sampleDistance()
        }
    }

    private fun sampleDistance() {
        @Suppress("DEPRECATION")
        val rssi = runCatching { wifiManager?.connectionInfo?.rssi }.getOrNull()
        _estimatedDistanceMeters.value = DistanceEstimator.estimateMeters(
            rssiDbm = rssi,
            deviceKey = ACTIVE_WIFI_KALMAN_KEY,
        )
    }

    private fun recordResult(rttMs: Long?) {
        if (rttHistoryMs.size >= HISTORY_SIZE) rttHistoryMs.removeFirstOrNull()
        rttHistoryMs.add(rttMs)
        recomputeQuality()
    }

    private fun recomputeQuality() {
        val samples = rttHistoryMs.toList()
        if (samples.isEmpty()) {
            _quality.value = ConnectionQuality.DISCONNECTED
            return
        }
        val successes = samples.filterNotNull()
        val successRatio = successes.size.toFloat() / samples.size.toFloat()
        val avgLatency = successes.takeIf { it.isNotEmpty() }?.average()?.toLong()

        _quality.value = ConnectionQuality.fromMetrics(
            avgLatencyMs = avgLatency,
            successRatio = successRatio,
            isConnected = connection.isConnected
        )
    }

    fun stop() {
        monitorJob?.cancel()
        monitorJob = null
        pendingPings.clear()
        rttHistoryMs.clear()
        NativeCoreEngine.clearKalmanHistory(ACTIVE_WIFI_KALMAN_KEY)
        _quality.value = ConnectionQuality.DISCONNECTED
        _estimatedDistanceMeters.value = null
    }
}
