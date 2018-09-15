package knf.kuma.search

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
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

    fun init(genres: MutableList<String>, selected: MutableList<String>, listener: MultiChoiceListener) {
        this.genres = genres
        this.selected = selected
        this.listener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(activity!!)
                .setTitle("Generos")
                .setMultiChoiceItems(genres.toTypedArray(), states) { _, index, isSelected ->
                    if (isSelected)
                        selected.add(genres[index])
                    else
                        selected.remove(genres[index])
                }.setPositiveButton("BUSCAR") { _, _ ->
                    selected.sort()
                    listener!!.onOkay(selected)
                }.setNegativeButton("CERRAR") { dialogInterface, _ -> dialogInterface.dismiss() }.create()
    }

    override fun show(manager: FragmentManager, tag: String) {
        try {
            super.show(manager, tag)
        } catch (e: Exception) {
            //
        }

    }

    interface MultiChoiceListener {
        fun onOkay(selected: MutableList<String>)
    }
}
