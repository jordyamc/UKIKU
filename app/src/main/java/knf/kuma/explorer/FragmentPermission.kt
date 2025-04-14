package knf.kuma.explorer

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import knf.kuma.R
import knf.kuma.download.FileAccessHelper
import kotlinx.coroutines.launch
import org.jetbrains.anko.find
import xdroid.toaster.Toaster

class FragmentPermission : Fragment() {
    private lateinit var listener: PermissionListener
    private val permissionContract = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) {
            listener.onPermission()
        } else {
            Toast.makeText(requireContext(), "Permiso denegado", Toast.LENGTH_SHORT).show()
        }
    }
    private val treeChooser = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) {
        val validation = FileAccessHelper.isUriValid(it)
        if (!validation.isValid) {
            Toaster.toast("Directorio invalido: $validation")
        } else {
            listener.onPermission()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_explorer_permission_pending, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.find<MaterialButton>(R.id.permission).apply {
            setOnClickListener {
                lifecycleScope.launch {
                    if (!FileAccessHelper.isStoragePermissionEnabledAsync()) {
                        when {
                            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q -> {
                                permissionContract.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                            }

                            else -> {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                                    Toaster.toastLong("Por favor selecciona un directorio para las descargas")
                                else
                                    Toaster.toastLong("Por favor selecciona la raiz del almacenamiento")
                                treeChooser.launch(null)
                            }
                        }
                    }
                }
            }
        }
    }

    fun setListener(listener: PermissionListener) {
        this.listener = listener
    }

    interface PermissionListener {
        fun onPermission()
    }

    companion object {

        const val TAG = "Permission"

        operator fun get(listener: PermissionListener): FragmentPermission {
            val fragmentFiles = FragmentPermission()
            fragmentFiles.setListener(listener)
            return fragmentFiles
        }
    }
}
