package de.einsatzpulse.alarm

import java.security.MessageDigest

/** Never persist a bearer token in WorkManager input or notification metadata. */
object AlarmIdentity {
    fun fingerprint(server:String,credential:String):String {
        if(!server.startsWith("https://") || !credential.startsWith("epna_")) return ""
        val value="$server|$credential".toByteArray(Charsets.UTF_8)
        return MessageDigest.getInstance("SHA-256").digest(value)
            .joinToString("") { "%02x".format(it) }
    }

    fun dedupKey(fingerprint:String,tag:String):String = "shown:$fingerprint:$tag"

    fun workName(fingerprint:String,type:String,incidentId:Int,unitId:Int,
                 deviceId:Int,sentAtMs:Long):String {
        val scope=fingerprint.take(20)
        return if(type=="test") "test:$scope:$deviceId:$sentAtMs"
            else "alarm:$scope:$incidentId:$unitId"
    }
}
