package com.example.traveljournal.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

object MediaSaver {
    private const val MEDIA_DIR = "media"

    fun ensureMediaDir(context: Context): File {
        val dir = File(context.filesDir, MEDIA_DIR)
        if (!dir.exists() && !dir.mkdirs()) {
            throw IOException("Impossible de créer le répertoire média")
        }
        return dir
    }

    fun createImageFile(context: Context): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        val fileName = "IMG_${timestamp}.jpg"
        return File(ensureMediaDir(context), fileName)
    }

    fun createVideoFile(context: Context): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        val fileName = "VID_${timestamp}.mp4"
        return File(ensureMediaDir(context), fileName)
    }

    fun getContentUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
    }
}
