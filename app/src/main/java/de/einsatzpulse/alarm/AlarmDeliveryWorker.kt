package de.einsatzpulse.alarm

import android.content.Context
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException

/**
 * Firebase deliberately returns quickly: long network verification is moved to
 * an expedited WorkManager job so process death doesn't silently drop alarms.
 */
class AlarmDeliveryWorker(context:Context,params:WorkerParameters):CoroutineWorker(context,params) {
    /** Required for expedited WorkManager tasks on Android 8–11. */
    override suspend fun getForegroundInfo():ForegroundInfo {
        val manager=applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val id="ep_alarm_processing_v1"
        if(manager.getNotificationChannel(id)==null) {
            manager.createNotificationChannel(NotificationChannel(id,
                "EinsatzPulse · Alarmprüfung",NotificationManager.IMPORTANCE_LOW).apply {
                description="Kurzzeitige Hintergrundprüfung eines zusätzlichen Alarmhinweises"
                setSound(null,null)
            })
        }
        val notification=NotificationCompat.Builder(applicationContext,id)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle("EinsatzPulse prüft einen Alarmhinweis")
            .setContentText("Berechtigung und Einsatzzuordnung werden überprüft")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true).build()
        return ForegroundInfo(47007,notification)
    }

    override suspend fun doWork():Result {
        val now=System.currentTimeMillis()
        val issued=inputData.getLong("sent_at_ms",0L)
        // Never raise a stale siren after background scheduling or offline recovery.
        if(issued<=0L || issued>now+30_000L || now-issued>120_000L) return Result.success()
        val session=SecureSession(applicationContext)
        val identity=inputData.getString("session_key") ?: return Result.failure()
        if(!session.linked || session.fingerprint!=identity) return Result.success()

        val type=inputData.getString("type") ?: return Result.failure()
        val api=AlarmApi(applicationContext)
        val device=try {
            api.device()
        } catch(e:CancellationException) {throw e}
          catch(_:Exception) {return Result.retry()}
        if(!device.enabled) return Result.success()

        if(type=="test") {
            if(inputData.getInt("device_id",0)!=device.id) return Result.success()
            if(SecureSession(applicationContext).fingerprint!=identity) return Result.success()
            val key=AlarmIdentity.dedupKey(identity,"test:"+device.id+":"+issued)
            if(AlarmNotifications.shownRecently(applicationContext,key)) return Result.success()
            if(AlarmNotifications.showTest(applicationContext,device)) {
                AlarmNotifications.markShown(applicationContext,key)
            }
            return Result.success()
        }
        if(type!="alarm") return Result.failure()
        val incidentId=inputData.getInt("incident_id",0)
        val unitId=inputData.getInt("unit_id",0)
        if(incidentId<=0 || unitId<=0) return Result.failure()
        val key=AlarmIdentity.dedupKey(identity,
            AlarmNotifications.notificationTag(incidentId,unitId))
        if(AlarmNotifications.shownRecently(applicationContext,key)) return Result.success()

        // Verify *current* unit membership and incident state before revealing
        // keywords, location, sounds or attendance actions on the device.
        val runtime=try {
            api.runtime()
        } catch(e:CancellationException) {throw e}
          catch(_:Exception) {return Result.retry()}
        if(SecureSession(applicationContext).fingerprint!=identity) return Result.success()
        val incident=runtime.incidents.firstOrNull {
            it.id==incidentId && it.unitId==unitId && it.status=="active" && !it.demo
        } ?: return Result.success()
        AlarmReceiptWorker.schedule(applicationContext,incidentId,unitId,"received")
        if(AlarmNotifications.show(applicationContext,incidentId,unitId,
                device,incident.keyword,incident.town,
                allowReply=runtime.kind=="person")) {
            AlarmNotifications.markShown(applicationContext,key)
            AlarmReceiptWorker.schedule(applicationContext,incidentId,unitId,"shown")
        }
        return Result.success()
    }
}
