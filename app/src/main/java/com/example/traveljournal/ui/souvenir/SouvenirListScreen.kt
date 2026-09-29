package com.example.traveljournal.ui.souvenir

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.material.icons.Icons
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.NavController
import android.net.Uri
import java.util.Date
import com.example.traveljournal.data.model.SouvenirEntity
import com.example.traveljournal.viewmodel.SouvenirViewModel
import com.example.traveljournal.ui.voyage.NewSouvenirDialog
import com.example.traveljournal.ui.common.AppTopBar
import com.example.traveljournal.util.MediaSaver
import java.io.File



@Composable
fun SouvenirListScreen(
    voyageId: Long,
    navController: NavController,
    viewModel: SouvenirViewModel
) {
    val souvenirs: List<SouvenirEntity> by viewModel.getSouvenirs(voyageId).collectAsState(initial = emptyList())
    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var selectedSouvenirForEdit by remember { mutableStateOf<SouvenirEntity?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { pickedUri = it; showDialog = true }
    }
    Scaffold(
        topBar = {
            AppTopBar(title = "Souvenirs", navController = navController)
        },
        floatingActionButton = {
            if (souvenirs.isNotEmpty()) {
                FloatingActionButton(onClick = { launcher.launch(arrayOf("image/*", "video/*")) }) { Icon(Icons.Default.Add, contentDescription = "Ajouter un souvenir") }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (souvenirs.isEmpty()) {
                Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Aucun souvenir", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { launcher.launch(arrayOf("image/*", "video/*")) }) { Text("Ajouter un souvenir") }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(souvenirs) { s ->
                        SouvenirItem(s, 
                            onSelect = { navController.navigate("souvenir/${s.id}") },
                            onEdit = { selectedSouvenirForEdit = s },
                            onDelete = { viewModel.deleteSouvenir(s) }
                        )
                    }
                }
            }
        }
    }

    if (showDialog && pickedUri != null) {
        NewSouvenirDialog(pickedUri!!, voyages = emptyList(), currentVoyageId = voyageId, onDismiss = { showDialog = false }) { mediaPath, mediaType, voyage, title, description ->
            viewModel.addSouvenir(voyage, mediaPath, mediaType, title, description)
        }
    }

    selectedSouvenirForEdit?.let { s ->
        var editTitle by remember { mutableStateOf(s.title ?: "") }
        var editDesc by remember { mutableStateOf(s.description ?: "") }
        AlertDialog(
            onDismissRequest = { selectedSouvenirForEdit = null },
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
                    viewModel.updateSouvenir(s.copy(title = editTitle.ifBlank { null }, description = editDesc.ifBlank { null }))
                    selectedSouvenirForEdit = null
                }) { Text("Enregistrer") }
            },
            dismissButton = {
                TextButton(onClick = { selectedSouvenirForEdit = null }) { Text("Annuler") }
            }
        )
    }
}

@Composable
fun SouvenirItem(s: SouvenirEntity, onSelect: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val context = LocalContext.current
    Card(modifier = Modifier
        .padding(8.dp)
        .fillMaxWidth()
        .height(100.dp)
        .clickable { onSelect() }) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()) {
            val file = File(s.mediaPath)
            val uri = MediaSaver.getContentUri(context, file)
            AsyncImage(model = uri, contentDescription = s.description, modifier = Modifier.size(80.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(s.title ?: "", style = MaterialTheme.typography.titleMedium)
                Text(Date(s.createdAt).toString(), style = MaterialTheme.typography.bodySmall)
            }
            var showMenu by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu")                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text("Modifier") }, onClick = {
                        showMenu = false
                        onEdit()
                    })
                    DropdownMenuItem(text = { Text("Supprimer") }, onClick = {
                        showMenu = false
                        onDelete()
                    })
                }
            }
        }
    }
}
