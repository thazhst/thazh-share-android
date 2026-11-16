package com.thazh.share.service

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import com.thazh.share.network.NfcConnectionCoordinator

/**
 * Host Card Emulation (HCE) responder so tapping two devices running Thazh Share
 * exchanges Wi-Fi Direct names and addresses without deprecated Android Beam.
 */
class ShareNfcHceService : HostApduService() {

    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        if (commandApdu == null || commandApdu.size < 4) {
            return NfcConnectionCoordinator.SW_UNKNOWN
        }

        if (isSelectAidApdu(commandApdu)) {
            return NfcConnectionCoordinator.SW_OK
        }

        if (commandApdu[0] == NfcConnectionCoordinator.CLA_THAZH &&
            commandApdu[1] == NfcConnectionCoordinator.INS_EXCHANGE &&
            commandApdu.size >= 5
        ) {
            val lc = commandApdu[4].toInt() and 0xFF
            if (commandApdu.size >= 5 + lc) {
                val peerBytes = commandApdu.copyOfRange(5, 5 + lc)
                val peer = NfcConnectionCoordinator.parsePayload(peerBytes)
                if (peer != null) {
                    NfcConnectionCoordinator.onPeerExchanged(peer, initiateConnect = false)
                }
            }
            return NfcConnectionCoordinator.currentPayloadBytes() + NfcConnectionCoordinator.SW_OK
        }

        return NfcConnectionCoordinator.SW_INS_NOT_SUPPORTED
    }

    override fun onDeactivated(reason: Int) {
        // No persistent session state to clean up per tap.
    }

    private fun isSelectAidApdu(apdu: ByteArray): Boolean {
        val aid = NfcConnectionCoordinator.AID_BYTES
        if (apdu.size < 5 + aid.size) return false
        if (apdu[0] != 0x00.toByte() || apdu[1] != 0xA4.toByte() || apdu[2] != 0x04.toByte()) {
            return false
        }
        val lc = apdu[4].toInt() and 0xFF
        if (lc != aid.size) return false
        for (i in aid.indices) {
            if (apdu[5 + i] != aid[i]) return false
        }
        return true
    }
}
