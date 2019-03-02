package knf.kuma.search

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsMultiChoice
import java.util.*

class GenresDialog : DialogFragment() {

    private var genres: MutableList<String> = ArrayList()
    private var selected: MutableList<String> = ArrayList()
    private var listener: MultiChoiceListener? = null

    private val states: BooleanArray
        get() {
            val states = BooleanArray(genres.size)
            var index = 0
            for (genre in genres) {
                states[index++] = selected.contains(genre)
            }
            return states
        }

    private val selectedStates: IntArray
        get() {
            val states = IntArray(selected.size)
            for ((index, item) in selected.withIndex())
                states[index] = genres.indexOf(item)
            return states
        }

    fun init(genres: MutableList<String>, selected: MutableList<String>, listener: MultiChoiceListener) {
        this.genres = genres
        this.selected = selected
        this.listener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            MaterialDialog(it).apply {
                title(text = "Géneros")
                listItemsMultiChoice(items = genres, initialSelection = selectedStates, allowEmptySelection = true) { _: MaterialDialog, _: IntArray, items: List<String> ->
                    selected.apply {
                        clear()
                        addAll(items)
                        sort()
                    }
                    listener?.onOkay(selected)
                }
                positiveButton(text = "BUSCAR")
                negativeButton(text = "CERRAR")
            }
            /*AlertDialog.Builder(it)
                    .setTitle("Generos")
                    .setMultiChoiceItems(genres.toTypedArray(), states) { _, index, isSelected ->
                        if (isSelected)
                            selected.add(genres[index])
                        else
                            selected.remove(genres[index])
                    }.setPositiveButton("BUSCAR") { _, _ ->
                        selected.sort()
                        listener?.onOkay(selected)
                    }.setNegativeButton("CERRAR") { dialogInterface, _ -> dialogInterface.dismiss() }
                    .create()*/
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
        fun onOkay(selected: MutableList<String>)
    }
}
