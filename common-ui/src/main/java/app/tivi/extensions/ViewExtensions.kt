/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package app.tivi.extensions

import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnLayout
import androidx.core.view.doOnNextLayout
import androidx.core.view.doOnPreDraw
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

fun ViewGroup.beginDelayedTransition(duration: Long = 200) {
    TransitionManager.beginDelayedTransition(this, AutoTransition().apply { setDuration(duration) })
}

fun View.getBounds(rect: Rect) {
    rect.set(left, top, right, bottom)
}

/**
 * Call [View.requestApplyInsets] in a safe away. If we're attached it calls it straight-away.
 * If not it sets an [View.OnAttachStateChangeListener] and waits to be attached before calling
 * [View.requestApplyInsets].
 */
fun View.requestApplyInsetsWhenAttached() = doOnAttach {
    it.requestApplyInsets()
}

fun View.doOnAttach(f: (View) -> Unit) {
    if (isAttachedToWindow) {
        f(this)
    } else {
        addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                f(v)
                removeOnAttachStateChangeListener(this)
            }

            override fun onViewDetachedFromWindow(v: View) {
                removeOnAttachStateChangeListener(this)
            }
        })
    }
}

/**
 * Allows easy listening to layout passing. Return [true] if you need the listener to keep being
 * attached.
 */
inline fun <V : View> V.doOnLayouts(crossinline action: (view: V) -> Boolean) {
    addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
        @Suppress("UNCHECKED_CAST")
        override fun onLayoutChange(
            view: View,
            left: Int,
            top: Int,
            right: Int,
            bottom: Int,
            oldLeft: Int,
            oldTop: Int,
            oldRight: Int,
            oldBottom: Int
        ) {
            if (!action(view as V)) {
                view.removeOnLayoutChangeListener(this)
            }
        }
    })
}

/**
 * Allows easy listening to layout passing. Return [true] if you need the listener to keep being
 * attached.
 */
inline fun View.doOnSizeChange(crossinline action: (view: View) -> Boolean) {
    addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
        override fun onLayoutChange(
            view: View,
            left: Int,
            top: Int,
            right: Int,
            bottom: Int,
            oldLeft: Int,
            oldTop: Int,
            oldRight: Int,
            oldBottom: Int
        ) {
            if ((bottom - top) != (oldBottom - oldTop) || (right - left) != (oldRight - oldLeft)) {
                if (!action(view)) {
                    view.removeOnLayoutChangeListener(this)
                }
            }
        }
    })
}

suspend fun View.awaitNextLayout() = suspendCoroutine<Unit> { cont ->
    doOnNextLayout { cont.resume(Unit) }
}

suspend fun View.awaitLayout() {
    if (isLaidOut) return
    suspendCoroutine<Unit> { cont -> doOnLayout { cont.resume(Unit) } }
}

suspend fun View.awaitPreDraw() = suspendCoroutine<Unit> { cont ->
    doOnPreDraw { cont.resume(Unit) }
}

suspend fun View.awaitAnimationFrame() = suspendCoroutine<Unit> { cont ->
    postOnAnimation { cont.resume(Unit) }
}