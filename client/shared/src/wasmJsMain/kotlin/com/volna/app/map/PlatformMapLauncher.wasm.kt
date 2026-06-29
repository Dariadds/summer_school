package com.volna.app.map

import com.volna.app.domain.model.MeetingPoint
import com.volna.app.domain.model.Route
import kotlinx.browser.window

actual object PlatformMapLauncher : MapLauncher {
    actual override fun openYandexMaps(meetingPoint: MeetingPoint, route: Route?) {
        window.open(meetingPoint.toYandexPointUrl(), "_blank")
    }

    actual override fun buildRouteTo(meetingPoint: MeetingPoint) {
        window.open(meetingPoint.toYandexRouteUrl(), "_blank")
    }
}

