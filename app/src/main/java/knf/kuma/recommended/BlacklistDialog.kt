package knf.kuma.recommended

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.afollestad.materialdialogs.list.listItemsMultiChoice
import knf.kuma.pojos.av1.GenreRecord

class BlacklistDialog : DialogFragment() {

    private var genresRecord = emptyList<GenreRecord>()
    private var listener: MultiChoiceListener? = null

    fun init(list: List<GenreRecord>, listener: MultiChoiceListener) {
        this.genresRecord = list
        this.listener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            MaterialDialog(it).apply {
                lifecycleOwner()
                title(text = "Lista negra")
                listItemsMultiChoice(items = genresRecord.map { it.name }, initialSelection = genresRecord.mapIndexedNotNull { index, record -> if (record.isBlocked) index else null }.toIntArray(), allowEmptySelection = true) { _, indexes, items ->
                    listener?.onOkay(genresRecord.onEachIndexed { index, record -> record.isBlocked = index in indexes })
                }
                positiveButton(text = "SELECCIONAR")
                negativeButton(text = "CERRAR")
            }
        } ?: super.onCreateDialog(savedInstanceState)
    }

    interface MultiChoiceListener {
        fun onOkay(selected: List<GenreRecord>)
    }
}
