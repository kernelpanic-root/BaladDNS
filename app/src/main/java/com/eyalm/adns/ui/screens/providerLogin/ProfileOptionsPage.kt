package com.eyalm.adns.ui.screens.providerLogin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eyalm.adns.data.network.NextDnsProfile
import com.eyalm.adns.ui.components.OnboardingTemplate
import com.eyalm.adns.ui.components.ProfilesList
import com.eyalm.adns.ui.components.StandardBottomBar

@Composable
fun ProfileOptionPage(
    profiles: List<NextDnsProfile>,
    onNextClick: (profile: NextDnsProfile) -> Unit,
    onBackClick: () -> Unit,
    createProfile: (name: String) -> Unit
) {
    
    val openCreateProfileDialog = remember { mutableStateOf(false) }
    var selectedProfile by remember { mutableStateOf(profiles[0]) }

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
        onBackClick = onBackClick,
        bottomBarContent = {
            StandardBottomBar(
                message = "Choose your profile.",
                enabled = true,
                onNextClick = { onNextClick(selectedProfile) }
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Choose your profile.")
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
    var isValid by remember { mutableStateOf(false) }

    AlertDialog(
        icon = {
            Icon(Icons.Default.Add, contentDescription = "Add Icon")
        },
        title = {
            Text(text = "Create Profile")
        },
        text = {
            Text(text = "How would you like to name your profile?")
            TextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Profile Name") },

            )
        },
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmation(name) // TODO: Error handling: validate name, make sure it doesn't already exist
                }
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                }
            ) {
                Text("Dismiss")
            }
        }
    )
}