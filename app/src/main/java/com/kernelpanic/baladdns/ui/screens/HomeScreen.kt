package com.kernelpanic.baladdns.ui.screens
import com.kernelpanic.baladdns.R
import androidx.compose.ui.res.stringResource


import android.content.Intent
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.kernelpanic.baladdns.ui.components.DnsSwitch
import com.kernelpanic.baladdns.ui.components.dialogs.InformationDialog
import com.kernelpanic.baladdns.ui.theme.AdnsTheme
import com.kernelpanic.baladdns.ui.theme.pageTitle

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    isEnabled: Boolean,
    runningTime: String,
    onToggle: () -> Unit,
    controlsEnabled: Boolean = true,
    server: String,
    onEditClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    innerPadding: PaddingValues
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp)
        ) {
            Text(
                text = if (isEnabled) stringResource(R.string.goooodbyenads) else stringResource(R.string.blockerndisabled),
                style = MaterialTheme.typography.pageTitle,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 48.sp,
                lineHeight = 48.sp,
            )

            Spacer(modifier = Modifier.height(32.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(text = stringResource(R.string.private_dns))
                        Text(
                            text = if (isEnabled) stringResource(R.string.running) else stringResource(R.string.not_running),
                            color = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                    IconButton(
                        modifier = Modifier.align(Alignment.Top),
                        onClick = onSettingsClick,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.settings),
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(text = stringResource(R.string.server))
                        Text(text = server)
                    }
                    IconButton(
                        modifier = Modifier.align(Alignment.Top),
                        onClick = onEditClick,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.change_provider),
                        )
                    }
                }

                Column(
                    modifier = Modifier.alpha(if (isEnabled) 1f else 0f)
                ) {
                    Text(text = stringResource(R.string.uptime))
                    Text(text = runningTime)
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            DnsSwitch(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                isEnabled = isEnabled,
                onToggle = onToggle,
                enabled = controlsEnabled,
            )
        }
    }
}

@Composable
fun UpdateDialog(
    version: String,
    onClose: () -> Unit = {},
) {
    val context = LocalContext.current
    InformationDialog(
        title = stringResource(R.string.new_update),
        body = stringResource(
            R.string.version_v_is_available_would_you_like_to_download_it,
            version,
        ),
        confirmLabel = stringResource(R.string.download),
        onConfirm = {
            val url = "https://github.com/eyalm2000/adns/releases"
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            try {
                context.startActivity(intent)
            } catch (e: android.content.ActivityNotFoundException) {
                Log.e("MainActivity", "No browser found to open release URL", e)
            }
            onClose()
        },
        onDismiss = onClose,
    )
}


@Preview(showBackground = true)
@Composable
fun UpdateDialogPreview() {
    AdnsTheme {
        UpdateDialog(
            version = "1.0.0",
            onClose = {}
        )
    }
}

/**
@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    AdnsTheme {
        HomeScreen(
            isEnabled = true,
            runningTime = "00:05:23",
            onToggle = {}
        )
    }
}
        **/
