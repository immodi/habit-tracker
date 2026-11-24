package com.modi.habittracker.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.modi.habittracker.R
import com.modi.habittracker.ui.AppUiState
import com.modi.habittracker.ui.components.BrandMark
import com.modi.habittracker.ui.components.Notice
import com.modi.habittracker.ui.components.PrimaryButton
import com.modi.habittracker.ui.fetchGoogleIdToken
import com.modi.habittracker.ui.theme.AppColors
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    state: AppUiState,
    onSignIn: (String, String) -> Unit,
    onSignUp: (String, String) -> Unit,
    onSignInWithGoogle: (String) -> Unit,
    onClearError: () -> Unit,
) {
    var registering by rememberSaveable { mutableStateOf(false) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    var googleBusy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val busy = state.authBusy || googleBusy
    val error = localError ?: state.error

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 22.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrandMark(size = 46.dp)
            Spacer(Modifier.width(13.dp))
            Column {
                Text("Habits", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Small days, kept.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.Muted,
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Surface(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = AppColors.Surface,
            border = BorderStroke(1.dp, AppColors.Line),
        ) {
            Column(Modifier.padding(24.dp)) {
                Text(
                    if (registering) "Create your account" else "Welcome back",
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(Modifier.height(18.dp))

                AuthField(
                    label = "Email",
                    value = email,
                    onValueChange = { email = it },
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                )
                Spacer(Modifier.height(12.dp))
                AuthField(
                    label = "Password",
                    value = password,
                    onValueChange = { password = it },
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                    visualTransformation = PasswordVisualTransformation(),
                    placeholder = if (registering) "At least 6 characters" else null,
                )

                if (error != null) {
                    Spacer(Modifier.height(12.dp))
                    Notice(error)
                }

                Spacer(Modifier.height(18.dp))
                PrimaryButton(
                    text = if (registering) "Create account" else "Sign in",
                    onClick = {
                        localError = null
                        onClearError()
                        if (registering) onSignUp(email, password) else onSignIn(email, password)
                    },
                    enabled = !busy && email.isNotBlank() && password.length >= 6,
                    modifier = Modifier.fillMaxWidth(),
                )

                OrDivider()

                Surface(
                    onClick = {
                        scope.launch {
                            googleBusy = true
                            localError = null
                            onClearError()
                            try {
                                onSignInWithGoogle(fetchGoogleIdToken(context))
                            } catch (googleError: Exception) {
                                localError = googleError.message ?: "Google sign-in did not finish."
                            } finally {
                                googleBusy = false
                            }
                        }
                    },
                    enabled = !busy,
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Row(
                        Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (googleBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF101418),
                            )
                        } else {
                            Image(
                                painterResource(R.drawable.ic_google),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Continue with Google",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(0xFF101418),
                            )
                        }
                    }
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (registering) "Already have an account?" else "New here?",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.Muted,
                    )
                    TextButton(
                        onClick = {
                            registering = !registering
                            localError = null
                            onClearError()
                        },
                    ) {
                        Text(
                            if (registering) "Sign in" else "Create an account",
                            style = MaterialTheme.typography.labelMedium,
                            color = AppColors.Accent,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        Text(
            "Only you can read your habits and check-ins.",
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.MutedDim,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AuthField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    placeholder: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it, color = AppColors.MutedDim) } },
        singleLine = true,
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction,
        ),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AppColors.Accent.copy(alpha = 0.55f),
            unfocusedBorderColor = AppColors.LineStrong,
            focusedContainerColor = AppColors.BackgroundSoft,
            unfocusedContainerColor = AppColors.BackgroundSoft,
            cursorColor = AppColors.Accent,
            focusedLabelColor = AppColors.Accent,
            unfocusedLabelColor = AppColors.Muted,
            focusedTextColor = AppColors.Text,
            unfocusedTextColor = AppColors.Text,
        ),
    )
}

@Composable
private fun OrDivider() {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HorizontalDivider(Modifier.weight(1f), color = AppColors.Line)
        Text(
            "or",
            Modifier.padding(horizontal = 10.dp),
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.MutedDim,
        )
        HorizontalDivider(Modifier.weight(1f), color = AppColors.Line)
    }
}
