package com.kernelpanic.baladdns.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kernelpanic.baladdns.R

@Composable
fun PermissionCard(
    title: String,
    description: String,
    buttonText: String? = null,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
        ),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.87f),
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(
                    text = buttonText ?: stringResource(R.string.allow),
                    color = MaterialTheme.colorScheme.onError,
                )
            }
        }
    }
    Spacer(Modifier.height(12.dp))
}
