package knf.kuma.pojos

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import androidx.preference.PreferenceManager
import com.google.gson.annotations.SerializedName
import com.jaredrummler.android.device.DeviceName
import knf.kuma.backup.objects.BackupObject
import knf.kuma.commons.noCrashLet

open class AutoBackupObject() : BackupObject<Any>() {
    @SerializedName("name")
    var name: String? = ""
    @SerializedName("device_id")
    var device_id: String? = ""
    @SerializedName("value")
    var value: String? = null

    @SuppressLint("HardwareIds")
    constructor(context: Context?) : this() {
        if (context != null) {
            this.name = DeviceName.getDeviceName()
            this.device_id = Settings.Secure.getString(context.applicationContext.contentResolver, Settings.Secure.ANDROID_ID)
            this.value = PreferenceManager.getDefaultSharedPreferences(context).getString("auto_backup", "0")
        }
    }

    @SuppressLint("HardwareIds")
    constructor(context: Context?, newValue: String?) : this() {
        if (context != null) {
            this.name = DeviceName.getDeviceName()
            this.device_id = Settings.Secure.getString(context.applicationContext.contentResolver, Settings.Secure.ANDROID_ID)
            this.value = newValue
        }
    }

    override fun toString(): String {
        return "$name ID: $device_id"
    }

    override fun hashCode(): Int {
        return (name + device_id).hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return noCrashLet {
            other is AutoBackupObject &&
                    name == other.name &&
                    device_id == other.device_id
        } ?: false
    }
}
