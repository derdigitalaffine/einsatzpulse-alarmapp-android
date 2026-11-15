package de.einsatzpulse.alarm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class AlarmEnvelopeTest {
    @Test fun validatesFreshRealAlarm() {
        val now=1_800_000_000_000L
        val item=AlarmEnvelope.parse(mapOf("type" to "alarm",
            "incident_id" to "123","unit_id" to "45",
            "sent_at_ms" to (now-15_000L).toString()),now)
        assertNotNull(item)
        assertEquals(123,item!!.incidentId)
        assertEquals(45,item.unitId)
    }
    @Test fun filtersStaleInvalidOrIncompletePayloads() {
        val now=1_800_000_000_000L
        val candidates=listOf(
            mapOf("type" to "alarm","incident_id" to "2","unit_id" to "3",
                "sent_at_ms" to (now-121_000L).toString()),
            mapOf("type" to "alarm","incident_id" to "2","unit_id" to "3",
                "sent_at_ms" to (now+31_000L).toString()),
            mapOf("type" to "alarm","incident_id" to "0","unit_id" to "3",
                "sent_at_ms" to now.toString()),
            mapOf("type" to "alarm","incident_id" to "2","unit_id" to "-1",
                "sent_at_ms" to now.toString()),
            mapOf("type" to "test","device_id" to "0","sent_at_ms" to now.toString()),
            mapOf("type" to "unexpected","sent_at_ms" to now.toString()),
            mapOf("type" to "alarm","incident_id" to "2","unit_id" to "3"),
        )
        for(payload in candidates) assertNull(AlarmEnvelope.parse(payload,now))
        assertNotNull(AlarmEnvelope.parse(mapOf("type" to "test",
            "device_id" to "21","sent_at_ms" to now.toString()),now))
    }
    @Test fun notificationIdentitySeparatesSameIncidentAcrossUnits() {
        assertEquals("ep-alarm:7:8",AlarmNotifications.notificationTag(7,8))
        assertEquals("ep-alarm:7:9",AlarmNotifications.notificationTag(7,9))
    }
}
