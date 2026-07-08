package knf.kuma.animeinfo.img

import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import knf.kuma.commons.doOnUI
import knf.kuma.commons.httpJsoup
import knf.kuma.commons.iterator
import knf.kuma.commons.safeDismiss
import knf.kuma.commons.showSnackbar
import knf.kuma.custom.GenericActivity
import knf.kuma.databinding.LayoutImgBigBaseBinding
import org.jetbrains.anko.doAsync
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale

class ActivityImgFull : GenericActivity() {

    private val keyTitle = "title"
    private val binding by lazy { LayoutImgBigBaseBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.pager.adapter = ImgPagerAdapter(supportFragmentManager, intent.getStringExtra(keyTitle)
                ?: "", listOf(intent.dataString ?: ""))
        binding.indicator.setViewPager(binding.pager)
        ViewCompat.setOnApplyWindowInsetsListener(binding.indicator) { _, insets ->
            binding.indicator.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin += insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            }
            WindowInsetsCompat.CONSUMED
        }
        searchInMAL()
        onBackPressedDispatcher.addCallback(this) { supportFinishAfterTransition() }
    }

    private fun searchInMAL() {
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("scale_img", true))
            doAsync {
                val snackbar = binding.pager.showSnackbar("Buscando mejores imagenes...", Snackbar.LENGTH_INDEFINITE)
                try {
                    val title = intent.getStringExtra(keyTitle)
                    val response = httpJsoup("https://api.jikan.moe/v4/anime?q=${URLEncoder.encode(title, "utf-8")}&page=1")
                    if (response.statusCode() != 200)
                        throw IllegalStateException("Response code: ${response.statusCode()}")
                    val results = JSONObject(
                        response.body()
                            ?: "{}"
                    ).getJSONArray("data")
                    for (i in 0 until results.length()) {
                        val json = results.getJSONObject(i)
                        val name = json.getJSONArray("titles").getJSONObject(0).getString("title").lowercase(Locale.getDefault())
                        if (title?.lowercase(Locale.getDefault()) == name) {
                            val list = mutableListOf<String>()
                            //list.add(json.getString("image_url"))
                            try {
                                val picturesResponse = httpJsoup("https://api.jikan.moe/v4/anime/${json.getString("mal_id")}/pictures")
                                if (picturesResponse.statusCode() != 200)
                                    throw IllegalStateException("Response code: ${picturesResponse.statusCode()}")
                                val picturesArray = JSONObject(
                                    picturesResponse.body()
                                        ?: "{}"
                                ).getJSONArray("data")
                                for (item in picturesArray) {
                                    list.add(item.getJSONObject("jpg").getString("large_image_url"))
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            doOnUI {
                                binding.pager.adapter = ImgPagerAdapter(supportFragmentManager, intent.getStringExtra(keyTitle)
                                        ?: "", list)
                                binding.indicator.setViewPager(binding.pager)
                            }
                            break
                        }
                    }
                    snackbar.safeDismiss()
                } catch (e: Exception) {
                    e.printStackTrace()
                    snackbar.safeDismiss()
                }
            }
    }
}
