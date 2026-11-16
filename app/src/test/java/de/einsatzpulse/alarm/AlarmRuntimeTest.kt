package de.einsatzpulse.alarm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

class AlarmRuntimeTest {
    @Test fun parsesManagedHallAndOnlyItsAssignedUnits() {
        val data=JSONObject().put("device_id",21)
            .put("kind","hall").put("display_kind","hall")
            .put("test_view",false).put("fcm_configured",false)
            .put("server_time","2026-11-16T02:00:00Z")
            .put("units",JSONArray().put(JSONObject().put("id",9)
                .put("name","Wache A").put("organisation","Test")))
            .put("incidents",JSONArray().put(JSONObject().put("id",5)
                .put("unit_id",9).put("unit_name","Wache A").put("status","active")
                .put("created_at","2026-11-16T02:00:00Z")))
        val runtime=AlarmRuntime.from(data)
        assertEquals("hall",runtime.kind)
        assertEquals(9,runtime.units.single().id)
        assertEquals(9,runtime.incidents.single().unitId)
        assertFalse(runtime.fcmConfigured)
    }
    @Test fun rejectsUnknownRoleAndDoesNotTreatDemoAsActive() {
        val data=JSONObject().put("device_id",2).put("kind","superadmin")
            .put("display_kind","hall")
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            AlarmRuntime.from(data)
        }
    }
    @Test fun neverRingsOldPollResult() {
        val now=Instant.parse("2026-11-16T02:00:00Z").toEpochMilli()
        assertTrue(AlarmForegroundSync.recent("2026-11-16T01:59:00Z",now))
        assertFalse(AlarmForegroundSync.recent("2026-11-16T01:50:00Z",now))
        assertFalse(AlarmForegroundSync.recent("2026-11-16T02:02:00Z",now))
        assertFalse(AlarmForegroundSync.recent(null,now))
    }
}
