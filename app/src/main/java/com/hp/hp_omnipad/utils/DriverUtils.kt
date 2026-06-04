package com.hp.hp_omnipad.utils

fun convertDriveUrl(url: String): String {

    val regex = "file/d/(.*?)/".toRegex()
    val match = regex.find(url)

    return if (match != null) {
        val id = match.groupValues[1]
        "https://drive.google.com/thumbnail?id=$id&sz=w1000"
    } else {
        url
    }
}