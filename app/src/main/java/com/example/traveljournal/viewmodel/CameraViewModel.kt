package com.example.traveljournal.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.camera.core.CameraSelector


class CameraViewModel : ViewModel() {
    companion object {
        const val ENABLE_VIDEO_CAPTURE = true  // Set to true to enable video recording
    }

    private val _lastMediaPath = MutableStateFlow<String?>(null)
    val lastMediaPath: StateFlow<String?> = _lastMediaPath

    private val _lastMediaType = MutableStateFlow<String>("PHOTO")
    val lastMediaType: StateFlow<String> = _lastMediaType

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    // CameraSelector lens facing state
    private val _lensFacing = MutableStateFlow(CameraSelector.LENS_FACING_BACK)
    val lensFacing: StateFlow<Int> = _lensFacing

    fun setLastMedia(path: String, mediaType: String = "PHOTO") {
        _lastMediaPath.value = path
        _lastMediaType.value = mediaType
    }

    fun setRecording(value: Boolean) {
        _isRecording.value = value
    }

    fun toggleLensFacing() {
        _lensFacing.value = if (_lensFacing.value == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
    }
}
