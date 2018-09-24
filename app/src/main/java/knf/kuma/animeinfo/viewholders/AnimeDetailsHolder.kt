package knf.kuma.animeinfo.viewholders

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beloo.widget.chipslayoutmanager.ChipsLayoutManager
import com.beloo.widget.chipslayoutmanager.SpacingItemDecoration
import knf.kuma.R
import knf.kuma.animeinfo.AnimeRelatedAdapter
import knf.kuma.animeinfo.AnimeTagsAdapter
import knf.kuma.commons.noCrash
import knf.kuma.custom.ExpandableTV
import knf.kuma.database.CacheDB
import knf.kuma.pojos.AnimeObject
import knf.kuma.pojos.SeeingObject
import kotlinx.android.synthetic.main.fragment_anime_details.view.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import me.zhanghai.android.materialratingbar.MaterialRatingBar
import org.jetbrains.anko.doAsync
import xdroid.toaster.Toaster

class AnimeDetailsHolder(val view: View) {
    private var cardViews: MutableList<CardView> = arrayListOf(view.card_title, view.card_desc, view.card_details, view.card_genres, view.card_list, view.card_related)
    internal val title: TextView = view.title
    private val expandIcon: ImageButton = view.expand_icon
    private val desc: ExpandableTV = view.expandable_desc
    internal val type: TextView = view.type
    internal val state: TextView = view.state
    internal val id: TextView = view.aid
    private val layScore: LinearLayout = view.lay_score
    private val ratingCount: TextView = view.rating_count
    internal val ratingBar: MaterialRatingBar = view.ratingBar
    private val recyclerViewGenres: RecyclerView = view.recycler_genres
    private val spinnerList: Spinner = view.spinner_list
    private val recyclerViewRelated: RecyclerView = view.recycler_related
    private val clipboardManager = view.context.applicationContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    private var retard = 0

    init {
        recyclerViewGenres.layoutManager = ChipsLayoutManager.newBuilder(view.context).build()
        recyclerViewGenres.addItemDecoration(SpacingItemDecoration(5, 5))
        recyclerViewRelated.layoutManager = LinearLayoutManager(view.context)
        recyclerViewRelated.addItemDecoration(DividerItemDecoration(view.context, LinearLayout.VERTICAL))
    }

    fun populate(fragment: Fragment, animeObject: AnimeObject) {
        launch(UI) {
            title.text = animeObject.name
            noCrash {
                cardViews[0].setOnLongClickListener {
                    try {
                        val clip = ClipData.newPlainText("Anime title", animeObject.name)
                        clipboardManager.primaryClip = clip
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
                if (animeObject.description != null && animeObject.description!!.trim { it <= ' ' } != "") {
                    desc.setTextAndIndicator(animeObject.description!!.trim { it <= ' ' }, expandIcon)
                    desc.setAnimationDuration(300)
                    val onClickListener = View.OnClickListener {
                        launch(UI) {
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
                type.text = animeObject.type
                state.text = getStateString(animeObject.state!!, animeObject.day!!)
                id.text = animeObject.aid
                if (animeObject.rate_stars == null || animeObject.rate_stars == "0.0")
                    layScore.visibility = View.GONE
                else {
                    ratingCount.text = animeObject.rate_count
                    ratingBar.rating = animeObject.rate_stars!!.toFloat()
                }
                showCard(cardViews[2])
            }
            noCrash {
                if (animeObject.genres?.isNotEmpty() == true) {
                    recyclerViewGenres.adapter = AnimeTagsAdapter(fragment.context!!, animeObject.genres)
                    showCard(cardViews[3])
                }
            }
            noCrash {
                val seeingObject = CacheDB.INSTANCE.seeingDAO().getByAid(animeObject.aid)
                spinnerList.adapter = ArrayAdapter<String>(view.context, android.R.layout.simple_spinner_dropdown_item, view.context.resources.getStringArray(R.array.list_states))
                spinnerList.setSelection(seeingObject?.state ?: 0)
                spinnerList.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onNothingSelected(parent: AdapterView<*>?) {

                    }

                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        doAsync {
                            if (position == 0)
                                CacheDB.INSTANCE.seeingDAO().remove(SeeingObject.fromAnime(animeObject, position))
                            else
                                CacheDB.INSTANCE.seeingDAO().add(SeeingObject.fromAnime(animeObject, position))
                        }
                    }
                }
                showCard(cardViews[4])
            }
            noCrash {
                if (animeObject.related?.isNotEmpty() == true) {
                    recyclerViewRelated.adapter = AnimeRelatedAdapter(fragment, animeObject.related)
                    showCard(cardViews[5])
                }
            }
        }
    }

    private fun showCard(view: CardView) {
        retard += 100
        launch(UI) {
            delay(retard.toLong())
            view.visibility = View.VISIBLE
            val animation = AnimationUtils.makeInChildBottomAnimation(view.context)
            animation.duration = 250
            view.startAnimation(animation)
            if (cardViews.indexOf(view) == 1)
                desc.checkIndicator()
        }
        /*Handler(Looper.getMainLooper()).postDelayed({
            view.visibility = View.VISIBLE
            val animation = AnimationUtils.makeInChildBottomAnimation(view.context)
            animation.duration = 250
            view.startAnimation(animation)
            if (cardViews.indexOf(view) == 1)
                desc.checkIndicator()
        }, retard.toLong())*/
    }

    private fun getStateString(state: String, day: AnimeObject.Day): String {
        return when (day) {
            AnimeObject.Day.MONDAY -> "$state - Lunes"
            AnimeObject.Day.TUESDAY -> "$state - Martes"
            AnimeObject.Day.WEDNESDAY -> "$state - Miércoles"
            AnimeObject.Day.THURSDAY -> "$state - Jueves"
            AnimeObject.Day.FRIDAY -> "$state - Viernes"
            AnimeObject.Day.SATURDAY -> "$state - Sábado"
            AnimeObject.Day.SUNDAY -> "$state - Domingo"
            else -> state
        }
    }
}
