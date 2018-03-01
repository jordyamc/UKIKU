package knf.kuma.updater;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.thin.downloadmanager.DownloadRequest;
import com.thin.downloadmanager.DownloadStatusListenerV1;
import com.thin.downloadmanager.ThinDownloadManager;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import knf.kuma.R;
import xdroid.toaster.Toaster;

public class UpdateActivity extends AppCompatActivity {

    @BindView(R.id.rel_back)
    RelativeLayout back;
    @BindView(R.id.card)
    CardView cardView;
    @BindView(R.id.progress)
    ProgressBar progressBar;
    @BindView(R.id.progress_text)
    TextView progress_indicator;
    @BindView(R.id.download)
    Button download;

    public static void start(Context context) {
        context.startActivity(new Intent(context, UpdateActivity.class));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_updater);
        ButterKnife.bind(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS |
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        if (savedInstanceState == null) {
            AnimationDrawable animationDrawable = (AnimationDrawable) back.getBackground();
            animationDrawable.setEnterFadeDuration(2500);
            animationDrawable.setExitFadeDuration(2500);
            animationDrawable.start();
            progressBar.setMax(100);
            progressBar.setIndeterminate(true);
            showCard();
        }
    }

    private void showCard() {
        cardView.post(new Runnable() {
            @Override
            public void run() {
                Rect bounds = new Rect();
                cardView.getDrawingRect(bounds);
                int centerX = bounds.centerX();
                int centerY = bounds.centerY();
                int finalRadius = Math.max(bounds.width(), bounds.height());

                Animator anim = ViewAnimationUtils.createCircularReveal(cardView, centerX, centerY, 0f, finalRadius);
                anim.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        start();
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                });
                cardView.setVisibility(View.VISIBLE);
                anim.start();
            }
        });
    }

    private void start() {
        File file = getUpdate();
        if (file.exists())
            file.delete();
        new ThinDownloadManager().add(new DownloadRequest(Uri.parse("https://github.com/jordyamc/UKIKU/raw/master/app/release/app-release.apk"))
                .setDestinationURI(Uri.fromFile(file))
                .setDownloadResumable(false)
                .setStatusListener(new DownloadStatusListenerV1() {
                    @Override
                    public void onDownloadComplete(DownloadRequest downloadRequest) {
                        prepareForIntall();
                    }

                    @Override
                    public void onDownloadFailed(DownloadRequest downloadRequest, int errorCode, String errorMessage) {
                        Log.e("Update Error", "Code: " + errorCode + " Message: " + errorMessage);
                        Toaster.toast("Error al actualizar: "+errorMessage);
                        finish();
                    }

                    @Override
                    public void onProgress(DownloadRequest downloadRequest, long totalBytes, long downloadedBytes, int progress) {
                        setDownProgress(progress);
                    }
                }));
    }

    @OnClick(R.id.download)
    void install(Button button) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE, FileProvider.getUriForFile(this, "knf.kuma.fileprovider", getUpdate()))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    .putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, false)
                    .putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, getPackageName());
            startActivity(intent);
        } else {
            Intent intent = new Intent(Intent.ACTION_VIEW)
                    .setDataAndType(Uri.fromFile(getUpdate()), "application/vnd.android.package-archive")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            startActivity(intent);
        }
        finish();
    }

    private File getUpdate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            return new File(getFilesDir(), "update.apk");
        else
            return new File(getDownloadsDir(), "update.apk");
    }

    private File getDownloadsDir() {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    }

    private void setDownProgress(final int progress) {
        runOnUiThread(new Runnable() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {
                progressBar.setIndeterminate(false);
                progressBar.setProgress(progress);
                progress_indicator.setText(progress + "%");
            }
        });
    }

    private void prepareForIntall() {
        setDownProgress(100);
        final Animation fadein = AnimationUtils.loadAnimation(this, R.anim.fadein);
        fadein.setDuration(1000);
        final Animation fadeout = AnimationUtils.loadAnimation(this, R.anim.fadeout);
        fadeout.setDuration(1000);
        progress_indicator.post(new Runnable() {
            @Override
            public void run() {
                progress_indicator.setVisibility(View.INVISIBLE);
                progress_indicator.startAnimation(fadeout);
            }
        });
        download.post(new Runnable() {
            @Override
            public void run() {
                download.setVisibility(View.VISIBLE);
                download.startAnimation(fadein);
            }
        });
    }
}
