package com.example.xargoosh.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import android.content.ComponentName
import com.example.xargoosh.service.XargooshMediaService
import com.google.common.util.concurrent.MoreExecutors
import java.util.concurrent.atomic.AtomicBoolean

class WidgetActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in setOf("PLAY_PAUSE", "NEXT", "PREV")) return
        val pendingResult = goAsync()
        val sessionToken = SessionToken(context, ComponentName(context, XargooshMediaService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        val completed = AtomicBoolean(false)
        val handler = Handler(Looper.getMainLooper())
        val timeout = Runnable {
            if (completed.compareAndSet(false, true)) {
                controllerFuture.cancel(true)
                MediaController.releaseFuture(controllerFuture)
                pendingResult.finish()
            }
        }
        handler.postDelayed(timeout, 8_000L)
        controllerFuture.addListener({
            if (!completed.compareAndSet(false, true)) return@addListener
            handler.removeCallbacks(timeout)
            try {
                val controller = controllerFuture.get()
                when (action) {
                    "PLAY_PAUSE" -> {
                        if (controller.isPlaying) controller.pause() else controller.play()
                    }
                    "NEXT" -> controller.seekToNextMediaItem()
                    "PREV" -> controller.seekToPreviousMediaItem()
                }
            } catch (_: Exception) {
            } finally {
                MediaController.releaseFuture(controllerFuture)
                pendingResult.finish()
            }
        }, MoreExecutors.directExecutor())
    }
}
