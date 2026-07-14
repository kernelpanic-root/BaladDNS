package com.eyalm.adns.ui.screens.providerLogin
import com.eyalm.adns.R
import androidx.compose.ui.res.stringResource


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eyalm.adns.data.nextdns.api.NextDnsProfile
import com.eyalm.adns.ui.components.OnboardingTemplate
import com.eyalm.adns.ui.components.ProfilesList
import com.eyalm.adns.ui.components.dialogs.FormDialog
import com.eyalm.adns.ui.components.StandardBottomBar
import com.eyalm.adns.ui.theme.pageTitle

@Composable
fun ProfileOptionPage(
    profiles: List<NextDnsProfile>,
    onNextClick: (profile: NextDnsProfile) -> Unit,
    createProfile: (name: String) -> Unit
) {
    
    val openCreateProfileDialog = remember { mutableStateOf(false) }
    var selectedProfile by remember(profiles) { mutableStateOf(profiles.firstOrNull()) }

    when {
        openCreateProfileDialog.value -> {
            CreateProfileDialog(
                onDismissRequest = { openCreateProfileDialog.value = false },
                onConfirmation = { name ->
                    openCreateProfileDialog.value = false
                    createProfile(name)
                }
            )

        }
    }

    OnboardingTemplate(
        hideTopBar = true,
        bottomBarContent = {
            StandardBottomBar(
                message = stringResource(R.string.choose_your_profile),
                enabled = selectedProfile != null,
                onNextClick = { selectedProfile?.let { onNextClick(it) } }
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(start = 16.dp, end = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Spacer(Modifier.height(100.dp))
                Text(stringResource(R.string.profile), style = MaterialTheme.typography.pageTitle)
                Text(stringResource(R.string.choose_your_profile))
                Spacer(modifier = Modifier.height(8.dp))
                ProfilesList(
                    profiles = profiles,
                    selectedProfile = selectedProfile,
                    onProfileSelected = { selectedProfile = it },
                    onCreateProfileClick = { openCreateProfileDialog.value = true }
                )

            }
        }
    )
}


@Composable
fun CreateProfileDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: (name: String) -> Unit,
) {

    var name by remember { mutableStateOf("") }
    FormDialog(
        title = stringResource(R.string.create_profile),
        confirmLabel = stringResource(R.string.create),
        confirmEnabled = name.trim().isNotEmpty(),
        onConfirm = { if (name.trim().isNotEmpty()) onConfirmation(name) },
        onDismiss = onDismissRequest,
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.profile_name)) },
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
        )
    }
}
