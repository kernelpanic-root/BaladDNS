package com.eyalm.adns.ui.screens.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eyalm.adns.R
import com.eyalm.adns.data.Locales
import com.eyalm.adns.data.nextdns.access.AccessEntry
import com.eyalm.adns.data.nextdns.access.AccessError
import com.eyalm.adns.data.nextdns.access.AccessField
import com.eyalm.adns.data.nextdns.access.AccessRole
import com.eyalm.adns.ui.components.ExpressiveListItem
import com.eyalm.adns.ui.components.dialogs.BaseDialog
import com.eyalm.adns.viewmodel.nextdns.AccessUiState
import com.eyalm.adns.viewmodel.nextdns.AccessViewModel

@Composable
fun AccessSection(
    profileId: String,
    refreshRevision: Long = 0,
) {
    val viewModel: AccessViewModel = viewModel(key = "access-$profileId")
    val state by viewModel.state.collectAsState()

    LaunchedEffect(profileId, refreshRevision) {
        viewModel.load(profileId, canManage = true)
    }

    ExpressiveListItem(
        topPadding = 4.dp,
        title = Locales.getString("settings", "access", "name"),
        description = Locales.getString("settings", "access", "description"),
        isFirst = true,
        isLast = true,
        trailingCenter = true,
        interactiveItem = { _,_ ->
            IconButton(onClick = viewModel::openInvite) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = Locales.getString("global", "add"))
            }
        }
    ) {
        if (!state.initialLoadComplete) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            if (!state.inviteDialogOpen && state.roleTarget == null && state.deleting == null) {
                state.errorMessage?.let { error ->
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.height(12.dp))

            state.items.forEachIndexed { index, entry ->
                AccessRow(
                    entry = entry,
                    isFirst = index == 0,
                    isLast = index == state.items.lastIndex,
                    onRoleClick = { viewModel.requestRoleChange(entry) },
                    onDelete = { viewModel.requestDelete(entry) },
                )
            }

            if (state.items.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
            }
        }
    }

    if (state.inviteDialogOpen) {
        InviteAccessDialog(
            state = state,
            onEmailChange = viewModel::updateEmail,
            onRoleChange = viewModel::updateInviteRole,
            onSubmit = viewModel::submitInvite,
            onDismiss = viewModel::dismissInvite,
        )
    }

    state.roleTarget?.let { target ->
        AccessRoleDialog(
            email = target.email,
            selectedRole = state.selectedRole,
            submitting = state.submitting,
            errorMessage = state.errorMessage,
            onRoleChange = viewModel::updateSelectedRole,
            onSubmit = viewModel::submitRoleChange,
            onDismiss = viewModel::dismissRoleChange,
        )
    }

    state.deleting?.let { entry ->
        BaseDialog(
            title = Locales.getString("global","remove"),
            body = stringResource(R.string.remove, entry.email),
            confirmLabel = Locales.getString("global", "remove"),
            destructive = true,
            submitting = state.submitting,
            errorMessage = state.errorMessage,
            onConfirm = viewModel::confirmDelete,
            onDismiss = viewModel::dismissDelete,
        )
    }

}

@Composable
private fun InviteAccessDialog(
    state: AccessUiState,
    onEmailChange: (String) -> Unit,
    onRoleChange: (AccessRole) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    BaseDialog(
        title = Locales.getString("global", "invite"),
        confirmLabel = Locales.getString("global", "invite"),
        destructive = false,
        submitting = state.submitting,
        errorMessage = state.errorMessage,
        onConfirm = onSubmit,
        onDismiss = onDismiss,
    ) {
        OutlinedTextField(
            value = state.email,
            onValueChange = onEmailChange,
            label = { Text(Locales.getString("settings", "access", "form", "email", "label")) },
            isError = AccessField.Email in state.fieldErrors,
            enabled = !state.submitting,
            supportingText = {
                state.fieldErrors[AccessField.Email]?.let { error ->
                    Text(accessErrorText(error))
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            )
        AccessRoleChoices(
            selectedRole = state.inviteRole,
            enabled = !state.submitting,
            onRoleChange = onRoleChange,
        )
    }
}

@Composable
private fun AccessRoleDialog(
    email: String,
    selectedRole: AccessRole,
    submitting: Boolean,
    errorMessage: String?,
    onRoleChange: (AccessRole) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    BaseDialog(
        title = Locales.getString("settings", "access", "form", "role", "label"),
        confirmLabel = Locales.getString("global", "save"),
        destructive = false,
        submitting = submitting,
        errorMessage = errorMessage,
        onConfirm = onSubmit,
        onDismiss = onDismiss,

    ) {
        Text(email, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(20.dp))
        AccessRoleChoices(
            selectedRole = selectedRole,
            enabled = !submitting,
            onRoleChange = onRoleChange,
        )
    }
}

@Composable
private fun AccessRoleChoices(
    selectedRole: AccessRole,
    enabled: Boolean,
    onRoleChange: (AccessRole) -> Unit,
) {
    AccessRole.entries.forEach { role ->
        ExpressiveListItem(
            title = role.label(),
            isSelected = role == selectedRole,
            onClick = { onRoleChange(role) },
            isLast = role == AccessRole.entries.last(),
            isFirst = role == AccessRole.entries.first(),
            interactiveItem = { _,_ ->
                RadioButton(
                    selected = role == selectedRole,
                    enabled = enabled,
                    onClick = { onRoleChange(role) }
                )
            },
            overrideCorners = true
        )
    }
}

@Composable
private fun AccessRow(
    entry: AccessEntry,
    isFirst: Boolean,
    isLast: Boolean,
    onRoleClick: () -> Unit,
    onDelete: () -> Unit,
) {
    ExpressiveListItem(
        title = entry.email,
        isSelected = true,
        description = buildString {
            append(entry.role.label())
            if (entry.pending) {
                append(" · ")
                append(Locales.getString("settings", "access", "pending"))
            }
        },
        onClick = onRoleClick,
        interactiveItem = { _, _ ->
            Row {
                TextButton(onClick = onRoleClick) {
                    Text(entry.role.label())
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = Locales.getString("global", "remove"))
                }
            }
        },
        isFirst = isFirst,
        isLast = isLast,
        overrideCorners = true
    )
    Spacer(Modifier.height(4.dp))
}

private fun AccessRole.label(): String =
    Locales.getString("settings", "access", "roles", name.lowercase())

private fun accessErrorText(error: AccessError): String = when (error) {
    AccessError.Required ->
        Locales.getString("settings", "access", "form", "email", "errors", "required")
    AccessError.Invalid ->
        Locales.getString("settings", "access", "form", "email", "errors", "invalid")
    AccessError.Duplicate ->
        Locales.getString("settings", "access", "form", "email", "errors", "duplicate")
}
