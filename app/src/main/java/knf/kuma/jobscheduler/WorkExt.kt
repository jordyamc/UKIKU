package knf.kuma.jobscheduler

import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.WorkManager
import androidx.work.WorkRequest

fun networkConnectedConstraints(): Constraints = Constraints.Builder().apply { setRequiredNetworkType(NetworkType.CONNECTED) }.build()

fun WorkRequest.enqueue() = WorkManager.getInstance().enqueue(this)