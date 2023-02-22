package com.maary.shareas

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.service.quicksettings.TileService

class QSTileService: TileService() {
    override fun onClick() {
        super.onClick()

        val intent = Intent(this, HistoryActivity::class.java)
            .addFlags(FLAG_ACTIVITY_NEW_TASK)

        startActivityAndCollapse(intent)
    }
}