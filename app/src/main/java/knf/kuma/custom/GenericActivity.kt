package knf.kuma.custom

import android.app.ActivityManager
import android.graphics.BitmapFactory
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import knf.kuma.R

open class GenericActivity : AppCompatActivity() {
    override fun onResume() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            val appIcon = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
            setTaskDescription(ActivityManager.TaskDescription(getString(R.string.app_name), appIcon, ContextCompat.getColor(this, R.color.colorPrimary)))
        }
        super.onResume()
    }
}