package knf.kuma.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import knf.kuma.BottomFragment
import knf.kuma.R
import knf.kuma.ads.AdsType
import knf.kuma.ads.implBanner
import knf.kuma.commons.EAHelper
import knf.kuma.commons.JsExtractor
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.safeContext
import knf.kuma.database.CacheDB
import knf.kuma.databinding.FragmentHomeBinding
import knf.kuma.pojos.QueueObject
import knf.kuma.pojos.SeeingObject
import knf.kuma.pojos.av1.DirectoryAV1Min
import knf.kuma.pojos.av1.RecentAV1
import knf.kuma.queue.QueueActivity
import knf.kuma.recents.RecentsActivity
import knf.kuma.recents.RecentsViewModel
import knf.kuma.recommended.RecommendActivity
import knf.kuma.recommended.RecommendHelper
import knf.kuma.seeing.SeeingActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.anko.doAsync
import kotlin.math.max

class HomeFragment : BottomFragment() {

    private val viewModel: RecentsViewModel by viewModels()
    private var lastNew: Int = 0
    private lateinit var binding: FragmentHomeBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        EAHelper.enter1("R")
        return inflater.inflate(R.layout.fragment_home, container, false).also {
            binding = FragmentHomeBinding.bind(it)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.listNew.apply {
            setAdapter(RecentsAV1Adapter(this@HomeFragment, false, isLarge = false, showSeen = false))
            setViewAllOnClick {
                PrefsUtil.recentLastHiddenNew = lastNew
                binding.listNew.hide()
            }
        }
        binding.listFavUpdated.apply {
            setAdapter(RecentsAV1Adapter(this@HomeFragment, false))
            setViewAllClass(RecentsActivity::class.java)
        }
        binding.listBestEmission.setAdapter(DirAdapter(this, false))
        binding.listPending.apply {
            setAdapter(QueueAdapter(this@HomeFragment))
            setViewAllClass(QueueActivity::class.java)
        }
        binding.listWaiting.apply {
            setAdapter(WaitingAdapter(this@HomeFragment))
            setViewAllClass(SeeingActivity::class.java)
        }
        binding.listRecommended.apply {
            setAdapter(RecommendedAdapter(requireActivity()))
            setViewAllClass(RecommendActivity::class.java)
        }
        binding.listRecommendedStaff.setAdapter(SearchAdapterMaterial(this))
        lifecycleScope.launch(Dispatchers.IO) {
            delay(1000)
            binding.adContainer.implBanner(AdsType.RECENT_BANNER, true)
            delay(500)
            binding.adContainer2.implBanner(AdsType.RECENT_BANNER2, true)
        }
        lifecycleScope.launch {
            viewModel.dbFlowData.collect { list ->
                if (list.isNotEmpty()) {
                    doAsync {
                        try {
                            binding.listNew.updateList(filterNew(list.filter { it.state.isNew }))
                            val favFiltered = list.filter { CacheDB.INSTANCE.favoriteAV1DAO().isFav(it.aid) }
                            if (favFiltered.isEmpty()) {
                                binding.listFavUpdated.apply {
                                    setSubheader("Ultimos actualizados")
                                    setError("Recientes no actualizados")
                                    updateList(list)
                                }
                            } else {
                                binding.listFavUpdated.apply {
                                    setSubheader("Favoritos actualizados")
                                    updateList(favFiltered)
                                }
                            }
                        } catch (e: Exception) {
                            Firebase.crashlytics.recordException(e)
                            lifecycleScope.launch(Dispatchers.Main) {
                                Toast.makeText(safeContext, "Error al mostrar recientes: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            CacheDB.INSTANCE.favoriteAV1DAO().countFlow.collectLatest {
                binding.listRecommended.updateList(RecommendHelper.createRecommended())
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            val result = JsExtractor.processLink("https://animeav1.com/catalogo?category=tv-anime&status=emision&order=score") ?: return@launch
            val items = mutableListOf<DirectoryAV1Min>()
            for (i in 0 until max(result.length(), 10)) {
                val item = result.getJSONObject(i)
                items.add(DirectoryAV1Min.fromJson(item))
            }
            binding.listBestEmission.updateList(items)
        }
        CacheDB.INSTANCE.queueDAO().all.observe(viewLifecycleOwner, Observer {
            doAsync { binding.listPending.updateList(QueueObject.takeOne(it)) }
        })
        lifecycleScope.launch {
            CacheDB.INSTANCE.organizerDAO().getAllWState(SeeingObject.STATE_CONSIDERING, SeeingObject.STATE_PAUSED).collectLatest {
                binding.listWaiting.updateList(it)
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            binding.listRecommendedStaff.updateList(StaffRecommendations.createList())
        }
        viewModel.reload()
    }

    private fun filterNew(list: List<RecentAV1>): List<RecentAV1> {
        if (list.isNotEmpty()) {
            lastNew = list[0].aid
            if (list[0].aid == PrefsUtil.recentLastHiddenNew)
                return emptyList()
        }
        return list
    }

    override fun onReselect() {
        EAHelper.enter1("R")
    }
}