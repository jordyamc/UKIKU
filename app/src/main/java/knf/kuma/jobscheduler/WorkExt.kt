package knf.kuma.jobscheduler

import androidx.work.*
import knf.kuma.App

fun networkConnectedConstraints(): Constraints = Constraints.Builder().apply { setRequiredNetworkType(NetworkType.CONNECTED) }.build()

fun WorkRequest.enqueue() = WorkManager.getInstance(App.context).enqueue(this)

fun PeriodicWorkRequest.enqueueUnique(tag: String, type: ExistingPeriodicWorkPolicy) = WorkManager.getInstance(App.context).enqueueUniquePeriodicWork(tag, type, this)