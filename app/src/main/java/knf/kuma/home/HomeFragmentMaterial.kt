package knf.kuma.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import knf.kuma.BottomFragment
import knf.kuma.R
import knf.kuma.ads.AdsType
import knf.kuma.ads.implBanner
import knf.kuma.commons.EAHelper
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.noCrashSuspend
import knf.kuma.database.CacheDB
import knf.kuma.databinding.FragmentHomeMaterialBinding
import knf.kuma.pojos.QueueObject
import knf.kuma.pojos.RecentObject
import knf.kuma.pojos.SeeingObject
import knf.kuma.queue.QueueActivityMaterial
import knf.kuma.recents.RecentsModelActivity
import knf.kuma.recents.RecentsViewModel
import knf.kuma.recommended.RecommendActivityMaterial
import knf.kuma.recommended.RecommendHelper
import knf.kuma.seeing.SeeingActivityMaterial
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.anko.doAsync

class HomeFragmentMaterial : BottomFragment() {

    private val viewModel: RecentsViewModel by viewModels()
    private var lastNew: String = "0"
    private lateinit var binding: FragmentHomeMaterialBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        EAHelper.enter1("R")
        return inflater.inflate(R.layout.fragment_home_material, container, false).also {
            binding = FragmentHomeMaterialBinding.bind(it)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.listNew.apply {
            setAdapter(RecentsAdapterMaterial(this@HomeFragmentMaterial, isLarge = false, showSeen = false))
            setViewAllOnClick {
                PrefsUtil.recentLastHiddenNew = lastNew.toInt()
                binding.listNew.hide()
            }
        }
        binding.listFavUpdated.apply {
            setAdapter(RecentsAdapterMaterial(this@HomeFragmentMaterial, true))
            setViewAllClass(RecentsModelActivity::class.java)
        }
        binding.listBestEmission.setAdapter(DirAdapterMaterial(this))
        binding.listPending.apply {
            setAdapter(QueueAdapterMaterial(this@HomeFragmentMaterial))
            setViewAllClass(QueueActivityMaterial::class.java)
        }
        binding.listWaiting.apply {
            setAdapter(WaitingAdapterMaterial(this@HomeFragmentMaterial))
            setViewAllClass(SeeingActivityMaterial::class.java)
        }
        binding.listRecommended.apply {
            setAdapter(RecommendedAdapterMaterial(activity))
            setViewAllClass(RecommendActivityMaterial::class.java)
        }
        binding.listRecommendedStaff.setAdapter(SearchAdapterMaterial(this))
        lifecycleScope.launch(Dispatchers.IO) {
            noCrashSuspend {
                delay(1000)
                binding.adContainer.implBanner(AdsType.RECENT_BANNER, true)
                delay(500)
                binding.adContainer2.implBanner(AdsType.RECENT_BANNER2, true)
            }
        }
        viewModel.dbLiveData.observe(viewLifecycleOwner, Observer { list ->
            doAsync {
                binding.listNew.updateList(filterNew(list.filter { it.isNew }))
                val favFiltered = list.filter { CacheDB.INSTANCE.favsDAO().isFav(it.aid.toInt()) }
                if (favFiltered.isEmpty()) {
                    binding.listFavUpdated.apply {
                        setSubheader("Ultimos actualizados")
                        updateList(list)
                    }
                } else {
                    binding.listFavUpdated.apply {
                        setSubheader("Favoritos actualizados")
                        updateList(favFiltered)
                    }
                }
            }
        })
        CacheDB.INSTANCE.favsDAO().countLive.observe(viewLifecycleOwner, Observer {
            doAsync { binding.listFavUpdated.updateList(CacheDB.INSTANCE.recentsDAO().all.filter { CacheDB.INSTANCE.favsDAO().isFav(it.aid.toInt()) }) }
            RecommendHelper.createRecommended {
                binding.listRecommended.updateList(it)
            }
        })
        CacheDB.INSTANCE.animeDAO().emissionVotesLimited.observe(viewLifecycleOwner, Observer {
            binding.listBestEmission.updateList(it)
        })
        CacheDB.INSTANCE.queueDAO().all.observe(viewLifecycleOwner, Observer {
            doAsync { binding.listPending.updateList(QueueObject.takeOne(it)) }
        })
        CacheDB.INSTANCE.seeingDAO().getAllWState(SeeingObject.STATE_CONSIDERING, SeeingObject.STATE_PAUSED).observe(viewLifecycleOwner, Observer {
            binding.listWaiting.updateList(it)
        })
        StaffRecommendations.createList {
            binding.listRecommendedStaff.updateList(it)
        }
        viewModel.reload()
    }

    private fun filterNew(list: List<RecentObject>): List<RecentObject> {
        if (list.isNotEmpty()) {
            lastNew = list[0].aid
            if (list[0].aid.toInt() == PrefsUtil.recentLastHiddenNew)
                return emptyList()
        }
        return list
    }

    override fun onReselect() {
        EAHelper.enter1("R")
    }
}