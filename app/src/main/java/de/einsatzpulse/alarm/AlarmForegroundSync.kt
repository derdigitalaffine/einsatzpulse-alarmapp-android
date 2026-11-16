package de.einsatzpulse.alarm

import android.content.Context
import java.time.OffsetDateTime

/**
 * Direct self-hosted status refresh: usable immediately while the app is OPEN.
 * Does not claim background delivery, unlike a configured FCM transport.
 */
object AlarmForegroundSync {
    fun recent(iso:String?,now:Long=System.currentTimeMillis()):Boolean {
        val stamp=try {OffsetDateTime.parse(iso ?: "").toInstant().toEpochMilli()}
            catch(_:Exception) {return false}
        return stamp>0L && stamp<=now+30_000 && now-stamp<=120_000
    }

    fun apply(context:Context,runtime:AlarmRuntime,device:AlarmDevice) {
        val session=SecureSession(context)
        val fingerprint=session.fingerprint
        if(!session.linked || fingerprint.isBlank() ||
           runtime.deviceId!=device.id || !device.enabled) return
        // The server generates this signal without Firebase, and it is
        // explicitly a TEST, not a dispatch. At most one local display.
        if(recent(runtime.testSignalAt)) {
            val key=AlarmIdentity.dedupKey(fingerprint,
                "poll-test:"+runtime.deviceId+":"+runtime.testSignalAt)
            if(!AlarmNotifications.shownRecently(context,key) &&
               AlarmNotifications.showTest(context,device)) {
                AlarmNotifications.markShown(context,key)
            }
        }
        if(runtime.testView) return // Never synthesize real notifications from fixtures.
        for(incident in runtime.incidents) {
            if(incident.demo || incident.id<=0 || incident.unitId<=0 ||
               incident.status!="active" || !recent(incident.createdAt)) continue
            val key=AlarmIdentity.dedupKey(fingerprint,
                AlarmNotifications.notificationTag(incident.id,incident.unitId))
            if(AlarmNotifications.shownRecently(context,key)) continue
            AlarmReceiptWorker.schedule(context,incident.id,incident.unitId,"received")
            if(AlarmNotifications.show(context,incident.id,incident.unitId,
                device,incident.keyword,incident.town,
                allowReply=runtime.kind=="person")) {
                AlarmNotifications.markShown(context,key)
                AlarmReceiptWorker.schedule(context,incident.id,incident.unitId,"shown")
            }
        }
    }
}
