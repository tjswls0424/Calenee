package org.jin.calenee.chat.notification

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters

class CaleneeWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    override fun doWork(): Result {
        Log.d("fcm_test/doWork","doWork()")
        return Result.success()
    }
}