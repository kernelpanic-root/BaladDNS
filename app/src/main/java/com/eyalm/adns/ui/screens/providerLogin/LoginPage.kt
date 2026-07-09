package com.eyalm.adns.ui.screens.providerLogin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eyalm.adns.R
import com.eyalm.adns.data.Locales
import com.eyalm.adns.data.nextdns.auth.NextDnsLoginFailure
import com.eyalm.adns.data.nextdns.auth.NextDnsLoginField
import com.eyalm.adns.data.nextdns.auth.NextDnsLoginMode
import com.eyalm.adns.ui.components.OnboardingTemplate
import com.eyalm.adns.ui.components.StandardBottomBar
import com.eyalm.adns.ui.theme.pageTitle
import com.eyalm.adns.viewmodel.ProviderLoginViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Login(
    onBackClick: () -> Unit,
    viewModel: ProviderLoginViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    var passwordVisible by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    val canSubmit = when (state.mode) {
        NextDnsLoginMode.Password ->
            state.email.isNotBlank() &&
                state.password.isNotBlank() &&
                (!state.requiresTwoFactor || state.code.length == 6)

        NextDnsLoginMode.ApiKey -> state.apiKey.isNotBlank()
    }

    OnboardingTemplate(
        onBackClick = onBackClick,
        bottomBarContent = {
            if (state.submitting) {
                LinearWavyProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            } else {
                StandardBottomBar(
                    message = state.generalError?.let { loginFailureMessage(it, null) }.orEmpty(),
                    buttonText = stringResource(R.string.login),
                    enabled = canSubmit,
                    onNextClick = viewModel::submit,
                )
            }
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.login),
                    style = MaterialTheme.typography.pageTitle,
                    modifier = Modifier.padding(top = 16.dp),
                )

                if (state.mode == NextDnsLoginMode.Password) {
                    Text(
                        stringResource(
                            R.string.adns_uses_your_credentials_only_to_automatically_create_your_nextdns_api_key_which_is_then_stored
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        lineHeight = 20.sp,
                    )

                    LoginTextField(
                        value = state.email,
                        onValueChange = viewModel::onEmailChanged,
                        label = stringResource(R.string.email),
                        icon = Icons.Default.Email,
                        field = NextDnsLoginField.Email,
                        failure = state.fieldErrors[NextDnsLoginField.Email],
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next,
                        ),
                    )

                    LoginTextField(
                        value = state.password,
                        onValueChange = viewModel::onPasswordChanged,
                        label = stringResource(R.string.password),
                        icon = Icons.Default.Lock,
                        field = NextDnsLoginField.Password,
                        failure = state.fieldErrors[NextDnsLoginField.Password],
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = if (state.requiresTwoFactor) ImeAction.Next else ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { if (canSubmit) viewModel.submit() }
                        ),
                        visualTransformation = if (passwordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) {
                                        Icons.Default.Visibility
                                    } else {
                                        Icons.Default.VisibilityOff
                                    },
                                    contentDescription = stringResource(
                                        if (passwordVisible) {
                                            R.string.hide_password
                                        } else {
                                            R.string.show_password
                                        }
                                    ),
                                )
                            }
                        },
                    )

                    if (state.requiresTwoFactor) {
                        LoginTextField(
                            value = state.code,
                            onValueChange = viewModel::onCodeChanged,
                            label = stringResource(R.string.s_2fa_code),
                            icon = Icons.Default.Shield,
                            field = NextDnsLoginField.Code,
                            failure = state.fieldErrors[NextDnsLoginField.Code],
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.NumberPassword,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { if (canSubmit) viewModel.submit() }
                            ),
                        )
                    }

                    TextButton(
                        onClick = { viewModel.setMode(NextDnsLoginMode.ApiKey) },
                    ) {
                        Text(
                            stringResource(R.string.log_in_with_an_api_key_instead),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(
                                R.string.adns_uses_your_nextdns_api_key_to_fetch_and_configure_your_profiles_your_api_key_is_stored_securel
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            lineHeight = 20.sp,
                        )
                        Row {
                            Text(
                                text = stringResource(R.string.you_can_create_one_at_the_nextdns_website),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                            )
                            Text(
                                text = stringResource(R.string.mynextdnsioaccount),
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                textDecoration = TextDecoration.Underline,
                                modifier = Modifier.clickable {
                                    uriHandler.openUri("https://my.nextdns.io/account")
                                },
                            )
                        }
                    }

                    LoginTextField(
                        value = state.apiKey,
                        onValueChange = viewModel::onApiKeyChanged,
                        label = stringResource(R.string.api_key),
                        icon = Icons.Default.VpnKey,
                        field = NextDnsLoginField.ApiKey,
                        failure = state.fieldErrors[NextDnsLoginField.ApiKey],
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { if (canSubmit) viewModel.submit() }
                        ),
                        visualTransformation = PasswordVisualTransformation(),
                    )

                    TextButton(
                        onClick = { viewModel.setMode(NextDnsLoginMode.Password) },
                    ) {
                        Text(
                            stringResource(R.string.log_in_with_credentials_instead),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun LoginTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    field: NextDnsLoginField,
    failure: NextDnsLoginFailure?,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
        trailingIcon = trailingIcon,
        visualTransformation = visualTransformation,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        singleLine = true,
        isError = failure != null,
        supportingText = failure?.let {
            { Text(loginFailureMessage(it, field)) }
        },
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
    )
}

@Composable
private fun loginFailureMessage(
    failure: NextDnsLoginFailure,
    field: NextDnsLoginField?,
): String = when (failure) {
    NextDnsLoginFailure.Required -> when (field) {
        NextDnsLoginField.Email -> Locales.getString("account", "errors", "email", "required")
        NextDnsLoginField.Password -> Locales.getString("account", "errors", "password", "required")
        NextDnsLoginField.Code -> Locales.getString("account", "errors", "code", "required")
        NextDnsLoginField.ApiKey, null -> stringResource(R.string.api_key_required)
    }

    NextDnsLoginFailure.InvalidEmail ->
        Locales.getString("account", "errors", "email", "invalid")

    NextDnsLoginFailure.InvalidCredentials ->
        Locales.getString("account", "errors", "password", "invalidCombination")

    NextDnsLoginFailure.InvalidTwoFactorCode ->
        Locales.getString("account", "errors", "code", "incorrect")

    NextDnsLoginFailure.InvalidTwoFactorFormat ->
        Locales.getString("account", "errors", "code", "invalid")

    NextDnsLoginFailure.InvalidApiKey -> stringResource(R.string.invalid_api_key)
    NextDnsLoginFailure.Offline -> stringResource(R.string.network_error_please_try_again)
    NextDnsLoginFailure.ServerUnavailable -> stringResource(R.string.server_unavailable_try_again)
    NextDnsLoginFailure.Unknown -> stringResource(R.string.login_failed_try_again)
}
