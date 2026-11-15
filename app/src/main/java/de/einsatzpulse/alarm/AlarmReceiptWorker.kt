package de.einsatzpulse.alarm

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit

/** Persist displayed/received/opened evidence until Django acknowledges it. */
class AlarmReceiptWorker(context:Context,params:WorkerParameters):CoroutineWorker(context,params) {
    override suspend fun doWork():Result {
        val session=SecureSession(applicationContext)
        val identity=inputData.getString("session_key") ?: return Result.failure()
        if(!session.linked || session.fingerprint!=identity) return Result.success()
        val incident=inputData.getInt("incident_id",0)
        val unit=inputData.getInt("unit_id",0)
        val event=inputData.getString("event") ?: return Result.failure()
        if(incident<=0 || unit<=0 || event !in setOf("received","shown","opened"))
            return Result.failure()
        try {
            AlarmApi(applicationContext).receipt(incident,unit,event)
        } catch(e:CancellationException) {
            throw e
        } catch(e:AlarmHttpException) {
            // Permanent HTTP errors (revoked membership / missing unit) should
            // not create an unbounded background retry loop.
            if(e.statusCode!=429 && e.statusCode<500) return Result.failure()
            return if(runAttemptCount<7) Result.retry() else Result.failure()
        } catch(_:Exception) {
            return if(runAttemptCount<7) Result.retry() else Result.failure()
        }
        return Result.success()
    }

    companion object {
        fun schedule(context:Context,incidentId:Int,unitId:Int,event:String) {
            if(incidentId<=0 || unitId<=0 || event !in setOf("received","shown","opened")) return
            val session=SecureSession(context)
            if(!session.linked) return
            val fingerprint=session.fingerprint
            if(fingerprint.isBlank()) return
            val data=Data.Builder().putInt("incident_id",incidentId)
                .putInt("unit_id",unitId)
                .putString("event",event)
                .putString("session_key",fingerprint).build()
            val job=OneTimeWorkRequestBuilder<AlarmReceiptWorker>()
                .setInputData(data)
                .setConstraints(Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30,TimeUnit.SECONDS)
                .build()
            val key="receipt:"+fingerprint.take(20)+":$incidentId:$unitId:$event"
            WorkManager.getInstance(context.applicationContext)
                .enqueueUniqueWork(key,ExistingWorkPolicy.KEEP,job)
        }
    }
}
