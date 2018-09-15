package knf.kuma.recommended

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import java.util.*

class BlacklistDialog : DialogFragment() {

    private val genres = getGenres()
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

    fun init(selected: MutableList<String>, listener: MultiChoiceListener) {
        this.selected = selected
        this.listener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(activity!!)
                .setTitle("Lista negra")
                .setMultiChoiceItems(genres.toTypedArray(), states) { _, index, isSelected ->
                    if (isSelected) {
                        selected.add(genres[index])
                    } else {
                        selected.remove(genres[index])
                    }
                }.setPositiveButton("SELECCIONAR") { _, _ ->
                    selected.sort()
                    listener?.onOkay(selected)
                }.setNegativeButton("CERRAR") { dialogInterface, _ -> dialogInterface.dismiss() }.create()
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
