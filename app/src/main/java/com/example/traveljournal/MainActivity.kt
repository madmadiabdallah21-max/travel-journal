package com.example.traveljournal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.traveljournal.data.local.TravelJournalDatabase
import com.example.traveljournal.data.repository.TravelRepository
import com.example.traveljournal.ui.theme.TravelJournalTheme
import com.example.traveljournal.navigation.AppNavHost
import com.example.traveljournal.viewmodel.CameraViewModel
import com.example.traveljournal.viewmodel.SouvenirViewModel
import com.example.traveljournal.viewmodel.ThemeViewModel
import com.example.traveljournal.viewmodel.VoyageViewModel

class MainActivity : ComponentActivity() {
    private lateinit var repository: TravelRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = TravelJournalDatabase.getInstance(applicationContext)
        repository = TravelRepository(db.voyageDao(), db.souvenirDao())

        val voyageVmFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return VoyageViewModel(repository) as T
            }
        }

        val souvenirVmFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return SouvenirViewModel(repository) as T
            }
        }

        val cameraVmFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return CameraViewModel() as T
            }
        }

        val themeVmFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ThemeViewModel(applicationContext) as T
            }
        }

        val voyageViewModel: VoyageViewModel by viewModels { voyageVmFactory }
        val souvenirViewModel: SouvenirViewModel by viewModels { souvenirVmFactory }
        val cameraViewModel: CameraViewModel by viewModels { cameraVmFactory }
        val themeViewModel: ThemeViewModel by viewModels { themeVmFactory }

        setContent {
            val isDarkMode = themeViewModel.isDarkMode.collectAsState().value
            
            TravelJournalTheme(darkTheme = isDarkMode) {
                AppNavHost(
                    voyageViewModel = voyageViewModel,
                    souvenirViewModel = souvenirViewModel,
                    cameraViewModel = cameraViewModel,
                    themeViewModel = themeViewModel
                )
            }
        }
    }
}
