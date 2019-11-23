package knf.kuma.recommended

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.afollestad.materialdialogs.list.listItemsMultiChoice
import knf.kuma.commons.transform
import java.util.*

class BlacklistDialog : DialogFragment() {

    private val genres = getGenres()
    private var selected: MutableList<String> = mutableListOf()
    private var listener: MultiChoiceListener? = null

    private val statesIndex: IntArray
        get() {
            val states = IntArray(selected.size)
            selected.forEachIndexed { index, genre ->
                states[index] = genres.indexOf(genre)
            }
            return states
        }

    fun init(selected: MutableList<String>, listener: MultiChoiceListener) {
        this.selected = selected
        this.listener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            MaterialDialog(it).apply {
                lifecycleOwner()
                title(text = "Lista negra")
                listItemsMultiChoice(items = genres, initialSelection = statesIndex, allowEmptySelection = true) { _, _, items ->
                    selected = mutableListOf<String>().apply {
                        addAll(items.transform())
                        sort()
                    }
                    listener?.onOkay(selected)
                }
                positiveButton(text = "SELECCIONAR")
                negativeButton(text = "CERRAR")
            }
        } ?: super.onCreateDialog(savedInstanceState)
    }

    interface MultiChoiceListener {
        fun onOkay(selected: MutableList<String>)
    }

    companion object {

        fun getGenres(): MutableList<String> {
            return Arrays.asList(
                    "Acción",
                    "Artes Marciales",
                    "Aventuras",
                    "Carreras",
                    "Comedia",
                    "Demencia",
                    "Demonios",
                    "Deportes",
                    "Drama",
                    "Ecchi",
                    "Escolares",
                    "Espacial",
                    "Fantasía",
                    "Ciencia Ficción",
                    "Harem",
                    "Historico",
                    "Infantil",
                    "Josei",
                    "Juegos",
                    "Magia",
                    "Mecha",
                    "Militar",
                    "Misterio",
                    "Musica",
                    "Parodia",
                    "Policía",
                    "Psicológico",
                    "Recuentos de la vida",
                    "Romance",
                    "Samurai",
                    "Seinen",
                    "Shoujo",
                    "Shounen",
                    "Sin Generos",
                    "Sobrenatural",
                    "Superpoderes",
                    "Suspenso",
                    "Terror",
                    "Vampiros",
                    "Yaoi",
                    "Yuri")
        }
    }
}
