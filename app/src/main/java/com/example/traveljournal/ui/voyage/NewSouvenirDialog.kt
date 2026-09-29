package com.example.traveljournal.ui.voyage

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.example.traveljournal.data.model.VoyageEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.File

@Composable
fun NewSouvenirDialog(
    pickedUri: Uri,
    voyages: List<VoyageEntity>,
    currentVoyageId: Long?,
    onDismiss: () -> Unit,
    onAdd: (mediaPath: String, mediaType: String, voyageId: Long, title: String?, description: String?) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var selectedVoyage by remember { mutableStateOf(currentVoyageId ?: (voyages.firstOrNull()?.id ?: 0L)) }
    var isSaving by remember { mutableStateOf(false) }
    var saveFailed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(onDismissRequest = onDismiss, confirmButton = {
        TextButton(enabled = !isSaving, onClick = {
            scope.launch {
                isSaving = true
                saveFailed = false
                try {
                    val (saved, mediaType) = withContext(Dispatchers.IO) {
                        saveUriToFile(context, pickedUri)
                    }
                    onAdd(saved.absolutePath, mediaType, selectedVoyage, title.ifBlank { null }, desc.ifBlank { null })
                    onDismiss()
                } catch (exception: Exception) {
                    if (exception is CancellationException) throw exception
                    saveFailed = true
                } finally {
                    isSaving = false
                }
            }
        }) { Text(if (isSaving) "Enregistrement…" else "Publier") }
    }, dismissButton = {
        TextButton(onClick = onDismiss) { Text("Annuler") }
    }, title = { Text("Nouveau souvenir") }, text = {
        Column(Modifier.fillMaxWidth()) {
            AsyncImage(model = pickedUri, contentDescription = null, modifier = Modifier
                .fillMaxWidth()
                .height(180.dp))
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Titre") })
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") })
            Spacer(modifier = Modifier.height(8.dp))
            if (saveFailed) {
                Text("Impossible d’enregistrer ce média. Vérifiez son accès et réessayez.", color = MaterialTheme.colorScheme.error)
            }
            // simple voyage selector
            if (voyages.isNotEmpty()) {
                Column { Text("Choisir un carnet:")
                    voyages.forEach { v ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Button(onClick = { selectedVoyage = v.id }) { Text(v.title) }
                        }
                    }
                }
            }
        }
    })
}

private fun saveUriToFile(context: Context, uri: Uri): Pair<File, String> {
    val mimeType = context.contentResolver.getType(uri)
        ?: throw IOException("Type MIME du média indisponible")
    val fileExtension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
        ?: throw IOException("Extension du média indisponible")
    val (mediaType, suffix) = when {
        mimeType.startsWith("video/") -> "VIDEO" to ".$fileExtension"
        mimeType.startsWith("image/") -> "PHOTO" to ".$fileExtension"
        else -> throw IOException("Type de média non pris en charge : $mimeType")
    }
    val imagesDir = File(context.filesDir, "media")
    if (!imagesDir.exists() && !imagesDir.mkdirs()) {
        throw IOException("Impossible de créer le répertoire média")
    }
    val name = "MEDIA_${System.currentTimeMillis()}$suffix"
    val out = File(imagesDir, name)
    try {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Impossible d’ouvrir le média sélectionné")
        input.use { source ->
            out.outputStream().use { output -> source.copyTo(output) }
        }
        return out to mediaType
    } catch (exception: Exception) {
        out.delete()
        throw exception
    }
}
