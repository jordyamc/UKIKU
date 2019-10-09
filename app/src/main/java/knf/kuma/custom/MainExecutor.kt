package knf.kuma.custom

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executor

class MainExecutor : Executor {
    override fun execute(command: Runnable?) {
        command ?: return
        Handler(Looper.getMainLooper()).post(command)
    }
}