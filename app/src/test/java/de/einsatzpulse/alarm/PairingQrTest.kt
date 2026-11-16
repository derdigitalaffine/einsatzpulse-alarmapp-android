package de.einsatzpulse.alarm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.json.JSONObject
import java.time.Instant

class PairingQrTest {
    private val now=1_800_000_000_000L
    private val code="EPA-"+"A".repeat(24)

    private fun ticket(server:String="https://alarm.example.org:8443",
                       codeValue:String=code,expires:Long=now+600_000L):String =
        JSONObject()
            .put("format","einsatzpulse-alarm-pair")
            .put("version",1)
            .put("server",server)
            .put("code",codeValue)
            .put("expires_at",Instant.ofEpochMilli(expires).toString())
            .toString()

    @Test fun acceptsOnlyHttpsServerAndSingleUseTicketShape() {
        val parsed=PairingQr.parse(ticket(),now)
        assertEquals("https://alarm.example.org:8443",parsed.server)
        assertEquals(code,parsed.code)
        assertEquals(now+600_000L,parsed.expiresAtMs)
    }
    @Test fun rejectsExpiredAndFutureDatedCodes() {
        assertThrows(IllegalArgumentException::class.java) {
            PairingQr.parse(ticket(expires=now),now)
        }
        assertThrows(IllegalArgumentException::class.java) {
            PairingQr.parse(ticket(expires=now+12*60_000L),now)
        }
    }
    @Test fun rejectsInsecureOrOffOriginPayloads() {
        for(server in listOf("http://alarm.example.org",
            "https://alarm.example.org/path",
            "https://alice:pass@alarm.example.org",
            "https://alarm.example.org?token=something",
            "https://alarm.example.org/#fragment")) {
            assertThrows(IllegalArgumentException::class.java) {
                PairingQr.parse(ticket(server=server),now)
            }
        }
    }
    @Test fun rejectsUnknownFormatSchemaVersionOrMalformedCodes() {
        assertThrows(IllegalArgumentException::class.java) {
            PairingQr.parse("not QR",now)
        }
        assertThrows(IllegalArgumentException::class.java) {
            PairingQr.parse(ticket(codeValue="EPA-abc"),now)
        }
        val extra=JSONObject(ticket()).put("unexpected","danger").toString()
        assertThrows(IllegalArgumentException::class.java) {
            PairingQr.parse(extra,now)
        }
        val version=JSONObject(ticket()).put("version",2).toString()
        assertThrows(IllegalArgumentException::class.java) {
            PairingQr.parse(version,now)
        }
    }
}
