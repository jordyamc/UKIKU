package knf.kuma.seeing

import android.content.Context
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.afollestad.materialdialogs.checkbox.checkBoxPrompt
import com.afollestad.materialdialogs.checkbox.isCheckPromptChecked
import knf.kuma.commons.doOnUI
import knf.kuma.commons.noCrash
import knf.kuma.commons.safeShow
import knf.kuma.database.CacheDB
import knf.kuma.pojos.AnimeObject
import knf.kuma.pojos.FavoriteObject
import knf.kuma.pojos.SeeingObject
import org.jetbrains.anko.doAsync

object FavToSeeing {

    fun onConfirmation(context: Context) {
        MaterialDialog(context).safeShow {
            title(text = "Convertir favoritos")
            message(text = "Se marcarán todos los animes FINALIZADOS en favoritos como COMPLETADOS, continuar?")
            checkBoxPrompt(text = "Marcar todos los episodios como vistos") {}
            positiveButton(text = "Continuar") {
                start(context, it.isCheckPromptChecked())
            }
        }
    }

    private fun start(context: Context, withChapters: Boolean) {
        doAsync {
            noCrash {
                val favList = CacheDB.INSTANCE.favsDAO().allRaw
                var count = 0
                doOnUI {
                    val dialog = MaterialDialog(context).apply {
                        message(text = "Procesando favoritos... ($count/${favList.size})")
                        cancelable(false)
                        positiveButton(text = "Aceptar") {
                            it.dismiss()
                        }
                    }
                    dialog.getActionButton(WhichButton.POSITIVE).isEnabled = false
                    dialog.safeShow()
                    doAsync {
                        favList.forEach {
                            if (it.isCompleted) {
                                if (!it.isSeeing)
                                    CacheDB.INSTANCE.seeingDAO().add(SeeingObject.fromAnime(it).apply { state = SeeingObject.STATE_COMPLETED })
                                if (withChapters)
                                    CacheDB.INSTANCE.chaptersDAO().addAll(it.chapters)
                            }
                            count++
                            doOnUI { dialog.message(text = "Procesando favoritos... ($count/${favList.size})") }
                        }
                        doOnUI { dialog.getActionButton(WhichButton.POSITIVE).isEnabled = true }
                    }
                }
            }
        }
    }

    private val FavoriteObject.isCompleted: Boolean get() = CacheDB.INSTANCE.animeDAO().isCompleted(aid)
    private val FavoriteObject.isSeeing: Boolean get() = CacheDB.INSTANCE.seeingDAO().isSeeingAll(aid)
    private val FavoriteObject.chapters: List<AnimeObject.WebInfo.AnimeChapter>
        get() = CacheDB.INSTANCE.animeDAO().getFullByAid(aid)?.chapters ?: listOf()
}