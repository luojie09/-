package com.secretbase.app

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.secretbase.app.ui.home.HomeScreen
import com.secretbase.app.ui.home.HomeViewModel

@Composable
fun SecretBaseApp(viewModel: HomeViewModel = viewModel()) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    HomeScreen(
        uiState = uiState.value,
        snackbarHostState = snackbarHostState,
        onRetry = viewModel::refresh,
        onMoodCardClick = viewModel::onMoodCardClick,
        onMoodSelected = viewModel::updateMood,
        onDismissMoodPicker = viewModel::dismissMoodPicker,
        onQuickNoteChange = viewModel::updateQuickNote,
        onQuickNoteSubmit = viewModel::submitQuickNote,
        onPlaceholderAction = viewModel::onPlaceholderAction,
    )
}

