package knf.kuma.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.lifecycle.lifecycleScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import knf.kuma.R
import knf.kuma.commons.EAHelper
import knf.kuma.commons.setSurfaceBars
import knf.kuma.custom.GenericActivity
import knf.kuma.custom.VariantLinearLayoutManager
import knf.kuma.database.CacheDB
import knf.kuma.databinding.RecyclerGenreMaterialBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class GenreActivityMaterial : GenericActivity() {
    private val adapter: GenreAdapterMaterial by lazy { GenreAdapterMaterial(this) }
    private var isFirst = true
    private val binding by lazy { RecyclerGenreMaterialBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(EAHelper.getTheme())
        super.onCreate(savedInstanceState)
        setSurfaceBars()
        setContentView(binding.root)
        binding.toolbar.title = intent.getStringExtra("name")
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(false)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.recycler.layoutManager = VariantLinearLayoutManager(this)
        binding.recycler.layoutAnimation = AnimationUtils.loadLayoutAnimation(this, R.anim.layout_fall_down)
        binding.recycler.adapter = adapter
        lifecycleScope.launch {
            Pager(
                config = PagingConfig(25), 0,
                CacheDB.INSTANCE.animeDAO().getAllGenre("%" + intent.getStringExtra("name") + "%").asPagingSourceFactory()
            ).flow.collectLatest {
                adapter.submitData(it)
                binding.progress.visibility = View.GONE
                if (isFirst) {
                    isFirst = false
                    binding.recycler.scheduleLayoutAnimation()
                }
            }
        }
    }

    companion object {

        fun open(context: Context, name: String) {
            val intent = Intent(context, GenreActivityMaterial::class.java)
            intent.putExtra("name", name)
            context.startActivity(intent)
        }
    }
}
