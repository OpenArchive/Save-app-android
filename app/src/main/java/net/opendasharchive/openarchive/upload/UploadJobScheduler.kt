package net.opendasharchive.openarchive.upload

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.os.Build
import androidx.core.content.ContextCompat

interface UploadJobScheduler {
    fun schedule()
    fun cancel()
}

object UploadJobConfig {
    const val JOB_ID = 7918
}

class JobSchedulerUploadJobScheduler(
    private val appContext: Context
) : UploadJobScheduler {

    override fun schedule() {
        val jobScheduler =
            ContextCompat.getSystemService(appContext, JobScheduler::class.java) ?: return
        var jobBuilder = JobInfo.Builder(
            UploadJobConfig.JOB_ID,
            ComponentName(appContext, UploadService::class.java)
        ).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            jobBuilder = jobBuilder.setUserInitiated(true)
        }
        jobScheduler.schedule(jobBuilder.build())
    }

    override fun cancel() {
        val jobScheduler =
            ContextCompat.getSystemService(appContext, JobScheduler::class.java) ?: return
        jobScheduler.cancel(UploadJobConfig.JOB_ID)
    }
}
