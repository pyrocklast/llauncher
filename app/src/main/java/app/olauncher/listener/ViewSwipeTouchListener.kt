package app.olauncher.listener

import android.content.Context
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewConfiguration
import kotlin.math.abs

internal open class ViewSwipeTouchListener(c: Context?, v: View) : OnTouchListener {
    private val gestureDetector: GestureDetector
    private val swipeDistancePx = (40 * (c?.resources?.displayMetrics?.density ?: 2.5f)).toInt()
    private val swipeVelocityPx = c?.let { ViewConfiguration.get(it).scaledMinimumFlingVelocity } ?: 100

    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
        when (motionEvent.action) {
            MotionEvent.ACTION_DOWN -> view.isPressed = true
            MotionEvent.ACTION_UP -> view.isPressed = false
        }
        return gestureDetector.onTouchEvent(motionEvent)
    }

    private inner class GestureListener(private val view: View) : SimpleOnGestureListener() {
        // Lite: in screen-independent units instead of raw pixels, so swipes feel the same on every
        // screen: 40 dp of travel (about what upstream's 100 px is on a typical phone), at least
        // Android's own minimum fling speed
        private val SWIPE_THRESHOLD: Int = swipeDistancePx
        private val SWIPE_VELOCITY_THRESHOLD: Int = swipeVelocityPx

        override fun onDown(e: MotionEvent): Boolean {
            return true
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            onClick(view)
            return super.onSingleTapUp(e)
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            onDoubleClick()
            return super.onDoubleTap(e)
        }

        // Lite: fire at the system long-press time (Android's "Touch & hold delay" setting),
        // instead of waiting a further 500 ms on a timer that outlived the screen
        override fun onLongPress(e: MotionEvent) {
            // Lite: the usual long-press buzz, following the phone's touch feedback setting
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            onLongClick(view)
            super.onLongPress(e)
        }

        override fun onFling(
            event1: MotionEvent?,
            event2: MotionEvent,
            velocityX: Float,
            velocityY: Float,
        ): Boolean {
            try {
                val diffY = event2.y - (event1?.y ?: 0F)
                val diffX = event2.x - (event1?.x ?: 0F)
                if (abs(diffX) > abs(diffY)) {
                    if (abs(diffX) > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) onSwipeRight() else onSwipeLeft()
                    }
                } else {
                    if (abs(diffY) > SWIPE_THRESHOLD && abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffY < 0) onSwipeUp() else onSwipeDown()
                    }
                }
            } catch (exception: Exception) {
                exception.printStackTrace()
            }
            return false
        }
    }

    open fun onSwipeRight() {}
    open fun onSwipeLeft() {}
    open fun onSwipeUp() {}
    open fun onSwipeDown() {}
    open fun onLongClick(view: View) {}
    private fun onDoubleClick() {}
    open fun onClick(view: View) {}

    init {
        gestureDetector = GestureDetector(c, GestureListener(v))
    }
}