package knf.kuma.jobscheduler

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkRequest
import knf.kuma.App

fun networkConnectedConstraints(): Constraints = Constraints.Builder().apply { setRequiredNetworkType(NetworkType.CONNECTED) }.build()

fun WorkRequest.enqueue() = WorkManager.getInstance(App.context).enqueue(this)

fun PeriodicWorkRequest.enqueueUnique(tag: String, type: ExistingPeriodicWorkPolicy) = WorkManager.getInstance(App.context).enqueueUniquePeriodicWork(tag, type, this)