package de.einsatzpulse.alarm

import android.content.Intent
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

/**
 * FCM payload contains IDs only. Enriched details must be retrieved using
 * current device auth. Provider acceptance is not a receipt.
 */
class AlarmFirebaseService: FirebaseMessagingService() {
    override fun onNewToken(token:String) {
        if(!SecureSession(this).linked) return
        CoroutineScope(SupervisorJob()+Dispatchers.IO).launch {
            runCatching { AlarmApi(applicationContext).registerPushToken(token) }
        }
    }
    override fun onMessageReceived(message:RemoteMessage) {
        if(message.data["type"]!="alarm" || !SecureSession(this).linked) return
        val incidentId=message.data["incident_id"]?.toIntOrNull()?.takeIf { it>0 } ?: return
        val unitId=message.data["unit_id"]?.toIntOrNull()?.takeIf { it>0 } ?: return
        runBlocking {
            withTimeoutOrNull(11000) {
                val api=AlarmApi(applicationContext)
                // Don't display sensitive information if server verification is unavailable.
                val config=runCatching { api.device() }.getOrNull() ?: return@withTimeoutOrNull
                if(!config.enabled) return@withTimeoutOrNull
                val dashboard=runCatching { api.dashboard() }.getOrNull()
                // If online, actively reject a stale or unauthorized push.
                val matching=dashboard?.incidents?.firstOrNull { it.id==incidentId && it.unitId==unitId && it.status=="active" }
                if(dashboard!=null && matching==null) return@withTimeoutOrNull
                runCatching { api.receipt(incidentId,unitId,"received") }
                val shown=AlarmNotifications.show(applicationContext,incidentId,unitId,config,
                    matching?.keyword?:"",matching?.town?:"")
                if(shown) runCatching { api.receipt(incidentId,unitId,"shown") }
            }
        }
    }
}

class AlarmActionReceiver:android.content.BroadcastReceiver() {
    override fun onReceive(context:android.content.Context,intent:Intent) {
        val incident=intent.getIntExtra("incident_id",0)
        val unit=intent.getIntExtra("unit_id",0)
        val status=intent.getStringExtra("status")?:""
        if(incident<=0 || unit<=0 || status !in setOf("yes","no","delayed")) return
        val pending=goAsync()
        CoroutineScope(SupervisorJob()+Dispatchers.IO).launch {
            try {
                val confirmed=AlarmApi(context.applicationContext).reply(incident,unit,status)
                if(confirmed) {
                    (context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager)
                        .cancel(incident)
                }
            } catch (_:Exception) {
                // Keep notification for manual retry; never claim an unconfirmed reply.
            } finally { pending.finish() }
        }
    }
}
