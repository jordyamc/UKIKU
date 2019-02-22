package knf.kuma.animeinfo.img

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.squareup.picasso.Callback
import knf.kuma.R
import knf.kuma.commons.createSnackbar
import knf.kuma.commons.load
import knf.kuma.commons.safeDismiss
import knf.kuma.commons.showSnackbar
import kotlinx.android.synthetic.main.layout_img_big.*
import kotlinx.android.synthetic.main.layout_img_big.view.*
import org.jetbrains.anko.doAsync
import xdroid.toaster.Toaster
import java.io.FileOutputStream

class ImgFullFragment : Fragment(), PopupMenu.OnMenuItemClickListener {

    private var bitmap: Bitmap? = null
    private val keyTitle = "title"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.layout_img_big, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.img.load(arguments?.getString("img"), object : Callback {
            override fun onSuccess() {
                bitmap = (view.img.drawable as BitmapDrawable).bitmap
            }

            override fun onError() {
                view.error.visibility = View.VISIBLE
            }
        })
        view.img.setOnLongClickListener {
            if (bitmap != null) {
                context?.let {
                    val popupMenu = PopupMenu(it, view.anchor)
                    popupMenu.inflate(R.menu.menu_img)
                    popupMenu.setOnMenuItemClickListener(this@ImgFullFragment)
                    popupMenu.show()
                }
            }
            true
        }
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.download -> {
                val i = Intent(Intent.ACTION_CREATE_DOCUMENT)
                        .addCategory(Intent.CATEGORY_OPENABLE)
                        .setType("image/jpg")
                        .putExtra(Intent.EXTRA_TITLE, arguments?.getString(keyTitle) + ".jpg")
                startActivityForResult(i, 556)
            }
            R.id.share -> try {
                val intent = Intent(Intent.ACTION_SEND)
                        .setType("image/*")
                        .putExtra(Intent.EXTRA_TEXT, arguments?.getString(keyTitle))
                        .putExtra(Intent.EXTRA_STREAM, Uri.parse(MediaStore.Images.Media.insertImage(context?.contentResolver, bitmap, "", "")))
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
        val progressBar = ProgressBar(context).also {
            it.isIndeterminate = true
        }
        (snackbar.view as Snackbar.SnackbarLayout).addView(progressBar)
        snackbar.show()
        doAsync {
            try {
                val pfd = context?.contentResolver?.openFileDescriptor(data?.data ?: Uri.EMPTY, "w")
                pfd?.let {
                    val fileOutputStream = FileOutputStream(it.fileDescriptor)
                    bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)
                    fileOutputStream.close()
                    it.close()
                    snackbar.safeDismiss()
                    img.showSnackbar("Imagen guardada!")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                snackbar.safeDismiss()
                img.showSnackbar("Error al guardar imagen")
            }
        }
    }


    companion object {
        fun create(img: String, title: String): ImgFullFragment {
            val fragment = ImgFullFragment()
            fragment.arguments = Bundle().apply {
                putString("img", img)
                putString("title", title)
            }
            return fragment
        }
    }
}