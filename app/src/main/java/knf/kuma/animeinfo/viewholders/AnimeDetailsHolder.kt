package knf.kuma.animeinfo.viewholders

import android.annotation.SuppressLint
import android.content.ClipData
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beloo.widget.chipslayoutmanager.ChipsLayoutManager
import com.beloo.widget.chipslayoutmanager.SpacingItemDecoration
import knf.kuma.R
import knf.kuma.ads.AdsType
import knf.kuma.ads.implBanner
import knf.kuma.animeinfo.AnimeRelatedAdapter
import knf.kuma.animeinfo.AnimeTagsAdapter
import knf.kuma.backup.firestore.syncData
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.doOnUI
import knf.kuma.commons.noCrash
import knf.kuma.commons.removeAllDecorations
import knf.kuma.custom.ExpandableTV
import knf.kuma.database.CacheDB
import knf.kuma.pojos.AnimeObject
import knf.kuma.pojos.SeeingObject
import kotlinx.android.synthetic.main.fragment_anime_details.view.*
import kotlinx.coroutines.*
import org.jetbrains.anko.clipboardManager
import org.jetbrains.anko.doAsync
import uz.jamshid.library.ExactRatingBar
import xdroid.toaster.Toaster

class AnimeDetailsHolder(val view: View) {
    private var cardViews: MutableList<View> = arrayListOf(view.card_title, view.card_desc, view.adContainer, view.card_details, view.card_genres, view.card_list, view.card_related)
    internal val title: TextView = view.title
    private val expandIcon: ImageButton = view.expand_icon
    private val desc: ExpandableTV = view.expandable_desc
    internal val type: TextView = view.type
    internal val state: TextView = view.state
    internal val id: TextView = view.aid
    internal val followers: TextView = view.followers
    private val layScore: LinearLayout = view.lay_score
    private val ratingCount: TextView = view.rating_count
    private val ratingBar: ExactRatingBar = view.ratingBar
    private val recyclerViewGenres: RecyclerView = view.recycler_genres
    private val spinnerList: Spinner = view.spinner_list
    private val recyclerViewRelated: RecyclerView = view.recycler_related
    private val clipboardManager = view.context.applicationContext.clipboardManager
    private var retard = 0
    private var needAnimation = true

    init {
        recyclerViewGenres.layoutManager = ChipsLayoutManager.newBuilder(view.context).build()
        recyclerViewGenres.addItemDecoration(SpacingItemDecoration(5, 5))
        recyclerViewRelated.layoutManager = LinearLayoutManager(view.context)
    }

    @SuppressLint("SetTextI18n")
    fun populate(fragment: Fragment, animeObject: AnimeObject) {
        fragment.lifecycleScope.launch(Dispatchers.Main) {
            title.text = animeObject.name
            noCrash {
                cardViews[0].setOnLongClickListener {
                    try {
                        val clip = ClipData.newPlainText("Anime title", animeObject.name)
                        clipboardManager.setPrimaryClip(clip)
                        Toaster.toast("Título copiado")
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toaster.toast("Error al copiar título")
                    }
                    true
                }
                showCard(cardViews[0])
            }
            noCrash {
                if (animeObject.description != null && animeObject.description?.trim() != "") {
                    desc.setTextAndIndicator(animeObject.description?.trim() ?: "", expandIcon)
                    desc.setAnimationDuration(300)
                    val onClickListener = View.OnClickListener {
                        doOnUI {
                            expandIcon.setImageResource(if (desc.isExpanded) R.drawable.action_expand else R.drawable.action_shrink)
                            desc.toggle()
                        }
                    }
                    desc.setOnClickListener(onClickListener)
                    expandIcon.setOnClickListener(onClickListener)
                    showCard(cardViews[1])
                }
            }
            noCrash {
                if (PrefsUtil.isAdsEnabled) {
                    showCard(cardViews[2])
                }
            }
            noCrash {
                type.text = animeObject.type
                state.text = getStateString(animeObject.state, animeObject.day)
                id.text = animeObject.aid
                followers.text = animeObject.followers
                if (animeObject.rate_stars == null || animeObject.rate_stars == "0.0")
                    layScore.visibility = View.GONE
                else {
                    ratingCount.text = "${animeObject.rate_count} (${animeObject.rate_stars
                            ?: "?.?"})"
                    ratingBar.setStar(animeObject.rate_stars?.toFloat() ?: 0f)
                }
                showCard(cardViews[3])
            }
            noCrash {
                fragment.context?.let { context ->
                    if (animeObject.genres?.isNotEmpty() == true &&
                            animeObject.genresString.trim().let { it != "" && it != "Sin generos" }) {
                        recyclerViewGenres.adapter = AnimeTagsAdapter(context, animeObject.genres)
                        showCard(cardViews[4])
                    }
                }
            }
            noCrash {
                spinnerList.adapter = ArrayAdapter<String>(view.context, android.R.layout.simple_spinner_dropdown_item, view.context.resources.getStringArray(R.array.list_states))
                fragment.lifecycleScope.launch(Dispatchers.Main){
                    spinnerList.setSelection(withContext(Dispatchers.IO){
                        CacheDB.INSTANCE.seeingDAO().getByAid(animeObject.aid)
                    }?.state ?: 0)
                    spinnerList.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onNothingSelected(parent: AdapterView<*>?) {

                        }

                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                            doAsync {
                                if (position == 0)
                                    CacheDB.INSTANCE.seeingDAO().remove(SeeingObject.fromAnime(animeObject, position))
                                else
                                    CacheDB.INSTANCE.seeingDAO().add(SeeingObject.fromAnime(animeObject, position))
                                syncData { seeing() }
                            }
                        }
                    }
                }
                showCard(cardViews[5])
            }
            noCrash {
                if (animeObject.related?.isNotEmpty() == true) {
                    recyclerViewRelated.removeAllDecorations()
                    if (animeObject.related.size > 1)
                        recyclerViewRelated.addItemDecoration(DividerItemDecoration(view.context, LinearLayout.VERTICAL))
                    recyclerViewRelated.adapter = AnimeRelatedAdapter(fragment, animeObject.related)
                    showCard(cardViews[6])
                } else {
                    doOnUI { cardViews[6].visibility = View.GONE }
                }
            }
            needAnimation = false
            if (PrefsUtil.isAdsEnabled) {
                fragment.lifecycleScope.launch(Dispatchers.IO) {
                    retard += 300
                    delay(retard.toLong())
                    view.adContainer.implBanner(AdsType.INFO_BANNER, true)
                }
            }
        }
    }

    private fun showCard(view: View) {
        if (view.visibility == View.VISIBLE || !needAnimation) return
        retard += 100
        GlobalScope.launch(Dispatchers.Main) {
            delay(retard.toLong())
            view.visibility = View.VISIBLE
            val animation = AnimationUtils.makeInChildBottomAnimation(view.context)
            animation.duration = 250
            view.startAnimation(animation)
            if (cardViews.indexOf(view) == 1)
                desc.checkIndicator()
        }
    }

    private fun getStateString(state: String?, day: AnimeObject.Day): String {
        return when (day) {
            AnimeObject.Day.MONDAY -> "$state - Lunes"
            AnimeObject.Day.TUESDAY -> "$state - Martes"
            AnimeObject.Day.WEDNESDAY -> "$state - Miércoles"
            AnimeObject.Day.THURSDAY -> "$state - Jueves"
            AnimeObject.Day.FRIDAY -> "$state - Viernes"
            AnimeObject.Day.SATURDAY -> "$state - Sábado"
            AnimeObject.Day.SUNDAY -> "$state - Domingo"
            else -> state ?: ""
        }
    }
}
