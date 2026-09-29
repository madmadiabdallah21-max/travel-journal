package com.example.traveljournal.ui.souvenir

import android.content.Intent
import android.content.ClipData
import android.net.Uri
import java.io.File
import android.media.MediaMetadataRetriever
import android.location.Geocoder
import androidx.exifinterface.media.ExifInterface
import java.util.Locale
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.traveljournal.ui.common.AppTopBar
import com.example.traveljournal.ui.common.FullScreenMediaViewer
import com.example.traveljournal.ui.common.VideoPlayer
import com.example.traveljournal.viewmodel.SouvenirViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SouvenirDetailScreen(navController: NavController, souvenirId: Long, souvenirViewModel: SouvenirViewModel) {
    val context = LocalContext.current
    var souvenir by remember { mutableStateOf<com.example.traveljournal.data.model.SouvenirEntity?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showViewer by remember { mutableStateOf(false) }
    var locationLabel by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(souvenirId) {
        souvenir = souvenirViewModel.getSouvenirById(souvenirId)
    }

    souvenir?.let { s ->
        val file = File(s.mediaPath)
        val uri = com.example.traveljournal.util.MediaSaver.getContentUri(context, file)
        
        // Resolve location from media metadata
        LaunchedEffect(uri, s.mediaType) {
            locationLabel = resolveLocationLabel(context, uri, s.mediaType)
        }

        Scaffold(
            topBar = {
                AppTopBar(title = "Souvenir", navController = navController, actions = {
                    IconButton(onClick = {
                        val file = File(s.mediaPath)
                        val uri = com.example.traveljournal.util.MediaSaver.getContentUri(context, file)
                        val mime = if (s.mediaType == "VIDEO") "video/*" else "image/*"
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = mime
                            putExtra(Intent.EXTRA_TEXT, s.description ?: "")
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            clipData = ClipData.newUri(context.contentResolver, "file", uri)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share"))
                    }) { Icon(Icons.Default.Share, contentDescription = "Share") }
                    IconButton(onClick = { showEditDialog = true }) { Icon(Icons.Default.Edit, contentDescription = "Edit") }
                    IconButton(onClick = {
                        souvenirViewModel.deleteSouvenir(s)
                        navController.popBackStack()
                    }) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
                })
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())) {
                if (s.mediaType == "VIDEO") {
                    VideoPlayer(uri = uri)
                } else {
                    // Static image preview (no zoom/pan in detail view)
                    AsyncImage(
                        model = uri,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clickable { showViewer = true }
                    )
                }

                // Full screen viewer as Dialog (shown when showViewer = true)
                if (showViewer) {
                    FullScreenMediaViewer(uri = uri, onClose = { showViewer = false }, mediaType = s.mediaType)
                }
                Spacer(modifier = Modifier.height(8.dp))
                
                // Location label from metadata (discrete style, Instagram-like)
                if (locationLabel != null) {
                    Text(
                        locationLabel!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
                
                Text(s.title ?: "", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(8.dp))
                Text(s.description ?: "", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(8.dp))
            }
        }

        if (showEditDialog) {
            var editTitle by remember { mutableStateOf(s.title ?: "") }
            var editDesc by remember { mutableStateOf(s.description ?: "") }
            AlertDialog(
                onDismissRequest = { showEditDialog = false },
                title = { Text("Modifier souvenir") },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(value = editTitle, onValueChange = { editTitle = it }, label = { Text("Titre") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = editDesc, onValueChange = { editDesc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        souvenirViewModel.updateSouvenir(s.copy(title = editTitle.ifBlank { null }, description = editDesc.ifBlank { null }))
                        showEditDialog = false
                    }) { Text("Enregistrer") }
                },
                dismissButton = {
                    TextButton(onClick = { showEditDialog = false }) { Text("Annuler") }
                }
            )
        }
    } ?: run {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Loading...") }
    }
}

/**
 * Resolve location (city, country) from media metadata.
 * For PHOTO: reads GPS data via ExifInterface.
 * For VIDEO: attempts MediaMetadataRetriever (if available), otherwise returns null.
 * All work is done on Dispatchers.IO.
 */
private suspend fun resolveLocationLabel(
    context: android.content.Context,
    uri: Uri,
    mediaType: String
): String? {
    return withContext(Dispatchers.IO) {
        try {
            when (mediaType) {
                "PHOTO" -> resolvePhotoLocation(context, uri)
                "VIDEO" -> resolveVideoLocation(context, uri)
                else -> null
            }
        } catch (e: Exception) {
            // Best-effort: any error returns null
            null
        }
    }
}

/**
 * Extract location from PHOTO via ExifInterface GPS tags.
 */
private suspend fun resolvePhotoLocation(context: android.content.Context, uri: Uri): String? {
    return withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val exif = ExifInterface(inputStream)
            val latLong = exif.latLong

            if (latLong != null && latLong.size == 2) {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latLong[0], latLong[1], 1)
                
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    val city = addr.locality ?: addr.subAdminArea ?: addr.adminArea
                    val country = addr.countryName
                    return@withContext when {
                        city != null && country != null -> "📍 $city, $country"
                        city != null -> "📍 $city"
                        country != null -> "📍 $country"
                        else -> null
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Extract location from VIDEO via MediaMetadataRetriever (best-effort).
 */
private suspend fun resolveVideoLocation(context: android.content.Context, uri: Uri): String? {
    return withContext(Dispatchers.IO) {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val locationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION)
            retriever.release()
            
            // locationStr format is typically: "-33.8688/151.2093" (lat/lon) or "+33.8688+151.2093"
            // For MVP, we'll attempt basic parsing if it looks like coordinates
            if (locationStr.isNullOrBlank()) return@withContext null
            
            try {
                // Try to parse as lat/lon
                val parts = locationStr.split("/", "+").mapNotNull { it.toDoubleOrNull() }
                if (parts.size >= 2) {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(parts[0], parts[1], 1)
                    
                    if (!addresses.isNullOrEmpty()) {
                        val addr = addresses[0]
                        val city = addr.locality ?: addr.subAdminArea ?: addr.adminArea
                        val country = addr.countryName
                        return@withContext when {
                            city != null && country != null -> "📍 $city, $country"
                            city != null -> "📍 $city"
                            country != null -> "📍 $country"
                            else -> null
                        }
                    }
                }
            } catch (e: Exception) {
                // Parsing failed, return null
            }
            null
        } catch (e: Exception) {
            null
        }
    }
}
