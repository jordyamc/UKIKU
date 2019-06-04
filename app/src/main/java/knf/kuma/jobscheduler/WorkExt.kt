package knf.kuma.jobscheduler

import androidx.work.*

fun networkConnectedConstraints(): Constraints = Constraints.Builder().apply { setRequiredNetworkType(NetworkType.CONNECTED) }.build()

fun WorkRequest.enqueue() = WorkManager.getInstance().enqueue(this)

fun PeriodicWorkRequest.enqueueUnique(tag: String, type: ExistingPeriodicWorkPolicy) = WorkManager.getInstance().enqueueUniquePeriodicWork(tag, type, this)