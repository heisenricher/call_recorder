package com.callrecorder.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.callrecorder.app.data.preferences.AppPreferences
import com.callrecorder.app.ui.navigation.AppNavigation
import com.callrecorder.app.ui.theme.CallRecorderTheme
import com.callrecorder.app.util.PermissionUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var appPreferences: AppPreferences

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle runtime permissions outcome
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestAppPermissions()

        setContent {
            CallRecorderTheme {
                val scope = rememberCoroutineScope()
                val isDisclaimerAccepted by appPreferences.isDisclaimerAccepted.collectAsState(initial = true)
                var showDisclaimerDialog by remember { mutableStateOf(!isDisclaimerAccepted) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()

                    if (showDisclaimerDialog) {
                        LegalDisclaimerDialog(
                            onAgree = {
                                scope.launch {
                                    appPreferences.setDisclaimerAccepted(true)
                                    showDisclaimerDialog = false
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun requestAppPermissions() {
        if (!PermissionUtils.hasAllRuntimePermissions(this)) {
            permissionLauncher.launch(PermissionUtils.getRequiredRuntimePermissions())
        }
    }
}

@Composable
private fun LegalDisclaimerDialog(
    onAgree: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        title = {
            Text(text = stringResource(R.string.legal_disclaimer_title))
        },
        text = {
            Text(text = stringResource(R.string.legal_disclaimer_text))
        },
        confirmButton = {
            Button(onClick = onAgree) {
                Text(text = stringResource(R.string.agree_and_continue))
            }
        }
    )
}
