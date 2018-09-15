package knf.kuma.backup.objects

import java.text.SimpleDateFormat
import java.util.*

open class BackupObject<T> {
    var date: String? = null
    var data: MutableList<T>? = null

    constructor()

    constructor(data: MutableList<T>) {
        this.date = SimpleDateFormat("dd/MM/yyyy kk:mm", Locale.getDefault()).format(Calendar.getInstance().time)
        this.data = data
    }
}
