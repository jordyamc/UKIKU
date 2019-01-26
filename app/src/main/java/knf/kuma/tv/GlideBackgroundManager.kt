package knf.kuma.tv

import android.app.Activity
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.leanback.app.BackgroundManager
import androidx.palette.graphics.Palette
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import knf.kuma.App
import knf.kuma.commons.doOnUI
import java.lang.ref.WeakReference
import java.util.*

class GlideBackgroundManager(activity: Activity) {
    private val mActivityWeakReference: WeakReference<Activity> = WeakReference(activity)
    private val mBackgroundManager: BackgroundManager? = BackgroundManager.getInstance(activity)
    private var mBackgroundURI: String? = null
    private var mBackgroundTimer: Timer? = null
    private val mGlideDrawableSimpleTarget = object : SimpleTarget<Drawable>() {
        override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
            Palette.from((resource as BitmapDrawable).bitmap).generate { palette ->
                val textSwatch = palette?.darkMutedSwatch
                if (textSwatch != null)
                    setBackground(ColorDrawable(textSwatch.rgb))
            }
        }
    }

    init {
        if (mBackgroundManager?.isAttached == false)
            mBackgroundManager.attach(activity.window)
    }

    fun loadImage(imageUrl: String) {
        mBackgroundURI = imageUrl
        startBackgroundTimer()
    }

    fun setBackground(drawable: Drawable) {
        if (mBackgroundManager != null) {
            if (!mBackgroundManager.isAttached) {
                mBackgroundManager.attach(mActivityWeakReference.get()?.window)
            }
            mBackgroundManager.drawable = drawable
        }
    }

    /**
     * Cancels an ongoing background change
     */
    fun cancelBackgroundChange() {
        mBackgroundURI = null
        cancelTimer()
    }

    /**
     * Stops the timer
     */
    private fun cancelTimer() {
        mBackgroundTimer?.cancel()
    }

    /**
     * Starts the background change timer
     */
    private fun startBackgroundTimer() {
        cancelTimer()
        mBackgroundTimer = Timer()
        /* set delay time to reduce too much background image loading process */
        mBackgroundTimer?.schedule(UpdateBackgroundTask(), BACKGROUND_UPDATE_DELAY.toLong())
    }

    /**
     * Updates the background with the last known URI
     */
    fun updateBackground() {
        Glide.with(App.context)
                .load(mBackgroundURI)
                .into<SimpleTarget<Drawable>>(mGlideDrawableSimpleTarget)
    }

    private inner class UpdateBackgroundTask : TimerTask() {
        override fun run() {
            doOnUI {
                if (mBackgroundURI != null) {
                    updateBackground()
                }
            }
        }
    }

    companion object {

        private val TAG = GlideBackgroundManager::class.java.simpleName
        private const val BACKGROUND_UPDATE_DELAY = 200
        var instance: GlideBackgroundManager? = null
    }

}
