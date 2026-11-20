package com.thazh.share.service

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log
import com.thazh.share.network.NfcConnectionCoordinator
import com.thazh.share.network.NfcPeerPayload
import com.thazh.share.network.NfcRole

/**
 * Host Card Emulation (HCE) service for NFC Tap-to-Connect when the device is on the Receive tab.
 *
 * Correct two-device NFC flow:
 * 1. Device A is on the **Send tab** (`ShareTab.SEND`) with NFC on -> acts as NFC Reader (PCD).
 * 2. Device B is on the **Receive tab** (`ShareTab.RECEIVE`) with NFC on -> Reader mode is off,
 *    so Android routes the NFC field to this [ShareNfcHceService] (HCE Card Emulator).
 * 3. When the two devices touch back-to-back:
 *    - Device A sends `SELECT AID` (`F05448415A485348`) and then `INS_EXCHANGE_PEER` containing
 *      Device A's identity (`NfcPeerPayload`).
 *    - Device B ([ShareNfcHceService]) replies with Device B's identity (`NfcPeerPayload` + `90 00`),
 *      immediately wakes Wi-Fi Direct discovery & visibility (`initiateConnect = false`), and trusts
 *      Device A for automatic incoming connection approval.
 *    - Device A receives Device B's identity and initiates the Wi-Fi Direct connection (`initiateConnect = true`).
 */
class ShareNfcHceService : HostApduService() {

    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        if (commandApdu == null || commandApdu.size < 4) {
            return NfcConnectionCoordinator.STATUS_FAILED
        }

        val cla = commandApdu[0]
        val ins = commandApdu[1]

        // 1. ISO-7816 SELECT by AID: 00 A4 04 00 <Lc> <AID> [Le]
        if (cla == 0x00.toByte() && ins == 0xA4.toByte()) {
            val localBytes = NfcConnectionCoordinator
                .buildLocalPayload(NfcRole.RECEIVER_CARD_EMULATOR)
                .toJsonBytes()
            return localBytes + NfcConnectionCoordinator.STATUS_OK
        }

        // 2. Custom Peer Exchange APDU: 80 CA 00 00 <Lc> <PeerJsonBytes> [Le]
        if (cla == 0x80.toByte() && ins == NfcConnectionCoordinator.INS_EXCHANGE_PEER) {
            val lc = if (commandApdu.size >= 5) (commandApdu[4].toInt() and 0xFF) else 0
            val dataStart = 5
            val dataEnd = (dataStart + lc).coerceAtMost(commandApdu.size)
            if (lc > 0 && dataEnd > dataStart) {
                val peerBytes = commandApdu.copyOfRange(dataStart, dataEnd)
                val peerPayload = NfcPeerPayload.fromJsonBytes(peerBytes)
                if (peerPayload != null) {
                    Log.d(
                        TAG,
                        "HCE received Sender payload from ${peerPayload.displayName} (${peerPayload.wifiP2pName} / ${peerPayload.deviceAddress})",
                    )
                    // This device is the Receiver (HCE Card): prepare Wi-Fi Direct discovery & trust the Sender
                    NfcConnectionCoordinator.onPeerExchanged(
                        peer = peerPayload,
                        initiateConnect = false,
                    )
                }
            }
            val localBytes = NfcConnectionCoordinator
                .buildLocalPayload(NfcRole.RECEIVER_CARD_EMULATOR)
                .toJsonBytes()
            return localBytes + NfcConnectionCoordinator.STATUS_OK
        }

        return NfcConnectionCoordinator.STATUS_FAILED
    }

    override fun onDeactivated(reason: Int) {
        Log.d(TAG, "HCE deactivated (reason=$reason)")
    }

    companion object {
        private const val TAG = "ShareNfcHceService"
    }
}
