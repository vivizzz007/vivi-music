package com.music.vivi.desktop

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Desktop login: paste the `Cookie` request header from a logged-in
 * music.youtube.com browser session. [onLoggedIn] is invoked on success.
 */
@Composable
fun LoginScreen(language: String, onBack: () -> Unit, onLoggedIn: () -> Unit) {
    var cookie by remember { mutableStateOf("") }
    var dataSyncId by remember { mutableStateOf("") }
    var visitorData by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
    ) {
        BackButton(language, onBack)
        Text(Localization.get(language, "login"), style = MaterialTheme.typography.headlineMedium)
        Text(
            Localization.get(language, "login_instructions"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        OutlinedTextField(
            value = cookie,
            onValueChange = { cookie = it },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(160.dp),
            label = { Text(Localization.get(language, "cookie_label")) },
        )

        // Optional manual fallbacks, used only when auto-detection fails.
        Text(
            Localization.get(language, "advanced_login_hint"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp),
        )
        OutlinedTextField(
            value = dataSyncId,
            onValueChange = { dataSyncId = it },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            label = { Text(Localization.get(language, "data_sync_id_label")) },
            singleLine = true,
        )
        OutlinedTextField(
            value = visitorData,
            onValueChange = { visitorData = it },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            label = { Text(Localization.get(language, "visitor_data_label")) },
            singleLine = true,
        )

        Button(
            onClick = {
                scope.launch {
                    loading = true
                    error = null
                    status = null
                    try {
                        val account = withContext(Dispatchers.IO) {
                            LoginManager.login(
                                cookie = cookie,
                                dataSyncIdOverride = dataSyncId.ifBlank { null },
                                visitorDataOverride = visitorData.ifBlank { null },
                            )
                        }
                        status = "${Localization.get(language, "logged_in_as")}: ${account.name}"
                        onLoggedIn()
                    } catch (e: Exception) {
                        error = e.message ?: (e::class.simpleName ?: "error")
                    } finally {
                        loading = false
                    }
                }
            },
            enabled = cookie.isNotBlank() && !loading,
            modifier = Modifier.padding(top = 16.dp),
        ) {
            Text(if (loading) Localization.get(language, "logging_in") else Localization.get(language, "login"))
        }

        status?.let {
            Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
        }
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }
    }
}
