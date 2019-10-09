package knf.kuma.animeinfo

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleObserver
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import knf.kuma.BuildConfig
import knf.kuma.R
import kotlinx.android.synthetic.main.lay_bottom_actions.view.*
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.support.v4.toast

class BottomActionsDialog : BottomSheetDialogFragment(), LifecycleObserver {
    private var callback: ActionsCallback? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.lay_bottom_actions, container, false)
        view.action_seen.onClick {
            callback?.onSelect(STATE_SEEN)
            safeDismiss()
        }
        view.action_unseen.onClick {
            callback?.onSelect(STATE_UNSEEN)
            safeDismiss()
        }
        view.action_import_all.onClick {
            callback?.onSelect(STATE_IMPORT_MULTIPLE)
            safeDismiss()
        }
        view.action_download_all.onClick {
            if (BuildConfig.BUILD_TYPE == "playstore") {
                toast("Deshabilitado para esta version")
            } else {
                callback?.onSelect(STATE_DOWNLOAD_MULTIPLE)
            }
            safeDismiss()
        }
        view.action_queue_all.onClick {
            if (BuildConfig.BUILD_TYPE == "playstore") {
                toast("Deshabilitado para esta version")
            } else {
                callback?.onSelect(STATE_QUEUE_MULTIPLE)
            }
            safeDismiss()
        }
        return view
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnShowListener { dialogInterface ->
            try {
                val d = dialogInterface as? BottomSheetDialog
                d?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.also {
                    BottomSheetBehavior.from(it).setState(BottomSheetBehavior.STATE_EXPANDED)
                }
            } catch (e: Exception) {
                //
            }
        }
        return dialog
    }

    fun safeShow(manager: FragmentManager, tag: String) {
        try {
            show(manager, tag)
        } catch (e: Exception) {
            //
        }

    }

    private fun safeDismiss() {
        try {
            dismiss()
        } catch (e: Exception) {
            //
        }

    }

    override fun onDismiss(dialog: DialogInterface) {
        try {
            super.onDismiss(dialog)
            callback?.onDismiss()
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
    }

    interface ActionsCallback {
        fun onSelect(state: Int)

        fun onDismiss()
    }

    companion object {
        const val STATE_SEEN = 0
        const val STATE_UNSEEN = 1
        const val STATE_IMPORT_MULTIPLE = 2
        const val STATE_DOWNLOAD_MULTIPLE = 3
        const val STATE_QUEUE_MULTIPLE = 4

        fun newInstance(callback: ActionsCallback): BottomActionsDialog {
            val actionsDialog = BottomActionsDialog()
            actionsDialog.callback = callback
            return actionsDialog
        }
    }
}
