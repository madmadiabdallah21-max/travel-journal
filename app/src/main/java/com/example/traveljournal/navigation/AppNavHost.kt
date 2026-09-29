package com.example.traveljournal.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.traveljournal.ui.about.AboutScreen
import com.example.traveljournal.ui.camera.CameraScreen
import com.example.traveljournal.ui.capture.CapturePreviewScreen
import com.example.traveljournal.ui.common.AppDrawer
import com.example.traveljournal.ui.souvenir.SouvenirDetailScreen
import com.example.traveljournal.ui.souvenir.SouvenirListScreen
import com.example.traveljournal.ui.voyage.VoyageListScreen
import com.example.traveljournal.viewmodel.CameraViewModel
import com.example.traveljournal.viewmodel.SouvenirViewModel
import com.example.traveljournal.viewmodel.ThemeViewModel
import com.example.traveljournal.viewmodel.VoyageViewModel
import kotlinx.coroutines.launch

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    voyageViewModel: VoyageViewModel,
    souvenirViewModel: SouvenirViewModel,
    cameraViewModel: CameraViewModel,
    themeViewModel: ThemeViewModel
) {
    val drawerState = rememberDrawerState(initialValue = androidx.compose.material3.DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val isDarkMode = themeViewModel.isDarkMode.collectAsState().value
    val isRecording = cameraViewModel.isRecording.collectAsState().value

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !isRecording,
        drawerContent = {
            AppDrawer(
                navController = navController,
                isDarkMode = isDarkMode,
                onThemeToggle = { themeViewModel.toggleDarkMode() },
                drawerState = drawerState,
                scope = scope
            )
        }
    ) {
        NavHost(navController = navController, startDestination = "camera", modifier = modifier) {
            composable("camera") {
                CameraScreen(
                    navController = navController, 
                    cameraViewModel = cameraViewModel,
                    drawerState = drawerState,
                    scope = scope
                )
            }
        composable("capturePreview/{encodedPath}/{mediaType}") { backStack ->
            val encodedPath = backStack.arguments?.getString("encodedPath") ?: ""
            val mediaType = backStack.arguments?.getString("mediaType") ?: "PHOTO"
            CapturePreviewScreen(
                navController = navController,
                encodedPath = encodedPath,
                mediaType = mediaType,
                voyageViewModel = voyageViewModel,
                souvenirViewModel = souvenirViewModel
            )
        }
        composable("voyages") {
            VoyageListScreen(navController = navController, voyageViewModel = voyageViewModel)
        }
        composable("voyage/{voyageId}") { backStack ->
            val voyageIdArg = backStack.arguments?.getString("voyageId")
            val voyageId = voyageIdArg?.toLongOrNull()
            if (voyageId != null) {
                SouvenirListScreen(voyageId = voyageId, navController = navController, viewModel = souvenirViewModel)
            } else {
                // simple fallback UI when id not valid
                androidx.compose.material3.Surface(modifier = Modifier.fillMaxSize()) {
                    androidx.compose.material3.Text(text = "Voyage invalide")
                }
            }
        }
        composable("souvenir/{souvenirId}") { backStack ->
            val souvenirId = backStack.arguments?.getString("souvenirId")?.toLongOrNull() ?: 0L
            SouvenirDetailScreen(navController = navController, souvenirId = souvenirId, souvenirViewModel = souvenirViewModel)
        }
        composable("about") {
            AboutScreen(navController = navController)
        }
    }
    }
}
