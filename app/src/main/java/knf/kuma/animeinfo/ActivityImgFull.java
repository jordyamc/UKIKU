package knf.kuma.animeinfo;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.squareup.picasso.Callback;

import java.io.FileOutputStream;

import butterknife.BindView;
import butterknife.ButterKnife;
import knf.kuma.R;
import knf.kuma.commons.PicassoSingle;
import xdroid.toaster.Toaster;

/**
 * Created by Jordy on 15/01/2018.
 */

public class ActivityImgFull extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener {

    @BindView(R.id.img)
    ImageView imageView;
    @BindView(R.id.anchor)
    View anchor;
    private Bitmap bitmap;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_img_big);
        ButterKnife.bind(this);
        PicassoSingle.get(this).load(getIntent().getData()).into(imageView, new Callback() {
            @Override
            public void onSuccess() {
                bitmap=((BitmapDrawable) imageView.getDrawable()).getBitmap();
            }

            @Override
            public void onError() {

            }
        });
        imageView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                PopupMenu popupMenu = new PopupMenu(ActivityImgFull.this, anchor);
                popupMenu.inflate(R.menu.menu_img);
                popupMenu.setOnMenuItemClickListener(ActivityImgFull.this);
                popupMenu.show();
                return true;
            }
        });
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.download:
                Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                        .addCategory(Intent.CATEGORY_OPENABLE)
                        .setType("image/jpg")
                        .putExtra(Intent.EXTRA_TITLE, getIntent().getStringExtra("title")+".jpg");
                startActivityForResult(i, 556);
                break;
            case R.id.share:
                Intent intent = new Intent(Intent.ACTION_SEND)
                        .setType("image/*")
                        .putExtra(Intent.EXTRA_SUBJECT, getIntent().getStringExtra("title"))
                        .putExtra(Intent.EXTRA_STREAM, Uri.parse(MediaStore.Images.Media.insertImage(getContentResolver(),bitmap , "", "")));
                startActivity(Intent.createChooser(intent, "Compartir..."));
                break;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        final MaterialDialog dialog = new MaterialDialog.Builder(this)
                .progress(true, 100)
                .content("Guardando...")
                .cancelable(false)
                .build();
        dialog.show();
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    ParcelFileDescriptor pfd = getContentResolver().
                            openFileDescriptor(data.getData(), "w");
                    FileOutputStream fileOutputStream =
                            new FileOutputStream(pfd.getFileDescriptor());
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
                    fileOutputStream.close();
                    pfd.close();
                    Toaster.toast("Image guardada!");
                } catch (Exception e) {
                    e.printStackTrace();
                    Toaster.toast("Error al guardar imagen");
                }
                dialog.dismiss();
            }
        });
    }

    @Override
    public void onBackPressed() {
        supportFinishAfterTransition();
    }
}
