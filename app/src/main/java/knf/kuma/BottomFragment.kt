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
            if (!FileAccessHelper.INSTANCE.isUriValid(data!!.data!!)) {
                Toaster.toast("Directorio invalido")
                FileAccessHelper.openTreeChooser(this)
            }
        }
    }
}
