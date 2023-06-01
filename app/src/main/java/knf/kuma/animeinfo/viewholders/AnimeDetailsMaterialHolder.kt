package knf.kuma.animeinfo.viewholders

import android.annotation.SuppressLint
import android.content.ClipData
import android.view.View
import android.widget.*
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beloo.widget.chipslayoutmanager.ChipsLayoutManager
import com.beloo.widget.chipslayoutmanager.SpacingItemDecoration
import knf.kuma.R
import knf.kuma.ads.AdsType
import knf.kuma.ads.AdsUtils
import knf.kuma.ads.NativeManager
import knf.kuma.ads.implBanner
import knf.kuma.animeinfo.AnimeRelatedAdapterMaterial
import knf.kuma.animeinfo.AnimeTagsAdapterMaterial
import knf.kuma.backup.firestore.syncData
import knf.kuma.commons.*
import knf.kuma.custom.ExpandableTV
import knf.kuma.database.CacheDB
import knf.kuma.databinding.FragmentAnimeDetailsMaterialBinding
import knf.kuma.pojos.AnimeObject
import knf.kuma.pojos.SeeingObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.clipboardManager
import org.jetbrains.anko.doAsync
import uz.jamshid.library.ExactRatingBar
import xdroid.toaster.Toaster

class AnimeDetailsMaterialHolder(val view: View) {
    private val binding = FragmentAnimeDetailsMaterialBinding.bind(view)
    private var layouts: MutableList<View> = arrayListOf(binding.layTitle, binding.layDescription, binding.adContainer, binding.layDetails, binding.layGenres, binding.layFollow, binding.layRelated)
    internal val title: TextView = binding.title
    private val expandIcon: ImageButton = binding.expandIcon
    private val desc: ExpandableTV = binding.expandableDesc
    internal val type: TextView = binding.type
    internal val state: TextView = binding.state
    internal val id: TextView = binding.aid
    internal val followers: TextView = binding.followers
    private val layScore: LinearLayout = binding.layScore
    private val ratingCount: TextView = binding.ratingCount
    private val ratingBar: ExactRatingBar = binding.ratingBar
    private val recyclerViewGenres: RecyclerView = binding.recyclerGenres
    private val spinnerList: Spinner = binding.spinnerList
    private val recyclerViewRelated: RecyclerView = binding.recyclerRelated
    private val clipboardManager = view.context.applicationContext.clipboardManager
    private var needAnimation = true

    init {
        recyclerViewGenres.layoutManager = ChipsLayoutManager.newBuilder(view.context).build()
        recyclerViewGenres.addItemDecoration(SpacingItemDecoration(5, 5))
        recyclerViewRelated.layoutManager = LinearLayoutManager(view.context)
        binding.layAd.isVisible = PrefsUtil.isAdsEnabled
    }

    @SuppressLint("SetTextI18n")
    fun populate(fragment: Fragment, animeObject: AnimeObject) {
        fragment.lifecycleScope.launch(Dispatchers.Main) {
            title.text = animeObject.name
            noCrash {
                layouts[0].setOnLongClickListener {
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
                showLayout(layouts[0])
            }
            noCrash {
                if (animeObject.description != null && animeObject.description?.isBlank() == false) {
                    desc.setTextAndIndicator(animeObject.description?.trim() ?: "", expandIcon)
                    desc.setAnimationDuration(300)
                    val onClickListener = View.OnClickListener {
                        expandIcon.setImageResource(if (desc.isExpanded) R.drawable.action_expand else R.drawable.action_shrink)
                        desc.toggle()
                    }
                    desc.setOnClickListener(onClickListener)
                    expandIcon.setOnClickListener(onClickListener)
                    showLayout(layouts[1])
                } else {
                    binding.layDescriptionSeparator.isVisible = false
                }
            }
            if (PrefsUtil.isAdsEnabled) {
                launch {
                    noCrashSuspend {
                        if (AdsUtils.isAdmobEnabled)
                            NativeManager.take(fragment.lifecycleScope, 1) {
                                if (it.isNotEmpty()) {
                                    binding.adContainer.setNativeAd(it[0])
                                } else {
                                    binding.adContainer.isVisible = false
                                    binding.layAd.implBanner(AdsType.INFO_BANNER)
                                }
                            }
                        else {
                            binding.adContainer.isVisible = false
                            binding.layAd.implBanner(AdsType.INFO_BANNER)
                        }
                    }
                }
                showLayout(layouts[2])
            }
            noCrash {
                type.text = animeObject.type
                state.text = getStateString(animeObject.state, animeObject.day)
                id.text = animeObject.aid
                followers.text = animeObject.followers
                if (animeObject.rate_stars == null || animeObject.rate_stars == "0.0")
                    layScore.visibility = View.GONE
                else {
                    ratingCount.text = "${animeObject.rate_count} (${
                        animeObject.rate_stars
                                ?: "?.?"
                    })"
                    ratingBar.setStar(animeObject.rate_stars?.toFloat() ?: 0f)
                }
                showLayout(layouts[3])
            }
            noCrash {
                fragment.context?.let { context ->
                    if (animeObject.genres?.isNotEmpty() == true &&
                            animeObject.genresString.trim().let { it != "" && it != "Sin generos" }) {
                        recyclerViewGenres.adapter = AnimeTagsAdapterMaterial(context, animeObject.genres)
                        showLayout(layouts[4])
                    }
                }
            }
            if (!layouts[5].isVisible)
                noCrash {
                    spinnerList.adapter = ArrayAdapter(view.context, android.R.layout.simple_spinner_dropdown_item, view.context.resources.getStringArray(R.array.list_states))
                    fragment.lifecycleScope.launch(Dispatchers.Main) {
                        spinnerList.onItemSelectedListener = null
                        spinnerList.setSelection(withContext(Dispatchers.IO) {
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
                    showLayout(layouts[5])
                }
            noCrash {
                if (animeObject.related?.isNotEmpty() == true) {
                    recyclerViewRelated.removeAllDecorations()
                    recyclerViewRelated.adapter = AnimeRelatedAdapterMaterial(fragment, animeObject.related)
                    showLayout(layouts[6])
                } else {
                    launch(Dispatchers.Main) { layouts[6].visibility = View.GONE }
                }
            }
            needAnimation = false
        }
    }

    private fun showLayout(view: View) {
        if (view.visibility == View.VISIBLE || !needAnimation) return
        view.isVisibleAnimate = true
        if (layouts.indexOf(view) == 1)
            desc.checkIndicator()
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
