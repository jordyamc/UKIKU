package knf.kuma.seeing

import android.content.Context
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.afollestad.materialdialogs.checkbox.checkBoxPrompt
import com.afollestad.materialdialogs.checkbox.isCheckPromptChecked
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import knf.kuma.backup.firestore.syncData
import knf.kuma.commons.doOnUIGlobal
import knf.kuma.commons.noCrash
import knf.kuma.commons.noCrashLet
import knf.kuma.commons.safeShow
import knf.kuma.database.CacheDB
import knf.kuma.pojos.AnimeObject
import knf.kuma.pojos.FavoriteObject
import knf.kuma.pojos.SeeingObject
import knf.kuma.pojos.SeenObject
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
                doOnUIGlobal {
                    val dialog = MaterialDialog(context).apply {
                        lifecycleOwner()
                        message(text = "Procesando favoritos... ($count/${favList.size})")
                        cancelable(false)
                        positiveButton(text = "Aceptar") {
                            it.dismiss()
                        }
                    }
                    dialog.getActionButton(WhichButton.POSITIVE).isEnabled = false
                    dialog.safeShow()
                    doAsync {
                        var needSeeingUpdate = false
                        var needSeenUpdate = false
                        favList.forEach { favoriteObject ->
                            if (favoriteObject.isCompleted) {
                                if (!favoriteObject.isSeeing) {
                                    CacheDB.INSTANCE.seeingDAO().add(SeeingObject.fromAnime(favoriteObject).apply { state = SeeingObject.STATE_COMPLETED })
                                    needSeeingUpdate = true
                                }
                                if (withChapters) {
                                    CacheDB.INSTANCE.seenDAO().addAll(favoriteObject.chapters.map { SeenObject.fromChapter(it) })
                                    needSeenUpdate = true
                                }
                            }
                            count++
                            doOnUIGlobal { dialog.message(text = "Procesando favoritos... ($count/${favList.size})") }
                        }
                        syncData {
                            if (needSeeingUpdate)
                                seeing()
                            if (needSeenUpdate)
                                seen()
                        }
                        doOnUIGlobal { dialog.getActionButton(WhichButton.POSITIVE).isEnabled = true }
                    }
                }
            }
        }
    }

    fun getLast(list: List<SeenObject>): SeenObject? =
        list.maxByOrNull {
            noCrashLet(-1.0) {
                "(\\d+\\.?\\d?)".toRegex().findAll(it.number).last().destructured.component1()
                    .toDouble()
            }
        }

    private val FavoriteObject.isCompleted: Boolean get() = CacheDB.INSTANCE.animeDAO().isCompleted(aid)
    private val FavoriteObject.isSeeing: Boolean get() = CacheDB.INSTANCE.seeingDAO().isSeeingAll(aid)
    private val FavoriteObject.chapters: List<AnimeObject.WebInfo.AnimeChapter>
        get() = CacheDB.INSTANCE.animeDAO().getFullByAid(aid)?.chapters ?: listOf()
}