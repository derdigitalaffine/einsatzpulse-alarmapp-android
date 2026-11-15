package de.einsatzpulse.alarm

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException

/**
 * Firebase deliberately returns quickly: long network verification is moved to
 * an expedited WorkManager job so process death doesn't silently drop alarms.
 */
class AlarmDeliveryWorker(context:Context,params:WorkerParameters):CoroutineWorker(context,params) {
    override suspend fun doWork():Result {
        val now=System.currentTimeMillis()
        val issued=inputData.getLong("sent_at_ms",0L)
        // Never raise a stale siren after background scheduling or offline recovery.
        if(issued<=0L || issued>now+30_000L || now-issued>120_000L) return Result.success()
        if(!SecureSession(applicationContext).linked) return Result.failure()

        val type=inputData.getString("type") ?: return Result.failure()
        val api=AlarmApi(applicationContext)
        val device=try {
            api.device()
        } catch(e:CancellationException) {throw e}
          catch(_:Exception) {return Result.retry()}
        if(!device.enabled) return Result.success()

        if(type=="test") {
            if(inputData.getInt("device_id",0)!=device.id) return Result.success()
            val key="test:"+device.id+":"+issued
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
        val key=AlarmNotifications.notificationTag(incidentId,unitId)
        if(AlarmNotifications.shownRecently(applicationContext,key)) return Result.success()

        // Verify *current* unit membership and incident state before revealing
        // keywords, location, sounds or attendance actions on the device.
        val dashboard=try {
            api.dashboard()
        } catch(e:CancellationException) {throw e}
          catch(_:Exception) {return Result.retry()}
        val incident=dashboard.incidents.firstOrNull {
            it.id==incidentId && it.unitId==unitId && it.status=="active"
        } ?: return Result.success()
        try { api.receipt(incidentId,unitId,"received") } catch(_:Exception) {}
        if(AlarmNotifications.show(applicationContext,incidentId,unitId,
                device,incident.keyword,incident.town)) {
            AlarmNotifications.markShown(applicationContext,key)
            try { api.receipt(incidentId,unitId,"shown") } catch(_:Exception) {}
        }
        return Result.success()
    }
}
