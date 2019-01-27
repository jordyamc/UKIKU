package knf.kuma.pojos;

import android.annotation.SuppressLint;
import android.content.Context;
import android.provider.Settings;

import com.jaredrummler.android.device.DeviceName;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;
import knf.kuma.backup.objects.BackupObject;

public class AutoBackupObject extends BackupObject {
    public String name;
    public String device_id;
    @Nullable
    public String value;

    AutoBackupObject() {
    }

    @SuppressLint("HardwareIds")
    public AutoBackupObject(Context context) {
        if (context != null) {
            this.name = DeviceName.getDeviceName();
            this.device_id = Settings.Secure.getString(context.getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
            this.value = PreferenceManager.getDefaultSharedPreferences(context).getString("auto_backup", "0");
        }
    }

    @SuppressLint("HardwareIds")
    public AutoBackupObject(Context context, @Nullable String newValue) {
        if (context != null) {
            this.name = DeviceName.getDeviceName();
            this.device_id = Settings.Secure.getString(context.getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
            this.value = newValue;
        }
    }

    @Override
    public int hashCode() {
        return (name + device_id).hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj instanceof AutoBackupObject &&
                name.equals(((AutoBackupObject) obj).name) &&
                device_id.equals(((AutoBackupObject) obj).device_id);
    }
}
