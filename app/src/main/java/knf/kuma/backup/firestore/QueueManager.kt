package knf.kuma.backup.firestore

import knf.kuma.commons.noCrash
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

object QueueManager {
    private var list = mutableListOf<() -> Unit>()
    private var isRunning = false
    private var isClosed = false

    fun add(items: List<() -> Unit>) {
        if (isClosed) return
        if (isRunning)
            list.addAll(items)
        else
            run(items)
    }

    fun open() {
        isRunning = false
        isClosed = false
    }

    fun close() {
        isClosed = true
    }

    private fun run(items: List<() -> Unit> = list) {
        val tlist = ArrayList(items)
        list = mutableListOf()
        isRunning = true
        GlobalScope.launch(Dispatchers.IO) {
            tlist.forEach {
                if (isClosed) return@launch
                noCrash { it() }
            }
            if (list.isNotEmpty())
                run()
            else
                isRunning = false
        }
    }
}