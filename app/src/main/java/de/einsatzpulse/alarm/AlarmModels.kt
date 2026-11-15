package de.einsatzpulse.alarm

import org.json.JSONObject

data class AlarmUnit(val id: Int,val name: String,val organisation: String,
                     val availability: String)
data class AlarmIncident(
    val id: Int,val unitId: Int,val unitName: String,val number: String,
    val keyword: String,val summary: String,val town: String,
    val street: String,val status: String,val alarmAt: String,
    val reply: String,val yes: Int,val delayed: Int,val no: Int
) {
    val address: String get() = listOf(street,town).filter { it.isNotBlank() }.joinToString(" · ")
}
data class AlarmDevice(
    val id: Int,val label: String,val enabled: Boolean,val tone: String,
    val bypassDnd: Boolean,val vibrate: Boolean,val pushRegistered: Boolean
) {
    companion object {
        fun from(value: JSONObject): AlarmDevice {
            return AlarmDevice(value.optInt("id"),value.optString("label","Android"),
                value.optBoolean("enabled",true),value.optString("tone","signal"),
                value.optBoolean("bypass_dnd"),value.optBoolean("vibrate",true),
                value.optBoolean("push_registered"))
        }
    }
}
data class AlarmDashboard(val incidents: List<AlarmIncident>,val units: List<AlarmUnit>) {
    companion object {
        fun from(value: JSONObject): AlarmDashboard {
            val units = value.optJSONArray("units")
            val incidents = value.optJSONArray("incidents")
            return AlarmDashboard(
                (0 until (incidents?.length() ?: 0)).mapNotNull { index ->
                    val item = incidents?.optJSONObject(index) ?: return@mapNotNull null
                    val counts=item.optJSONObject("responses")
                    AlarmIncident(item.optInt("id"),item.optInt("unit_id"),
                        item.optString("unit_name"),item.optString("number"),
                        item.optString("keyword"),item.optString("summary"),
                        item.optString("town"),
                        listOf(item.optString("street"),item.optString("house_number"))
                            .filter { it.isNotBlank() }.joinToString(" "),
                        item.optString("status"),item.optString("alarm_at").takeIf { it!="null" } ?: "",
                        item.optJSONObject("my_response")?.optString("status") ?: "",
                        counts?.optInt("yes")?:0, counts?.optInt("delayed")?:0, counts?.optInt("no")?:0)
                },
                (0 until (units?.length() ?: 0)).mapNotNull { index ->
                    val item=units?.optJSONObject(index) ?: return@mapNotNull null
                    AlarmUnit(item.optInt("id"),item.optString("name"),
                        item.optString("organisation_name"),
                        item.optJSONObject("availability")?.optString("status") ?: "")
                })
        }
    }
}
