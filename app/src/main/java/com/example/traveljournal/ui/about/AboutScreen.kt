package com.example.traveljournal.ui.about

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.traveljournal.ui.common.AppTopBar

@Composable
fun AboutScreen(navController: NavController) {
    Scaffold(
        topBar = {
            AppTopBar(title = "À propos", navController = navController)
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Text("Carnet de voyage", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Version: 1.0.0", color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Une application Android pour capturer, organiser et partager des souvenirs de voyage.",
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}
