package com.example.traveljournal.ui.voyage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.traveljournal.data.model.VoyageEntity
import com.example.traveljournal.viewmodel.VoyageViewModel
import com.example.traveljournal.ui.common.AppTopBar

@Composable
fun VoyageListScreen(navController: NavController, voyageViewModel: VoyageViewModel) {
    val voyages by voyageViewModel.voyages.collectAsState()
    var showNewVoyageDialog by remember { mutableStateOf(false) }
    var selectedVoyageForEdit by remember { mutableStateOf<VoyageEntity?>(null) }

    Scaffold(
        topBar = {
            AppTopBar(title = "Voyages", navController = navController)
        },
        floatingActionButton = {
            if (voyages.isNotEmpty()) {
                FloatingActionButton(onClick = { showNewVoyageDialog = true }) { Icon(Icons.Default.Add, contentDescription = "Créer un voyage") }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (voyages.isEmpty()) {
                // Empty state: single CTA button center
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "Aucun voyage", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { showNewVoyageDialog = true }) {
                        Text("Créer un voyage")
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(voyages) { v ->
                        VoyageItem(v, 
                            onSelect = { navController.navigate("voyage/${v.id}") },
                            onEdit = { selectedVoyageForEdit = v },
                            onDelete = { voyageViewModel.deleteVoyage(v) }
                        )
                    }
                }
            }
        }
    }

    if (showNewVoyageDialog) {
        CreateVoyageDialog(voyage = null, onDismiss = { showNewVoyageDialog = false }) { title, desc, thumbPath ->
            voyageViewModel.addVoyage(title = title, description = desc, thumbnailPath = thumbPath)
            showNewVoyageDialog = false
        }
    }

    selectedVoyageForEdit?.let { v ->
        CreateVoyageDialog(voyage = v, onDismiss = { selectedVoyageForEdit = null }) { title, desc, thumbPath ->
            voyageViewModel.updateVoyage(v.copy(title = title, description = desc, thumbnailPath = thumbPath))
            selectedVoyageForEdit = null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoyageItem(v: VoyageEntity, onSelect: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier
        .padding(8.dp)
        .fillMaxWidth()
        .height(80.dp), onClick = onSelect) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()) {
            if (v.thumbnailPath != null) {
                AsyncImage(model = v.thumbnailPath, contentDescription = v.title, modifier = Modifier.size(64.dp))
            } else {
                Box(modifier = Modifier
                    .size(64.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) { 
                    Text("📷", style = MaterialTheme.typography.titleLarge) 
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) { 
                Text(v.title, style = MaterialTheme.typography.titleMedium)
                Text(v.description ?: "", style = MaterialTheme.typography.bodySmall) 
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
