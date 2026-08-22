package com.vinaynalavade.expensetracker.presentation.security

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vinaynalavade.expensetracker.R
import com.vinaynalavade.expensetracker.core.security.BiometricAuthHelper
import com.vinaynalavade.expensetracker.presentation.components.AppTopBar

private enum class SetupStep {
    CREATE_PIN,
    CONFIRM_PIN
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLockSetupScreen(
    viewModel: AppLockViewModel,
    onNavigateBack: () -> Unit,
    onSetupComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isBiometricAvailable = rememberBiometricAvailability(context)

    var currentStep by remember { mutableStateOf(SetupStep.CREATE_PIN) }
    var firstPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showBiometricPromptDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            if (event is AppLockUiEvent.SetupSuccess) {
                onSetupComplete()
            }
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.setup_pin_title),
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Title & Instructions
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = when (currentStep) {
                        SetupStep.CREATE_PIN -> stringResource(R.string.setup_pin_title)
                        SetupStep.CONFIRM_PIN -> stringResource(R.string.setup_pin_confirm_title)
                    },
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = when (currentStep) {
                        SetupStep.CREATE_PIN -> stringResource(R.string.setup_pin_subtitle)
                        SetupStep.CONFIRM_PIN -> stringResource(R.string.setup_pin_confirm_subtitle)
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    textAlign = TextAlign.Center
                )
            }

            // Middle: PIN Dots & Error
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val currentPinLength = when (currentStep) {
                    SetupStep.CREATE_PIN -> firstPin.length
                    SetupStep.CONFIRM_PIN -> confirmPin.length
                }

                PinDotsIndicator(
                    pinLength = 4,
                    filledCount = currentPinLength,
                    isError = errorMessage != null
                )

                Spacer(modifier = Modifier.height(16.dp))

                AnimatedVisibility(
                    visible = errorMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        text = errorMessage.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Bottom: Keypad
            NumericKeypad(
                onDigit = { digit ->
                    errorMessage = null
                    when (currentStep) {
                        SetupStep.CREATE_PIN -> {
                            if (firstPin.length < 4) {
                                val next = firstPin + digit
                                firstPin = next
                                if (next.length == 4) {
                                    currentStep = SetupStep.CONFIRM_PIN
                                }
                            }
                        }
                        SetupStep.CONFIRM_PIN -> {
                            if (confirmPin.length < 4) {
                                val next = confirmPin + digit
                                confirmPin = next
                                if (next.length == 4) {
                                    if (next == firstPin) {
                                        if (isBiometricAvailable) {
                                            showBiometricPromptDialog = true
                                        } else {
                                            viewModel.completeSetup(firstPin, enableBiometric = false)
                                        }
                                    } else {
                                        errorMessage = context.getString(R.string.setup_pin_mismatch)
                                        firstPin = ""
                                        confirmPin = ""
                                        currentStep = SetupStep.CREATE_PIN
                                    }
                                }
                            }
                        }
                    }
                },
                onDelete = {
                    errorMessage = null
                    when (currentStep) {
                        SetupStep.CREATE_PIN -> {
                            if (firstPin.isNotEmpty()) firstPin = firstPin.dropLast(1)
                        }
                        SetupStep.CONFIRM_PIN -> {
                            if (confirmPin.isNotEmpty()) {
                                confirmPin = confirmPin.dropLast(1)
                            } else {
                                currentStep = SetupStep.CREATE_PIN
                            }
                        }
                    }
                },
                showBiometric = false
            )
        }
    }

    if (showBiometricPromptDialog) {
        AlertDialog(
            onDismissRequest = {
                // If dismissed without choosing, complete with PIN only
                showBiometricPromptDialog = false
                viewModel.completeSetup(firstPin, enableBiometric = false)
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.setup_biometric_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.setup_biometric_subtitle),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBiometricPromptDialog = false
                        viewModel.completeSetup(firstPin, enableBiometric = true)
                    }
                ) {
                    Text("Enable Biometrics")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showBiometricPromptDialog = false
                        viewModel.completeSetup(firstPin, enableBiometric = false)
                    }
                ) {
                    Text("PIN Only")
                }
            }
        )
    }
}
