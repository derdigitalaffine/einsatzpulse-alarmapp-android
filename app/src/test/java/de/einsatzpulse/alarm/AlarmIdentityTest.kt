package de.einsatzpulse.alarm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmIdentityTest {
    @Test fun accountScopedIdentityCannotMixSessionsOrServers() {
        val alpha=AlarmIdentity.fingerprint("https://server-a.example","epna_alpha")
        val beta=AlarmIdentity.fingerprint("https://server-a.example","epna_beta")
        val other=AlarmIdentity.fingerprint("https://server-b.example","epna_alpha")
        assertEquals(64,alpha.length)
        assertNotEquals(alpha,beta)
        assertNotEquals(alpha,other)
        assertEquals("",AlarmIdentity.fingerprint("http://insecure.example","epna_alpha"))
    }
    @Test fun sameIncidentDoesNotCollideAcrossAccountsOrUnits() {
        val a=AlarmIdentity.fingerprint("https://a.example","epna_first")
        val b=AlarmIdentity.fingerprint("https://a.example","epna_second")
        assertNotEquals(
            AlarmIdentity.workName(a,"alarm",42,7,0,1L),
            AlarmIdentity.workName(b,"alarm",42,7,0,1L))
        assertNotEquals(
            AlarmIdentity.workName(a,"alarm",42,7,0,1L),
            AlarmIdentity.workName(a,"alarm",42,8,0,1L))
        assertNotEquals(
            AlarmIdentity.dedupKey(a,"ep-alarm:42:7"),
            AlarmIdentity.dedupKey(b,"ep-alarm:42:7"))
        assertTrue(AlarmIdentity.workName(a,"test",0,0,5,123L)
            .contains("test:"))
    }
}
