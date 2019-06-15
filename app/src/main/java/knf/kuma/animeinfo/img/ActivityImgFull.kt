package knf.kuma.animeinfo.img

import android.os.Bundle
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import knf.kuma.R
import knf.kuma.commons.*
import knf.kuma.custom.GenericActivity
import kotlinx.android.synthetic.main.layout_img_big_base.*
import okhttp3.Request
import org.jetbrains.anko.doAsync
import org.json.JSONObject
import java.net.URLEncoder

class ActivityImgFull : GenericActivity() {

    private val keyTitle = "title"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_img_big_base)
        pager.adapter = ImgPagerAdapter(supportFragmentManager, intent.getStringExtra(keyTitle)
                ?: "", listOf(intent.dataString ?: ""))
        indicator.setViewPager(pager)
        searchInMAL()
    }

    private fun searchInMAL() {
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("scale_img", true))
            doAsync {
                val snackbar = pager.showSnackbar("Buscando mejores imagenes...", Snackbar.LENGTH_INDEFINITE)
                try {
                    val title = intent.getStringExtra(keyTitle)
                    val response = Request.Builder()
                            .url("https://api.jikan.moe/v3/search/anime?q=${URLEncoder.encode(title, "utf-8")}&page=1")
                            .build().execute()
                    if (response.code() != 200)
                        throw IllegalStateException("Response code: ${response.code()}")
                    val results = JSONObject(response.body()?.string()
                            ?: "{}").getJSONArray("results")
                    response.close()
                    for (i in 0 until results.length()) {
                        val json = results.getJSONObject(i)
                        val name = json.getString(keyTitle).toLowerCase()
                        if (title.toLowerCase() == name) {
                            val list = mutableListOf<String>()
                            list.add(json.getString("image_url"))
                            try {
                                val picturesResponse = Request.Builder().url("https://api.jikan.moe/v3/anime/${json.getString("mal_id")}/pictures").build().execute()
                                if (picturesResponse.code() != 200)
                                    throw IllegalStateException("Response code: ${picturesResponse.code()}")
                                val picturesArray = JSONObject(picturesResponse.body()?.string()
                                        ?: "{}").getJSONArray("pictures")
                                picturesResponse.close()
                                for (item in picturesArray) {
                                    list.add(item.getString("large"))
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            doOnUI {
                                pager.adapter = ImgPagerAdapter(supportFragmentManager, intent.getStringExtra(keyTitle), list)
                                indicator.setViewPager(pager)
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
