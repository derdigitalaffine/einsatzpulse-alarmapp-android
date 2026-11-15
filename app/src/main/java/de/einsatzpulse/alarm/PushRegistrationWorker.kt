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
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * FCM token rotation must survive service process termination and offline
 * registration. This never writes the raw Firebase token into WorkManager Data.
 */
class PushRegistrationWorker(context:Context,params:WorkerParameters):CoroutineWorker(context,params) {
    override suspend fun doWork():Result {
        val session=SecureSession(applicationContext)
        val identity=inputData.getString("session_key") ?: return Result.failure()
        if(!session.linked || session.fingerprint!=identity) return Result.success()
        if(FirebaseApp.getApps(applicationContext).isEmpty()) return Result.success()
        val token=try {
            withContext(Dispatchers.IO) {
                Tasks.await(FirebaseMessaging.getInstance().token,15,TimeUnit.SECONDS)
            }
        } catch(e:CancellationException) {throw e}
          catch(_:Exception) {return retryOrFail()}
        if(token.isBlank() || SecureSession(applicationContext).fingerprint!=identity)
            return Result.success()
        try {
            AlarmApi(applicationContext).registerPushToken(token)
        } catch(e:CancellationException) {
            throw e
        } catch(e:AlarmHttpException) {
            if(e.statusCode in 400..499 && e.statusCode!=429) return Result.failure()
            return retryOrFail()
        } catch(_:Exception) {
            return retryOrFail()
        }
        applicationContext.getSharedPreferences("ep_alarm_push_status",Context.MODE_PRIVATE)
            .edit().putLong(identity, System.currentTimeMillis()).apply()
        return Result.success()
    }

    private fun retryOrFail():Result =
        if(runAttemptCount<12) Result.retry() else Result.failure()

    companion object {
        fun lastSyncedAt(context:Context):Long {
            val key=SecureSession(context).fingerprint
            return if(key.isBlank()) 0L else
                context.getSharedPreferences("ep_alarm_push_status",Context.MODE_PRIVATE)
                    .getLong(key,0L)
        }

        fun enqueue(context:Context) {
            val session=SecureSession(context)
            if(!session.linked || FirebaseApp.getApps(context).isEmpty()) return
            val fingerprint=session.fingerprint
            if(fingerprint.isBlank()) return
            val job=OneTimeWorkRequestBuilder<PushRegistrationWorker>()
                .setInputData(Data.Builder().putString("session_key",fingerprint).build())
                .setConstraints(Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30,TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context.applicationContext)
                .enqueueUniqueWork("fcm-register:"+fingerprint.take(20),
                    ExistingWorkPolicy.KEEP,job)
        }
    }
}
