package knf.kuma

import android.app.Activity
import android.content.Intent

import androidx.fragment.app.Fragment
import knf.kuma.download.FileAccessHelper
import xdroid.toaster.Toaster

abstract class BottomFragment : Fragment() {
    abstract fun onReselect()

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FileAccessHelper.SD_REQUEST && resultCode == Activity.RESULT_OK) {
            val validation = FileAccessHelper.INSTANCE.isUriValid(data?.data)
            if (!validation.isValid) {
                Toaster.toast("Directorio invalido: $validation")
                FileAccessHelper.openTreeChooser(this)
            }
        }
    }
}
