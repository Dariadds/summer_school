package com.volna.app.map

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.volna.app.domain.model.MeetingPoint
import com.volna.app.domain.model.Route

actual object PlatformMapLauncher : MapLauncher {
    private var context: Context? = null

    fun initialize(context: Context) {
        this.context = context.applicationContext
    }

    actual override fun openYandexMaps(meetingPoint: MeetingPoint, route: Route?) {
        val pointUri = Uri.parse(meetingPoint.toYandexPointUrl())
        open(pointUri)
    }

    actual override fun buildRouteTo(meetingPoint: MeetingPoint) {
        val routeUri = Uri.parse(meetingPoint.toYandexRouteUrl())
        open(routeUri)
    }

    private fun open(uri: Uri) {
        val appContext = context ?: return
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { appContext.startActivity(intent) }
    }
}

