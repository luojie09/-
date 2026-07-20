package com.secretbase.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.secretbase.app.ui.theme.SecretBaseTheme

class MainActivity : ComponentActivity() {
    private var initialRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initialRoute = intent.getStringExtra(EXTRA_INITIAL_ROUTE)
        enableEdgeToEdge()
        setContent {
            SecretBaseTheme {
                SecretBaseApp(
                    initialRoute = initialRoute,
                    onInitialRouteConsumed = { initialRoute = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        initialRoute = intent.getStringExtra(EXTRA_INITIAL_ROUTE)
    }

    companion object {
        const val EXTRA_INITIAL_ROUTE = "secret_base_initial_route"
        const val DESTINATION_ANNIVERSARY = "anniversary"
    }
}
