package knf.kuma.player

import android.content.Context
import android.graphics.Point
import android.media.AudioManager
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity


class BVListener(val activity: AppCompatActivity) : View.OnTouchListener {

    private val audioManager: AudioManager = activity.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val screenSize: Point
    private val sWidth: Int
    private val sHeight: Int
    private var intLeft: Boolean = false
    private var intRight: Boolean = false
    private var downX: Float = 0.toFloat()
    private var downY: Float = 0.toFloat()
    private var diffX: Long = 0
    private var diffY: Long = 0

    private val window = activity.window
    private val params = activity.window.attributes
    private var currentBrightness = params.screenBrightness


    init {
        val display = activity.windowManager.defaultDisplay
        screenSize = Point()
        display.getSize(screenSize)
        sWidth = screenSize.x
        sHeight = screenSize.y
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {

                //touch is start
                downX = event.x
                downY = event.y
                if (event.x < sWidth / 2) {

                    //here check touch is screen left or right side
                    intLeft = true
                    intRight = false

                } else if (event.x > sWidth / 2) {

                    //here check touch is screen left or right side
                    intLeft = false
                    intRight = true
                }
                v?.performClick()
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_MOVE -> {

                if (event.action == MotionEvent.ACTION_UP)
                    v?.performClick()

                //finger move to screen
                //val x2 = event.x
                val y2 = event.y

                diffX = Math.ceil((event.x - downX).toDouble()).toLong()
                diffY = Math.ceil((event.y - downY).toDouble()).toLong()

                if (Math.abs(diffY) > Math.abs(diffX)) {
                    if (intLeft) {
                        //if left its for brightness
                        if (downY < y2) {
                            //down swipe brightness decrease
                            currentBrightness -= 0.1f
                            if (currentBrightness < 0) currentBrightness = 0f
                            params.screenBrightness = currentBrightness
                            window.attributes = params
                        } else if (downY > y2) {
                            //up  swipe brightness increase
                            currentBrightness += 0.1f
                            if (currentBrightness > 1) currentBrightness = 1f
                            params.screenBrightness = currentBrightness
                            window.attributes = params
                        }

                    } else if (intRight) {

                        //if right its for audio
                        if (downY < y2) {

                            //down swipe volume decrease
                            audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_PLAY_SOUND)

                        } else if (downY > y2) {

                            //up  swipe volume increase
                            audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND)
                        }
                    }
                }
            }
        }
        return true
    }
}