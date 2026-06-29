package com.volna.app.map

import com.volna.app.domain.model.MeetingPoint
import com.volna.app.domain.model.Route
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual object PlatformMapLauncher : MapLauncher {
    actual override fun openYandexMaps(meetingPoint: MeetingPoint, route: Route?) {
        open(meetingPoint.toYandexPointUrl())
    }

    actual override fun buildRouteTo(meetingPoint: MeetingPoint) {
        open(meetingPoint.toYandexRouteUrl())
    }

    private fun open(url: String) {
        NSURL.URLWithString(url)?.let { UIApplication.sharedApplication.openURL(it) }
    }
}

