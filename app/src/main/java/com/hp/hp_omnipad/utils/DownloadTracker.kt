package com.hp.hp_omnipad.utils

import android.app.DownloadManager
import android.content.Context

object DownloadTracker {

    fun getDownloadProgress(
        context: Context,
        downloadId: Long
    ): Int {

        val manager =
            context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        val query = DownloadManager.Query().setFilterById(downloadId)

        manager.query(query).use { cursor ->

            if (cursor.moveToFirst()) {

                val downloaded =
                    cursor.getInt(
                        cursor.getColumnIndexOrThrow(
                            DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR
                        )
                    )

                val total =
                    cursor.getInt(
                        cursor.getColumnIndexOrThrow(
                            DownloadManager.COLUMN_TOTAL_SIZE_BYTES
                        )
                    )

                if (total > 0) {
                    return (downloaded * 100L / total).toInt()
                }
            }
        }

        return 0
    }
}