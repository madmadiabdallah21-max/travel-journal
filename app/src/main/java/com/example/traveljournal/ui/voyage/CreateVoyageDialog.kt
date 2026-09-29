package com.example.traveljournal.ui.voyage

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.traveljournal.data.model.VoyageEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.File

@Composable
fun CreateVoyageDialog(
    voyage: VoyageEntity? = null,
    onDismiss: () -> Unit,
    onCreate: (title: String, description: String?, thumbnailPath: String?) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(voyage?.title ?: "") }
    var description by remember { mutableStateOf(voyage?.description ?: "") }
    var savedThumbnailPath by remember { mutableStateOf(voyage?.thumbnailPath) }
    var isCopyingThumbnail by remember { mutableStateOf(false) }
    var thumbnailCopyFailed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val canCreate = title.isNotBlank()

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { 
            scope.launch {
                isCopyingThumbnail = true
                thumbnailCopyFailed = false
                try {
                    val copied = withContext(Dispatchers.IO) { copyImageToMedia(context, it) }
                    savedThumbnailPath = copied.absolutePath
                } catch (exception: Exception) {
                    if (exception is CancellationException) throw exception
                    thumbnailCopyFailed = true
                } finally {
                    isCopyingThumbnail = false
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (voyage == null) "Nouveau carnet" else "Modifier carnet") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Titre (obligatoire)") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description (optionnelle)") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(12.dp))
                if (thumbnailCopyFailed) {
                    Text("Impossible d’enregistrer cette image.")
                }
                Row {
                    if (savedThumbnailPath != null) {
                        AsyncImage(model = savedThumbnailPath, contentDescription = "Thumbnail", modifier = Modifier.size(80.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    IconButton(onClick = { imagePicker.launch("image/*") }) {
                        Icon(Icons.Default.Image, contentDescription = "Sélectionner image")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = canCreate && !isCopyingThumbnail,
                onClick = { if (canCreate && !isCopyingThumbnail) onCreate(title.trim(), description.ifBlank { null }, savedThumbnailPath) }
            ) {
                Text(if (voyage == null) "Créer" else "Modifier") 
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

private fun copyImageToMedia(context: Context, uri: Uri): File {
    val mediaDir = File(context.filesDir, "media")
    if (!mediaDir.exists() && !mediaDir.mkdirs()) {
        throw IOException("Impossible de créer le répertoire média")
    }
    val mimeType = context.contentResolver.getType(uri)
        ?: throw IOException("Type MIME de l’image indisponible")
    val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
        ?: throw IOException("Extension de l’image indisponible")
    val out = File(mediaDir, "THUMB_${System.currentTimeMillis()}.$extension")
    try {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Impossible d’ouvrir l’image sélectionnée")
        input.use { source ->
            out.outputStream().use { output -> source.copyTo(output) }
        }
        return out
    } catch (exception: Exception) {
        out.delete()
        throw exception
    }
}
