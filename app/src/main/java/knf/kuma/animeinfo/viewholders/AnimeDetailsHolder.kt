package knf.kuma.animeinfo.viewholders

import android.annotation.SuppressLint
import android.content.ClipData
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import ir.mahdiparastesh.chlm.ChipsLayoutManager
import ir.mahdiparastesh.chlm.SpacingItemDecoration
import knf.kuma.R
import knf.kuma.ads.AdsType
import knf.kuma.ads.implBanner
import knf.kuma.animeinfo.AnimeRelatedAdapterMaterial
import knf.kuma.animeinfo.AnimeTagsAdapterMaterial
import knf.kuma.backup.firestore.syncData
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.noCrash
import knf.kuma.commons.removeAllDecorations
import knf.kuma.custom.ExpandableTV
import knf.kuma.custom.VariantLinearLayoutManager
import knf.kuma.database.CacheDB
import knf.kuma.databinding.FragmentAnimeDetailsBinding
import knf.kuma.pojos.av1.DirectoryAV1
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.clipboardManager
import org.jetbrains.anko.doAsync
import uz.jamshid.library.ExactRatingBar
import xdroid.toaster.Toaster

class AnimeDetailsHolder(val view: View) {
    private val binding = FragmentAnimeDetailsBinding.bind(view)
    private var cardViews: MutableList<View> = arrayListOf(binding.cardTitle, binding.cardDesc, binding.adContainer, binding.cardDetails, binding.cardGenres, binding.cardList, binding.cardRelated)
    internal val title: TextView = binding.title
    private val expandIcon: ImageButton = binding.expandIcon
    private val desc: ExpandableTV = binding.expandableDesc
    internal val type: TextView = binding.type
    internal val state: TextView = binding.state
    internal val id: TextView = binding.aid
    private val layScore: LinearLayout = binding.layScore
    private val ratingCount: TextView = binding.ratingCount
    private val ratingBar: ExactRatingBar = binding.ratingBar
    private val recyclerViewGenres: RecyclerView = binding.recyclerGenres
    private val spinnerList: Spinner = binding.spinnerList
    private val recyclerViewRelated: RecyclerView = binding.recyclerRelated
    private val clipboardManager = view.context.applicationContext.clipboardManager
    private var retard = 0
    private var needAnimation = true

    init {
        recyclerViewGenres.layoutManager = ChipsLayoutManager.newBuilder(view.context).build()
        recyclerViewGenres.addItemDecoration(SpacingItemDecoration(5, 5))
        recyclerViewRelated.layoutManager = VariantLinearLayoutManager(view.context)
    }

    @SuppressLint("SetTextI18n")
    fun populate(fragment: Fragment, animeObject: DirectoryAV1) {
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
                if (!animeObject.description.isBlank()) {
                    desc.setTextAndIndicator(animeObject.description.trim(), expandIcon)
                    desc.setAnimationDuration(300)
                    val onClickListener = View.OnClickListener {
                        expandIcon.setImageResource(if (desc.isExpanded) R.drawable.action_expand else R.drawable.action_shrink)
                        desc.toggle()
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
                type.text = animeObject.typeText
                state.text = getStateString(animeObject.state, animeObject.day)
                id.text = animeObject.aid.toString()
                if (animeObject.rateStars == 0.0f)
                    layScore.visibility = View.GONE
                else {
                    ratingCount.text = "${animeObject.rateCount} (${animeObject.rateStars})"
                    ratingBar.setStar(animeObject.rateStars)
                }
                showCard(cardViews[3])
            }
            noCrash {
                fragment.context?.let { context ->
                    if (animeObject.genres.isNotEmpty()) {
                        recyclerViewGenres.adapter = AnimeTagsAdapterMaterial(context, animeObject.genres)
                        showCard(cardViews[4])
                    }
                }
            }
            noCrash {
                spinnerList.adapter = ArrayAdapter(view.context, android.R.layout.simple_spinner_dropdown_item, view.context.resources.getStringArray(R.array.list_states))
                fragment.lifecycleScope.launch(Dispatchers.Main) {
                    spinnerList.onItemSelectedListener = null
                    spinnerList.setSelection(withContext(Dispatchers.IO) {
                        CacheDB.INSTANCE.organizerDAO().getByAid(animeObject.aid)
                    }?.organizer?.state ?: 0)
                    spinnerList.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onNothingSelected(parent: AdapterView<*>?) {

                        }

                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                            doAsync {
                                if (position == 0)
                                    CacheDB.INSTANCE.organizerDAO().remove(animeObject.asOrganizer(position))
                                else
                                    CacheDB.INSTANCE.organizerDAO().add(animeObject.asOrganizer(position))
                                syncData { seeing() }
                            }
                        }
                    }
                }
                showCard(cardViews[5])
            }
            noCrash {
                if (animeObject.relations.isNotEmpty()) {
                    recyclerViewRelated.removeAllDecorations()
                    if (animeObject.relations.size > 1)
                        recyclerViewRelated.addItemDecoration(DividerItemDecoration(view.context, LinearLayout.VERTICAL))
                    recyclerViewRelated.adapter = AnimeRelatedAdapterMaterial(fragment, animeObject.relations)
                    showCard(cardViews[6])
                } else {
                    launch(Dispatchers.Main) { cardViews[6].visibility = View.GONE }
                }
            }
            needAnimation = false
            if (PrefsUtil.isAdsEnabled) {
                fragment.lifecycleScope.launch(Dispatchers.IO) {
                    retard += 300
                    delay(retard.toLong())
                    binding.adContainer.implBanner(AdsType.INFO_BANNER, true)
                }
            }
        }
    }

    private fun showCard(view: View) {
        if (view.isVisible || !needAnimation) return
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

    private fun getStateString(stateFlag: Int, day: Int?): String {
        val state = when (stateFlag) {
            0 -> "Finalizado"
            2 -> "Emisión"
            else -> "Próximamente"
        }
        return when (day) {
            1 -> "$state - Lunes"
            2 -> "$state - Martes"
            3 -> "$state - Miércoles"
            4 -> "$state - Jueves"
            5 -> "$state - Viernes"
            6 -> "$state - Sábado"
            7 -> "$state - Domingo"
            else -> state
        }
    }
}
