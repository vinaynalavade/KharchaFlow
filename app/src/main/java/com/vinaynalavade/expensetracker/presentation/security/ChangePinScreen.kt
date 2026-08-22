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
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.vinaynalavade.expensetracker.presentation.components.AppTopBar

private enum class ChangePinStep {
    CURRENT_PIN,
    NEW_PIN,
    CONFIRM_NEW_PIN
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePinScreen(
    viewModel: AppLockViewModel,
    onNavigateBack: () -> Unit,
    onPinChanged: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var currentStep by remember { mutableStateOf(ChangePinStep.CURRENT_PIN) }
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmNewPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.settings_change_pin),
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
            // Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LockReset,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = when (currentStep) {
                        ChangePinStep.CURRENT_PIN -> stringResource(R.string.change_pin_current_title)
                        ChangePinStep.NEW_PIN -> stringResource(R.string.change_pin_new_title)
                        ChangePinStep.CONFIRM_NEW_PIN -> stringResource(R.string.change_pin_confirm_title)
                    },
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = when (currentStep) {
                        ChangePinStep.CURRENT_PIN -> stringResource(R.string.change_pin_current_subtitle)
                        ChangePinStep.NEW_PIN -> stringResource(R.string.change_pin_new_subtitle)
                        ChangePinStep.CONFIRM_NEW_PIN -> stringResource(R.string.change_pin_confirm_subtitle)
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    textAlign = TextAlign.Center
                )
            }

            // Middle: PIN dots & Error
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val currentPinLength = when (currentStep) {
                    ChangePinStep.CURRENT_PIN -> currentPin.length
                    ChangePinStep.NEW_PIN -> newPin.length
                    ChangePinStep.CONFIRM_NEW_PIN -> confirmNewPin.length
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
                        ChangePinStep.CURRENT_PIN -> {
                            if (currentPin.length < 4) {
                                val next = currentPin + digit
                                currentPin = next
                                if (next.length == 4) {
                                    currentStep = ChangePinStep.NEW_PIN
                                }
                            }
                        }
                        ChangePinStep.NEW_PIN -> {
                            if (newPin.length < 4) {
                                val next = newPin + digit
                                newPin = next
                                if (next.length == 4) {
                                    currentStep = ChangePinStep.CONFIRM_NEW_PIN
                                }
                            }
                        }
                        ChangePinStep.CONFIRM_NEW_PIN -> {
                            if (confirmNewPin.length < 4) {
                                val next = confirmNewPin + digit
                                confirmNewPin = next
                                if (next.length == 4) {
                                    if (next == newPin) {
                                        viewModel.updatePin(
                                            currentPin = currentPin,
                                            newPin = newPin,
                                            onSuccess = onPinChanged,
                                            onError = { error ->
                                                errorMessage = error
                                                currentPin = ""
                                                newPin = ""
                                                confirmNewPin = ""
                                                currentStep = ChangePinStep.CURRENT_PIN
                                            }
                                        )
                                    } else {
                                        errorMessage = context.getString(R.string.setup_pin_mismatch)
                                        newPin = ""
                                        confirmNewPin = ""
                                        currentStep = ChangePinStep.NEW_PIN
                                    }
                                }
                            }
                        }
                    }
                },
                onDelete = {
                    errorMessage = null
                    when (currentStep) {
                        ChangePinStep.CURRENT_PIN -> {
                            if (currentPin.isNotEmpty()) currentPin = currentPin.dropLast(1)
                        }
                        ChangePinStep.NEW_PIN -> {
                            if (newPin.isNotEmpty()) {
                                newPin = newPin.dropLast(1)
                            } else {
                                currentStep = ChangePinStep.CURRENT_PIN
                            }
                        }
                        ChangePinStep.CONFIRM_NEW_PIN -> {
                            if (confirmNewPin.isNotEmpty()) {
                                confirmNewPin = confirmNewPin.dropLast(1)
                            } else {
                                currentStep = ChangePinStep.NEW_PIN
                            }
                        }
                    }
                },
                showBiometric = false
            )
        }
    }
}
