package knf.kuma.animeinfo

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import knf.kuma.R
import kotlinx.android.synthetic.main.lay_bottom_actions.view.*

class BottomActionsDialog : BottomSheetDialogFragment() {
    private var callback: ActionsCallback? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.lay_bottom_actions, container, false)
        view.action_seen.setOnClickListener {
            callback?.onSelect(STATE_SEEN)
            safeDismiss()
        }
        view.action_unseen.setOnClickListener {
            callback?.onSelect(STATE_UNSEEN)
            safeDismiss()
        }
        view.action_import_all.setOnClickListener {
            callback?.onSelect(STATE_IMPORT_MULTIPLE)
            safeDismiss()
        }
        return view
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnShowListener { dialogInterface ->
            try {
                val d = dialogInterface as BottomSheetDialog
                val bottomSheetInternal = d.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                BottomSheetBehavior.from(bottomSheetInternal!!).setState(BottomSheetBehavior.STATE_EXPANDED)
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
        super.onDismiss(dialog)
        callback?.onDismiss()
    }

    interface ActionsCallback {
        fun onSelect(state: Int)

        fun onDismiss()
    }

    companion object {
        const val STATE_SEEN = 0
        const val STATE_UNSEEN = 1
        const val STATE_IMPORT_MULTIPLE = 2
        const val STATE_DOWNLOAD_MULTIPLE = 3 //TODO: Multiple downloads

        fun newInstance(callback: ActionsCallback): BottomActionsDialog {
            val actionsDialog = BottomActionsDialog()
            actionsDialog.callback = callback
            return actionsDialog
        }
    }
}
