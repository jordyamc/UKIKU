package knf.kuma.tv;

import android.app.Activity;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;

import java.lang.ref.WeakReference;
import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.leanback.app.BackgroundManager;
import androidx.palette.graphics.Palette;

public class GlideBackgroundManager {

    private static final String TAG = GlideBackgroundManager.class.getSimpleName();
    private static final int BACKGROUND_UPDATE_DELAY = 200;
    public static GlideBackgroundManager instance;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private WeakReference<Activity> mActivityWeakReference;
    private BackgroundManager mBackgroundManager;
    private String mBackgroundURI;
    private Timer mBackgroundTimer;
    private SimpleTarget<Drawable> mGlideDrawableSimpleTarget = new SimpleTarget<Drawable>() {
        @Override
        public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
            Palette.from(((BitmapDrawable) resource).getBitmap()).generate(palette -> {
                Palette.Swatch textSwatch = palette.getDarkMutedSwatch();
                if (textSwatch != null)
                    setBackground(new ColorDrawable(textSwatch.getRgb()));
            });
        }
    };

    /**
     * @param activity The activity to which this WindowManager is attached
     */
    public GlideBackgroundManager(Activity activity) {
        mActivityWeakReference = new WeakReference<>(activity);
        mBackgroundManager = BackgroundManager.getInstance(activity);
        if (!mBackgroundManager.isAttached())
            mBackgroundManager.attach(activity.getWindow());
    }

    public void loadImage(String imageUrl) {
        mBackgroundURI = imageUrl;
        startBackgroundTimer();
    }

    public void setBackground(Drawable drawable) {
        if (mBackgroundManager != null) {
            if (!mBackgroundManager.isAttached()) {
                mBackgroundManager.attach(mActivityWeakReference.get().getWindow());
            }
            mBackgroundManager.setDrawable(drawable);
        }
    }

    /**
     * Cancels an ongoing background change
     */
    public void cancelBackgroundChange() {
        mBackgroundURI = null;
        cancelTimer();
    }

    /**
     * Stops the timer
     */
    private void cancelTimer() {
        if (mBackgroundTimer != null) {
            mBackgroundTimer.cancel();
        }
    }

    /**
     * Starts the background change timer
     */
    private void startBackgroundTimer() {
        cancelTimer();
        mBackgroundTimer = new Timer();
        /* set delay time to reduce too much background image loading process */
        mBackgroundTimer.schedule(new UpdateBackgroundTask(), BACKGROUND_UPDATE_DELAY);
    }

    /**
     * Updates the background with the last known URI
     */
    public void updateBackground() {
        if (mActivityWeakReference.get() != null) {
            Glide.with(mActivityWeakReference.get())
                    .load(mBackgroundURI)
                    .into(mGlideDrawableSimpleTarget);
        }
    }

    private class UpdateBackgroundTask extends TimerTask {
        @Override
        public void run() {
            mHandler.post(() -> {
                if (mBackgroundURI != null) {
                    updateBackground();
                }
            });
        }
    }

}
