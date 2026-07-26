package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.ChatDetailsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.theme.ChatMindTheme
import com.example.ui.viewmodels.ChatDetailsViewModelFactory
import com.example.ui.viewmodels.MainViewModel

class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingIntent(intent)

        setContent {
            ChatMindTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ChatMindApp(mainViewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action

        if (Intent.ACTION_SEND == action || Intent.ACTION_VIEW == action) {
            val streamUri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java) ?: intent.data
            } else {
                @Suppress("DEPRECATION")
                (intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri) ?: intent.data
            }

            if (streamUri != null) {
                val fileName = getFileNameFromUri(streamUri)
                mainViewModel.uploadChat(streamUri, fileName)
            } else {
                val extraText = intent.getStringExtra(Intent.EXTRA_TEXT)
                if (!extraText.isNullOrBlank()) {
                    val title = intent.getStringExtra(Intent.EXTRA_SUBJECT) ?: "Shared Chat"
                    mainViewModel.uploadChatFromText(extraText, title)
                }
            }
        } else if (Intent.ACTION_SEND_MULTIPLE == action) {
            val uris = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
            }
            uris?.firstOrNull()?.let { uri ->
                val fileName = getFileNameFromUri(uri)
                mainViewModel.uploadChat(uri, fileName)
            }
        }
    }

    private fun getFileNameFromUri(uri: Uri): String {
        var name = "WhatsApp Export"
        try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex != -1) {
                    name = cursor.getString(nameIndex)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return name
    }
}

@Composable
fun ChatMindApp(mainViewModel: MainViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "dashboard") {
        composable("dashboard") {
            DashboardScreen(
                viewModel = mainViewModel,
                onNavigateToChat = { chatId ->
                    navController.navigate("chat_details/$chatId")
                }
            )
        }
        composable(
            "chat_details/{chatId}",
            arguments = listOf(navArgument("chatId") { type = NavType.LongType })
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getLong("chatId") ?: return@composable
            val context = androidx.compose.ui.platform.LocalContext.current
            val application = context.applicationContext as android.app.Application
            val chatDetailsViewModel: com.example.ui.viewmodels.ChatDetailsViewModel = viewModel(
                factory = ChatDetailsViewModelFactory(chatId, mainViewModel.repository, application)
            )
            ChatDetailsScreen(
                viewModel = chatDetailsViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
