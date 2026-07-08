package knf.kuma.search

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.afollestad.materialdialogs.list.listItemsMultiChoice
import knf.kuma.pojos.av1.Genre

class GenresDialog : DialogFragment() {

    private var genres: MutableList<Genre> = ArrayList()
    private var selected: List<Genre> = ArrayList()
    private var listener: MultiChoiceListener? = null

    private val selectedStates: IntArray
        get() {
            val states = mutableListOf<Int>()
            for ((_, item) in selected.withIndex())
                states.add(genres.indexOf(item))
            return states.toIntArray()
        }

    fun init(genres: MutableList<Genre>, selected: List<Genre>, listener: MultiChoiceListener) {
        this.genres = genres
        this.selected = selected
        this.listener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            MaterialDialog(it).apply {
                lifecycleOwner()
                title(text = "Géneros")
                listItemsMultiChoice(items = genres.map { it.name }, initialSelection = selectedStates, allowEmptySelection = true) { _: MaterialDialog, selectedIndex: IntArray, _: List<CharSequence> ->
                    listener?.onOkay(genres.filterIndexed { index, _ -> index in selectedIndex })
                }
                positiveButton(text = "BUSCAR")
                negativeButton(text = "CERRAR")
            }
        } ?: super.onCreateDialog(savedInstanceState)
    }

    override fun show(manager: FragmentManager, tag: String?) {
        try {
            super.show(manager, tag)
        } catch (e: Exception) {
            //
        }
    }

    override fun dismiss() {
        try {
            super.dismiss()
        } catch (e: Exception) {
            //
        }
    }

    interface MultiChoiceListener {
        fun onOkay(selected: List<Genre>)
    }
}
