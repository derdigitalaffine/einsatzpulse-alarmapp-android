package de.einsatzpulse.alarm

import android.content.Intent
import android.content.Context
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import androidx.work.BackoffPolicy
import androidx.work.Data
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class AlarmFirebaseService:FirebaseMessagingService() {
    override fun onNewToken(token:String) {
        // Persist retry intent, not a network request tied to the short
        // FirebaseMessagingService callback lifetime.
        if(token.isNotBlank()) PushRegistrationWorker.enqueue(applicationContext)
    }

    override fun onMessageReceived(message:RemoteMessage) {
        val session=SecureSession(this)
        if(!session.linked) return
        val fingerprint=session.fingerprint
        if(fingerprint.isBlank()) return
        val data=AlarmEnvelope.parse(message.data) ?: return
        val builder=Data.Builder()
            .putString("type",data.type)
            .putString("session_key",fingerprint)
            .putLong("sent_at_ms",data.sentAtMs)
            .putInt("incident_id",data.incidentId)
            .putInt("unit_id",data.unitId)
            .putInt("device_id",data.deviceId)
        val work=OneTimeWorkRequestBuilder<AlarmDeliveryWorker>()
            .setInputData(builder.build())
            .setConstraints(Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL,20,TimeUnit.SECONDS)
            .apply {
                if(message.originalPriority==RemoteMessage.PRIORITY_HIGH)
                    setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            }.build()
        val identity=AlarmIdentity.workName(fingerprint,data.type,
            data.incidentId,data.unitId,data.deviceId,data.sentAtMs)
        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            identity,ExistingWorkPolicy.KEEP,work)
    }
}

class AlarmActionReceiver:android.content.BroadcastReceiver() {
    override fun onReceive(context:Context,intent:Intent) {
        val incident=intent.getIntExtra("incident_id",0)
        val unit=intent.getIntExtra("unit_id",0)
        val status=intent.getStringExtra("status")?:""
        if(incident<=0 || unit<=0 || status !in setOf("yes","no","delayed")) return
        // PendingIntent may outlive a locally removed or re-paired account.
        val session=SecureSession(context)
        if(!session.linked ||
           intent.getStringExtra("session_key")!=session.fingerprint) return
        val pending=goAsync()
        CoroutineScope(SupervisorJob()+Dispatchers.IO).launch {
            try {
                val confirmed=kotlinx.coroutines.withTimeoutOrNull(8000) {
                    AlarmApi(context.applicationContext).reply(incident,unit,status)
                } ?: false
                if(confirmed) {
                    AlarmNotifications.cancel(context,incident,unit)
                }
            } catch (_:Exception) {
                // Keep the original notification. The app never claims a reply
                // was confirmed until Django responds successfully.
            } finally { pending.finish() }
        }
    }
}
