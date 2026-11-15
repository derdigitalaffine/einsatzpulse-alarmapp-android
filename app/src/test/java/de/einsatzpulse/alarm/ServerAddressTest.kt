package de.einsatzpulse.alarm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ServerAddressTest {
    @Test fun httpsOnly() {
        assertEquals("https://einsatz.example.de:8443",
            normalizeServerAddress(" https://einsatz.example.de:8443/ "))
        for (value in listOf("http://example.org","https://user:pass@example.org",
            "https://example.org/subpath","https://example.org/#token",
            "https://example.org/?token=yes","not-a-url","https://")) {
            assertThrows(IllegalArgumentException::class.java) { normalizeServerAddress(value) }
        }
    }
}
