package de.einsatzpulse.alarm

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class AlarmHttpException(val statusCode:Int,message:String):IOException(message)

class AlarmApi(context: Context) {
    private val session = SecureSession(context)
    private val http = OkHttpClient.Builder()
        .connectTimeout(8,TimeUnit.SECONDS).readTimeout(10,TimeUnit.SECONDS)
        .callTimeout(16,TimeUnit.SECONDS).build()
    private val jsonType = "application/json; charset=utf-8".toMediaType()
    private suspend fun call(server: String, path: String, method: String="GET",
                             payload: JSONObject?=null, bearer: String=session.token): JSONObject =
        withContext(Dispatchers.IO) {
            val target=normalizeServerAddress(server)
            require(path.startsWith("/alarm/")) { "Kein Alarm-API-Pfad" }
            val builder=Request.Builder().url(target+"/api"+path)
                .header("Accept","application/json").header("Cache-Control","no-store")
            if (bearer.isNotBlank()) builder.header("Authorization","Bearer "+bearer)
            val body=payload?.toString()?.toRequestBody(jsonType)
            when (method) {
                "GET" -> builder.get()
                "POST" -> builder.post(body?:JSONObject().toString().toRequestBody(jsonType))
                "PUT" -> builder.put(body?:JSONObject().toString().toRequestBody(jsonType))
                else -> throw IllegalArgumentException("Methode nicht unterstützt")
            }
            http.newCall(builder.build()).execute().use { response ->
                val result=JSONObject(response.body?.string() ?: "{}")
                if (!response.isSuccessful)
                    throw AlarmHttpException(response.code,result.optString("error","HTTP "+response.code))
                result
            }
        }
    suspend fun activate(server: String, code: String, label: String): AlarmDevice {
        val origin=normalizeServerAddress(server)
        val result=call(origin,"/alarm/native/activate/","POST",
            JSONObject().put("code",code.trim()).put("label",label.trim()),bearer="")
        val credential=result.getString("token")
        val device=AlarmDevice.from(result.getJSONObject("device"))
        session.save(origin,credential)
        return device
    }
    suspend fun unlink():Boolean {
        if(!session.linked) return false
        return call(session.server,"/alarm/native/unlink/","POST")
            .optBoolean("revoked",false)
    }

    suspend fun device(): AlarmDevice =
        AlarmDevice.from(call(session.server,"/alarm/native/me/").getJSONObject("device"))
    suspend fun dashboard(): AlarmDashboard =
        AlarmDashboard.from(call(session.server,"/alarm/me/"))
    suspend fun reply(incidentId:Int,unitId:Int,status:String,eta:Int?=null): Boolean {
        require(status in setOf("yes","no","delayed","unknown"))
        val payload=JSONObject().put("status",status)
        if (eta!=null && status in setOf("yes","delayed")) payload.put("eta_minutes",eta)
        val result=call(session.server,
            "/alarm/incidents/$incidentId/units/$unitId/reply/","PUT",payload)
        return result.optBoolean("confirmed",false)
    }
    suspend fun availability(unitId:Int,status:String): Boolean {
        require(status in setOf("available","limited","unavailable"))
        return call(session.server,"/alarm/units/$unitId/availability/","PUT",
            JSONObject().put("status",status)).optBoolean("confirmed",false)
    }
    suspend fun registerPushToken(token:String) {
        if (!session.linked || token.isBlank()) return
        call(session.server,"/alarm/native/token/","PUT",JSONObject().put("token",token))
    }
    suspend fun receipt(incidentId:Int,unitId:Int,event:String) {
        if (!session.linked || event !in setOf("received","shown","opened")) return
        call(session.server,"/alarm/native/receipt/","POST",
            JSONObject().put("incident_id",incidentId).put("unit_id",unitId).put("event",event))
    }
}
