package knf.kuma.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.crashlytics.android.Crashlytics
import knf.kuma.BottomFragment
import knf.kuma.R
import knf.kuma.ads.AdsType
import knf.kuma.ads.implBanner
import knf.kuma.commons.EAHelper
import knf.kuma.commons.PrefsUtil
import knf.kuma.database.CacheDB
import knf.kuma.pojos.QueueObject
import knf.kuma.pojos.RecentObject
import knf.kuma.pojos.SeeingObject
import knf.kuma.queue.QueueActivity
import knf.kuma.recents.RecentsActivity
import knf.kuma.recents.RecentsViewModel
import knf.kuma.recommended.RecommendActivity
import knf.kuma.recommended.RecommendHelper
import knf.kuma.seeing.SeeingActivity
import kotlinx.android.synthetic.main.fragment_home.*
import org.jetbrains.anko.doAsync

class HomeFragment : BottomFragment() {

    private val viewModel: RecentsViewModel by lazy { ViewModelProviders.of(this).get(RecentsViewModel::class.java) }
    private var lastNew: String = "0"

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel.dbLiveData.observe(this, Observer { list ->
            doAsync {
                listNew.updateList(filterNew(list.filter { it.isNew }))
                val favFiltered = list.filter { CacheDB.INSTANCE.favsDAO().isFav(it.aid.toInt()) }
                if (favFiltered.isEmpty()) {
                    listFavUpdated.apply {
                        setSubheader("Ultimos actualizados")
                        updateList(list)
                    }
                } else {
                    listFavUpdated.apply {
                        setSubheader("Favoritos actualizados")
                        updateList(favFiltered)
                    }
                }
            }
        })
        CacheDB.INSTANCE.favsDAO().countLive.observe(this, Observer {
            doAsync { listFavUpdated.updateList(CacheDB.INSTANCE.recentsDAO().all.filter { CacheDB.INSTANCE.favsDAO().isFav(it.aid.toInt()) }) }
            RecommendHelper.createRecommended {
                listRecommended.updateList(it)
            }
        })
        CacheDB.INSTANCE.animeDAO().emissionVotesLimited.observe(this, Observer {
            listBestEmission.updateList(it)
        })
        CacheDB.INSTANCE.queueDAO().all.observe(this, Observer {
            doAsync { listPending.updateList(QueueObject.takeOne(it)) }
        })
        CacheDB.INSTANCE.seeingDAO().getAllWState(SeeingObject.STATE_CONSIDERING, SeeingObject.STATE_PAUSED).observe(this, Observer {
            listWaiting.updateList(it)
        })
        StaffRecommendations.createList {
            listRecommendedStaff.updateList(it)
        }
        viewModel.reload()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        EAHelper.enter1("R")
        Crashlytics.setString("screen", "Home")
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listNew.apply {
            setAdapter(RecentsAdapter(this@HomeFragment, isLarge = false, showSeen = false))
            setViewAllOnClick {
                PrefsUtil.recentLastHiddenNew = lastNew.toInt()
                listNew.hide()
            }
        }
        listFavUpdated.apply {
            setAdapter(RecentsAdapter(this@HomeFragment, true))
            setViewAllClass(RecentsActivity::class.java)
        }
        listBestEmission.setAdapter(DirAdapter(this))
        listPending.apply {
            setAdapter(QueueAdapter(this@HomeFragment))
            setViewAllClass(QueueActivity::class.java)
        }
        listWaiting.apply {
            setAdapter(WaitingAdapter(this@HomeFragment))
            setViewAllClass(SeeingActivity::class.java)
        }
        listRecommended.apply {
            setAdapter(RecommendedAdapter(activity))
            setViewAllClass(RecommendActivity::class.java)
        }
        listRecommendedStaff.setAdapter(SearchAdapter(this))
        adContainer.implBanner(AdsType.HOME_BANNER, true)
        adContainer2.implBanner(AdsType.HOME_BANNER2, true)
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