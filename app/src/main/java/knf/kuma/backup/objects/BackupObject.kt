package knf.kuma.backup.objects

import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

open class BackupObject<T> {
    @SerializedName("version")
    var version: Int? = null
    @SerializedName("date")
    var date: String? = null
    @SerializedName("data")
    var data: List<T>? = null

    constructor()

    constructor(data: List<T>) {
        this.version = BACKUP_VERSION
        this.date = SimpleDateFormat("dd/MM/yyyy kk:mm", Locale.getDefault()).format(Calendar.getInstance().time)
        this.data = data
    }

    companion object {
        const val BACKUP_VERSION = 2
    }
}
