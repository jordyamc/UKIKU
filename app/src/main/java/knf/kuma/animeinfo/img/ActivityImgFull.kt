package knf.kuma.animeinfo.img

import android.os.Bundle
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import knf.kuma.commons.doOnUI
import knf.kuma.commons.execute
import knf.kuma.commons.iterator
import knf.kuma.commons.safeDismiss
import knf.kuma.commons.showSnackbar
import knf.kuma.custom.GenericActivity
import knf.kuma.databinding.LayoutImgBigBaseBinding
import okhttp3.Request
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
        searchInMAL()
    }

    private fun searchInMAL() {
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("scale_img", true))
            doAsync {
                val snackbar = binding.pager.showSnackbar("Buscando mejores imagenes...", Snackbar.LENGTH_INDEFINITE)
                try {
                    val title = intent.getStringExtra(keyTitle)
                    val response = Request.Builder()
                        .url("https://api.jikan.moe/v4/anime?q=${URLEncoder.encode(title, "utf-8")}&page=1")
                            .build().execute()
                    if (response.code != 200)
                        throw IllegalStateException("Response code: ${response.code}")
                    val results = JSONObject(
                        response.body?.string()
                            ?: "{}"
                    ).getJSONArray("data")
                    response.close()
                    for (i in 0 until results.length()) {
                        val json = results.getJSONObject(i)
                        val name = json.getString(keyTitle).lowercase(Locale.getDefault())
                        if (title?.lowercase(Locale.getDefault()) == name) {
                            val list = mutableListOf<String>()
                            //list.add(json.getString("image_url"))
                            try {
                                val picturesResponse = Request.Builder().url("https://api.jikan.moe/v4/anime/${json.getString("mal_id")}/pictures").build().execute()
                                if (picturesResponse.code != 200)
                                    throw IllegalStateException("Response code: ${picturesResponse.code}")
                                val picturesArray = JSONObject(
                                    picturesResponse.body?.string()
                                        ?: "{}"
                                ).getJSONArray("data")
                                picturesResponse.close()
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

    override fun onBackPressed() {
        supportFinishAfterTransition()
    }
}
