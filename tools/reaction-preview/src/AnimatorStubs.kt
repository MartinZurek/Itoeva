package android.animation

/** Attrappen fuer die Offline-Uebersetzung; Zeitverlauf und Android-Player prueft die CI. */
open class Animator

open class AnimatorListenerAdapter {
    open fun onAnimationEnd(animation: Animator) {}
}

class ValueAnimator private constructor() : Animator() {
    var duration: Long = 0L
    val animatedValue: Any = 1f

    fun addUpdateListener(listener: (ValueAnimator) -> Unit) {}
    fun addListener(listener: AnimatorListenerAdapter) {}
    fun removeAllListeners() {}
    fun start() {}
    fun cancel() {}

    companion object {
        @JvmStatic
        fun ofFloat(vararg values: Float): ValueAnimator = ValueAnimator()
    }
}
