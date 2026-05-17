package com.eyalm.adns.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.eyalm.adns.data.network.NextDnsProfile

@Composable
fun ProfilesList(
    profiles: List<NextDnsProfile>,
    selectedProfile: NextDnsProfile?,
    onProfileSelected: (NextDnsProfile) -> Unit,
    onCreateProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        profiles.forEach { profile ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = profile == selectedProfile,
                    onClick = { onProfileSelected(profile) }
                )
                Text(
                    text = profile.name
                )
            }
        }

        TextButton(
            onClick = onCreateProfileClick
        ) {
            Text("Create New Profile")
        }
    }
}