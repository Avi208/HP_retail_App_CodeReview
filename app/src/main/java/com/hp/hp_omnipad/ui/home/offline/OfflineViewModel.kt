package com.hp.hp_omnipad.ui.home.offline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hp.hp_omnipad.data.repository.FirestoreRepository
import com.hp.hp_omnipad.ui.home.model.VideoItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class OfflineViewModel : ViewModel() {

    private val _videos = MutableStateFlow<List<VideoItem>>(emptyList())
    val videos = _videos.asStateFlow()

    init {
        loadVideos()
    }

    private fun loadVideos() {

        viewModelScope.launch {

            _videos.value = FirestoreRepository.getDownloadedVideos()
        }
    }

    fun deleteVideo(video: VideoItem) {

        viewModelScope.launch {

            val folder = File(video.videoUrl).parentFile

            if (folder != null && folder.exists()) {
                folder.deleteRecursively()
            }

            FirestoreRepository.deleteDownloadedVideo(video.id)

            loadVideos()
        }
    }
}