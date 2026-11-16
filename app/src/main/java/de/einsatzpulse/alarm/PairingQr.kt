package de.einsatzpulse.alarm

import org.json.JSONObject
import java.time.OffsetDateTime

/** Backend-generated, short-lived QR pairing ticket. Never connect on scan alone. */
data class PairingQr(val server:String,val code:String,val expiresAtMs:Long) {
    companion object {
        private val fields=setOf("format","version","server","code","expires_at")
        private val codePattern=Regex("""EPA-[A-Za-z0-9_-]{24}""")

        fun parse(raw:String,now:Long=System.currentTimeMillis()):PairingQr {
            require(raw.length in 50..1200) {
                "Ungültiger QR-Code für EinsatzPulse Alarm"
            }
            val data=try { JSONObject(raw) } catch(_:Exception) {
                throw IllegalArgumentException("Dies ist kein EinsatzPulse-Kopplungs-QR-Code")
            }
            val keys=mutableSetOf<String>()
            val names=data.keys()
            while(names.hasNext()) keys.add(names.next())
            require(keys==fields && data.optString("format")=="einsatzpulse-alarm-pair" &&
                data.optInt("version")==1) {
                "QR-Code oder Protokollversion nicht unterstützt"
            }
            val origin=normalizeServerAddress(data.getString("server"))
            val code=data.getString("code")
            require(codePattern.matches(code)) {
                "QR-Kopplungscode ungültig"
            }
            val expiresAt=try {
                OffsetDateTime.parse(data.getString("expires_at")).toInstant().toEpochMilli()
            } catch(_:Exception) {throw IllegalArgumentException("QR-Ablaufdatum ungültig")}
            require(expiresAt>now) {"Dieser QR-Code ist bereits abgelaufen"}
            require(expiresAt-now<=11*60*1000L) {
                "QR-Ablauf liegt außerhalb der zulässigen 10-Minuten-Frist"
            }
            return PairingQr(origin,code,expiresAt)
        }
    }
}
