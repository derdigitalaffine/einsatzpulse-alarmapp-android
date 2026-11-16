package de.einsatzpulse.alarm

import org.json.JSONObject

/** Authoritative role and limited incident scope from the paired server. */
data class RuntimeIncident(
    val id:Int,val unitId:Int,val unitName:String,
    val number:String,val keyword:String,val town:String,val street:String,
    val status:String,val createdAt:String,val demo:Boolean
)

data class RuntimeUnit(val id:Int,val name:String,val organisation:String)
data class RuntimeVehicle(val id:Int,val name:String,val callsign:String)

data class AlarmRuntime(
    val deviceId:Int,val kind:String,val displayKind:String,
    val testView:Boolean,val testExpiresAt:String?,
    val testSignalAt:String?,
    val vehicle:RuntimeVehicle?,val units:List<RuntimeUnit>,
    val incidents:List<RuntimeIncident>,val fcmConfigured:Boolean,
    val serverTime:String
) {
    companion object {
        fun from(data:JSONObject):AlarmRuntime {
            val allowed=setOf("person","hall","vehicle")
            val kind=data.getString("kind")
            val display=data.getString("display_kind")
            require(kind in allowed && display in allowed) {"Unbekannter Android-Gerätemodus"}
            val units=data.optJSONArray("units")
            val incidents=data.optJSONArray("incidents")
            val vehicle=data.optJSONObject("vehicle")
            return AlarmRuntime(
                data.getInt("device_id"),kind,display,
                data.optBoolean("test_view",false),
                data.optString("test_expires_at").takeIf { it!="" && it!="null" },
                data.optString("test_signal_at").takeIf { it!="" && it!="null" },
                vehicle?.let {RuntimeVehicle(it.getInt("id"),it.optString("name"),
                    it.optString("callsign"))},
                (0 until (units?.length() ?: 0)).mapNotNull { i ->
                    units?.optJSONObject(i)?.let {
                        RuntimeUnit(it.getInt("id"),it.optString("name"),
                            it.optString("organisation"))
                    }
                },
                (0 until (incidents?.length() ?: 0)).mapNotNull { i ->
                    incidents?.optJSONObject(i)?.let {
                        RuntimeIncident(it.getInt("id"),it.getInt("unit_id"),
                            it.optString("unit_name"),it.optString("number"),
                            it.optString("keyword"),it.optString("town"),
                            it.optString("street"),it.optString("status"),
                            it.optString("created_at"),it.optBoolean("demo",false))
                    }
                },
                data.optBoolean("fcm_configured",false),data.optString("server_time")
            )
        }
    }
}
