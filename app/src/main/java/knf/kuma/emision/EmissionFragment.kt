package knf.kuma.emision

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import knf.kuma.R
import knf.kuma.ads.AdsType
import knf.kuma.ads.implBanner
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.verifyManager
import knf.kuma.database.CacheDB
import knf.kuma.databinding.RecyclerEmisionBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class EmissionFragment : Fragment() {
    private val adapter: EmissionAdapter by lazy { EmissionAdapter(this) }
    private var isFirst = true

    private lateinit var binding: RecyclerEmisionBinding

    private val vm: EmissionViewModel by activityViewModels<EmissionViewModel>()
    private val day by lazy { arguments?.getInt("day", 1) ?: 1 }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return LayoutInflater.from(context).inflate(R.layout.recycler_emision, container, false).also {
            binding = RecyclerEmisionBinding.bind(it)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            delay(1.seconds)
            binding.adContainer.implBanner(AdsType.EMISSION_BANNER, true)
        }
        binding.recycler.verifyManager()
        binding.recycler.adapter = adapter
        viewLifecycleOwner.lifecycleScope.launch {
            vm.emissionFlow.first { it.isNotEmpty() }
            PrefsUtil.emissionShowHiddenFlow().flatMapLatest { showHidden ->
                if (showHidden) {
                    CacheDB.INSTANCE.calendarBlacklistDAO().allForDayFlow(day)
                } else {
                    CacheDB.INSTANCE.calendarBlacklistDAO().notHiddenForDayFlow(day)
                }
            }.collectLatest {
                binding.progress.visibility = View.GONE
                adapter.update(it.onEach { it.isFavorite = CacheDB.INSTANCE.favoriteAV1DAO().isFavSuspend(it.aid) }, !isFirst)
                if (isFirst) {
                    isFirst = false
                    binding.recycler.scheduleLayoutAnimation()
                }
                binding.error.isVisible = it.isNullOrEmpty()
            }
        }
    }

    companion object {

        operator fun get(day: Int): EmissionFragment {
            val bundle = Bundle()
            bundle.putInt("day", day)
            val fragment = EmissionFragment()
            fragment.arguments = bundle
            return fragment
        }
    }
}
