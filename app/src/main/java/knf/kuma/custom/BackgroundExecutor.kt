package knf.kuma.custom

import android.os.AsyncTask
import java.util.concurrent.Executor

class BackgroundExecutor : Executor {
    override fun execute(command: Runnable?) {
        AsyncTask.execute(command)
    }
}