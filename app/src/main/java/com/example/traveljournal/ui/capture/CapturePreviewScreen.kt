package com.example.traveljournal.ui.capture

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.traveljournal.data.model.VoyageEntity
import com.example.traveljournal.ui.common.AppTopBar
import com.example.traveljournal.ui.common.VideoPlayer
import com.example.traveljournal.ui.common.ZoomableImage
import com.example.traveljournal.util.MediaSaver
import com.example.traveljournal.viewmodel.SouvenirViewModel
import com.example.traveljournal.viewmodel.VoyageViewModel
import java.io.File
import java.net.URLDecoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CapturePreviewScreen(
    navController: NavController,
    encodedPath: String,
    mediaType: String,
    voyageViewModel: VoyageViewModel,
    souvenirViewModel: SouvenirViewModel
) {
    val context = LocalContext.current
    val voyages by voyageViewModel.voyages.collectAsState()
    
    // Decode the path
    val mediaPath = URLDecoder.decode(encodedPath, "UTF-8")
    val file = File(mediaPath)
    val uri = MediaSaver.getContentUri(context, file)
    
    var selectedVoyageId by remember { mutableStateOf<Long?>(null) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AppTopBar(title = "Aperçu", navController = navController)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Media Preview
            if (mediaType == "VIDEO") {
                VideoPlayer(uri = uri)
            } else {
                ZoomableImage(uri = uri)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Voyage Selector
            var expandedVoyage by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(
                    onClick = { expandedVoyage = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        text = voyages.find { it.id == selectedVoyageId }?.title ?: "Sélectionner un voyage",
                        modifier = Modifier.weight(1f)
                    )
                }
                DropdownMenu(
                    expanded = expandedVoyage,
                    onDismissRequest = { expandedVoyage = false }
                ) {
                    voyages.forEach { voyage ->
                        DropdownMenuItem(
                            text = { Text(voyage.title) },
                            onClick = {
                                selectedVoyageId = voyage.id
                                expandedVoyage = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Title Field
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Titre") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Description Field
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 4
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Error message
            if (showError) {
                Text(
                    "Veuillez sélectionner un voyage",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Annuler")
                }

                Button(
                    onClick = {
                        val voyageId = selectedVoyageId
                        if (voyageId != null) {
                            souvenirViewModel.addSouvenir(
                                voyageId = voyageId,
                                mediaPath = mediaPath,
                                mediaType = mediaType,
                                title = title.ifBlank { null },
                                description = description.ifBlank { null }
                            )
                            navController.navigate("voyage/$voyageId") {
                                popUpTo("camera") { inclusive = false }
                            }
                        } else {
                            showError = true
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Ajouter")
                }
            }
        }
    }
}
