package org.jin.calenee.chat.notification

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.orhanobut.logger.Logger

class CaleneeWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    override fun doWork(): Result {
        Logger.d("doWork()")
        return Result.success()
    }
}