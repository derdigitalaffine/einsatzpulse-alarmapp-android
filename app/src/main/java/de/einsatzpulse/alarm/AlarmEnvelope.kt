package de.einsatzpulse.alarm

/** Validate ID-only FCM messages and reject delayed notifications beyond server TTL. */
data class AlarmEnvelope(val type:String,val incidentId:Int,val unitId:Int,
                         val deviceId:Int,val sentAtMs:Long) {
    companion object {
        fun parse(data:Map<String,String>,now:Long=System.currentTimeMillis()):AlarmEnvelope? {
            val kind=data["type"] ?: return null
            if(kind!="alarm" && kind!="test") return null
            val sent=data["sent_at_ms"]?.toLongOrNull() ?: return null
            if(sent<=0L || sent>now+30_000L || now-sent>120_000L) return null
            val incident=data["incident_id"]?.toIntOrNull() ?: 0
            val unit=data["unit_id"]?.toIntOrNull() ?: 0
            val device=data["device_id"]?.toIntOrNull() ?: 0
            if(kind=="alarm" && (incident<=0 || unit<=0)) return null
            if(kind=="test" && device<=0) return null
            return AlarmEnvelope(kind,incident,unit,device,sent)
        }
    }
}
