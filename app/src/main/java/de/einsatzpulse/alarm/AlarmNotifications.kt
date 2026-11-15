package de.einsatzpulse.alarm

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

object AlarmNotifications {
    fun notificationTag(incidentId:Int,unitId:Int):String =
        "ep-alarm:"+incidentId+":"+unitId

    fun shownRecently(context:Context,key:String,now:Long=System.currentTimeMillis()):Boolean {
        val last=context.getSharedPreferences("ep_alarm_shown_v1",Context.MODE_PRIVATE)
            .getLong(key,0L)
        return last>0L && now>=last && now-last<24*60*60*1000L
    }
    fun markShown(context:Context,key:String,now:Long=System.currentTimeMillis()) {
        context.getSharedPreferences("ep_alarm_shown_v1",Context.MODE_PRIVATE)
            .edit().putLong(key,now).apply()
    }
    fun soundResource(tone:String):Int = when(tone) {
        "siren" -> R.raw.siren
        "chime" -> R.raw.chime
        "urgent" -> R.raw.urgent
        else -> R.raw.signal
    }
    fun preview(context:Context,tone:String) {
        val player=MediaPlayer.create(context,soundResource(tone)) ?: return
        player.setOnCompletionListener { it.release() }
        try { player.start() } catch (_:Exception) { player.release() }
    }
    fun dndGranted(context:Context):Boolean =
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .isNotificationPolicyAccessGranted
    private fun channel(context:Context,config:AlarmDevice):String {
        val manager=context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val wantsDnd=config.bypassDnd && manager.isNotificationPolicyAccessGranted
        // Settings are immutable after creation. Never delete or overwrite a user-edited channel.
        val key="alert_v1_${config.tone}_${if(wantsDnd)"priority" else "normal"}_${if(config.vibrate)"vib" else "silent"}"
        if(manager.getNotificationChannel(key)==null) {
            val channel=NotificationChannel(key,"Einsatzalarm · ${config.tone}",
                NotificationManager.IMPORTANCE_HIGH).apply {
                description="Zusatzalarmierung zu EinsatzPulse – keine amtliche Alarmierung"
                lockscreenVisibility=Notification.VISIBILITY_PRIVATE
                enableVibration(config.vibrate)
                if(config.vibrate) vibrationPattern=longArrayOf(0,500,220,650,250,700)
                val sound=Uri.parse("android.resource://${context.packageName}/${soundResource(config.tone)}")
                setSound(sound,AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                if(wantsDnd) setBypassDnd(true)
            }
            manager.createNotificationChannel(channel)
        }
        return key
    }
    fun cancel(context:Context,incidentId:Int,unitId:Int) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .cancel(notificationTag(incidentId,unitId),0)
    }

    fun showTest(context:Context,config:AlarmDevice):Boolean {
        if(!config.enabled) return false
        if(Build.VERSION.SDK_INT>=33 &&
            ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            return false
        val key=channel(context,config)
        val notification=NotificationCompat.Builder(context,key)
            .setSmallIcon(R.drawable.ic_alarm)
            .setColor(0xFFE64E54.toInt())
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentTitle("TESTALARM – KEIN EINSATZ")
            .setContentText("EinsatzPulse · Nur ein Funktionstest des Alarmtons")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Dies ist ein TEST. Es wurde kein Einsatz ausgelöst."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .setContentIntent(PendingIntent.getActivity(context,19768,
                Intent(context,MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            .build()
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify("ep-test-alarm",0,notification)
        return true
    }

    fun show(context:Context,incidentId:Int,unitId:Int,config:AlarmDevice,
             keyword:String="",town:String=""):Boolean {
        if(!config.enabled) return false
        if(Build.VERSION.SDK_INT>=33 &&
            ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            return false
        val channelId=channel(context,config)
        val openIntent=Intent(context,MainActivity::class.java)
            .putExtra("incident_id",incidentId).putExtra("unit_id",unitId)
            .setData(Uri.parse("einsatzpulse://alarm/"+incidentId+"/"+unitId+"/open"))
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val open=PendingIntent.getActivity(context,notificationTag(incidentId,unitId).hashCode(),
            openIntent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val title=if(keyword.isBlank())"EinsatzPulse: Alarmhinweis" else keyword
        val body=if(town.isBlank())"Einsatzmeldung – App öffnen und prüfen" else town
        val builder=NotificationCompat.Builder(context,channelId)
            .setSmallIcon(R.drawable.ic_alarm).setColor(0xFFE64E54.toInt())
            .setContentTitle(title).setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(open).setAutoCancel(true)
        listOf("yes" to "Ich komme","delayed" to "Später","no" to "Ich komme nicht")
            .forEachIndexed { index,action ->
                val intent=Intent(context,AlarmActionReceiver::class.java)
                    .putExtra("incident_id",incidentId).putExtra("unit_id",unitId)
                    .putExtra("status",action.first)
                    .setData(Uri.parse("einsatzpulse://alarm/"+incidentId+"/"+unitId+"/"+action.first))
                val pending=PendingIntent.getBroadcast(context,
                    (notificationTag(incidentId,unitId)+":"+index).hashCode(),intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                builder.addAction(0,action.second,pending)
            }
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(notificationTag(incidentId,unitId),0,builder.build())
        return true
    }
}
