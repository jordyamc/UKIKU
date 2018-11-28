package knf.kuma.animeinfo

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.view.MenuItem
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import com.google.android.material.snackbar.Snackbar
import com.squareup.picasso.Callback
import knf.kuma.R
import knf.kuma.commons.*
import kotlinx.android.synthetic.main.layout_img_big.*
import okhttp3.Request
import org.jetbrains.anko.doAsync
import org.json.JSONObject
import xdroid.toaster.Toaster
import java.io.FileOutputStream
import java.net.URLEncoder

class ActivityImgFull : AppCompatActivity(), PopupMenu.OnMenuItemClickListener {

    private var bitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_img_big)
        PicassoSingle[this].load(intent.data).into(img, object : Callback {
            override fun onSuccess() {
                bitmap = (img.drawable as BitmapDrawable).bitmap
            }

            override fun onError() {
                Toaster.toast("Error al cargar imagen")
                finish()
            }
        })
        img.setOnLongClickListener {
            if (bitmap != null) {
                val popupMenu = PopupMenu(this@ActivityImgFull, anchor!!)
                popupMenu.inflate(R.menu.menu_img)
                popupMenu.setOnMenuItemClickListener(this@ActivityImgFull)
                popupMenu.show()
            }
            true
        }
        searchInMAL()
    }

    private fun searchInMAL() {
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("scale_img", true))
            doAsync {
                val snackbar = img.showSnackbar("Buscando mejor resolucion...", Snackbar.LENGTH_INDEFINITE)
                try {
                    val title = intent.getStringExtra("title")
                    val response = Request.Builder()
                            .url("https://api.jikan.moe/v3/search/anime?q=${URLEncoder.encode(title, "utf-8")}&page=1")
                            .build().execute()
                    if (response.code() != 200)
                        throw IllegalStateException("Response code: ${response.code()}")
                    val results = JSONObject(response.body()!!.string()).getJSONArray("results")
                    response.close()
                    for (i in 0..(results.length() - 1)) {
                        val json = results.getJSONObject(i)
                        val name = json.getString("title").toLowerCase()
                        if (title.toLowerCase() == name) {
                            var imgUrl: String = json.getString("image_url")
                            try {
                                val picturesResponse = Request.Builder().url("https://api.jikan.moe/v3/anime/${json.getString("mal_id")}/pictures").build().execute()
                                if (picturesResponse.code() != 200)
                                    throw IllegalStateException("Response code: ${picturesResponse.code()}")
                                val picturesArray = JSONObject(picturesResponse.body()!!.string()).getJSONArray("pictures")
                                picturesResponse.close()
                                imgUrl = picturesArray.getJSONObject(picturesArray.length() - 1).getString("large")
                            } catch (e: Exception) {
                                //
                            }
                            doOnUI {
                                PicassoSingle[this@ActivityImgFull].load(imgUrl).into(img, object : Callback {
                                    override fun onSuccess() {
                                        bitmap = (img.drawable as BitmapDrawable).bitmap
                                    }

                                    override fun onError() {

                                    }
                                })
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

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.download -> {
                val i = Intent(Intent.ACTION_CREATE_DOCUMENT)
                        .addCategory(Intent.CATEGORY_OPENABLE)
                        .setType("image/jpg")
                        .putExtra(Intent.EXTRA_TITLE, intent.getStringExtra("title") + ".jpg")
                startActivityForResult(i, 556)
            }
            R.id.share -> try {
                val intent = Intent(Intent.ACTION_SEND)
                        .setType("image/*")
                        .putExtra(Intent.EXTRA_TEXT, intent.getStringExtra("title"))
                        .putExtra(Intent.EXTRA_STREAM, Uri.parse(MediaStore.Images.Media.insertImage(contentResolver, bitmap, "", "")))
                startActivity(Intent.createChooser(intent, "Compartir..."))
            } catch (e: Exception) {
                Toaster.toast("Error al compartir")
            }
        }
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val snackbar = img.createSnackbar("Guardando...", Snackbar.LENGTH_INDEFINITE)
        val progressBar = ProgressBar(this).also {
            it.isIndeterminate = true
        }
        (snackbar.view as Snackbar.SnackbarLayout).addView(progressBar)
        snackbar.show()
        doAsync {
            try {
                val pfd = contentResolver.openFileDescriptor(data!!.data!!, "w")
                val fileOutputStream = FileOutputStream(pfd!!.fileDescriptor)
                bitmap!!.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)
                fileOutputStream.close()
                pfd.close()
                snackbar.safeDismiss()
                img.showSnackbar("Image guardada!")
            } catch (e: Exception) {
                e.printStackTrace()
                snackbar.safeDismiss()
                img.showSnackbar("Error al guardar imagen")
            }
        }
    }

    override fun onBackPressed() {
        supportFinishAfterTransition()
    }
}
