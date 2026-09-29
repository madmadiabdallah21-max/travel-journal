package com.example.traveljournal.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDrawer(
    navController: NavController,
    isDarkMode: Boolean,
    onThemeToggle: () -> Unit,
    drawerState: DrawerState,
    scope: CoroutineScope,
    modifier: Modifier = Modifier
) {
    ModalDrawerSheet(modifier = modifier) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Paramètres",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp)
        )
        Divider()
        
        // Feedback
        NavigationDrawerItem(
            label = { Text("À propos") },
            icon = { Icon(Icons.Default.Info, contentDescription = null) },
            selected = false,
            onClick = {
                scope.launch { drawerState.close() }
                navController.navigate("about")
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        Divider()
        
        // Theme toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                if (isDarkMode) "Thème: Sombre" else "Thème: Clair",
                style = MaterialTheme.typography.bodyMedium
            )
            Switch(
                checked = isDarkMode,
                onCheckedChange = { 
                    onThemeToggle()
                    scope.launch { drawerState.close() }
                }
            )
        }
    }
}
