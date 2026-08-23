package com.vinaynalavade.expensetracker.presentation.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.vinaynalavade.expensetracker.R
import com.vinaynalavade.expensetracker.presentation.security.NumericKeypad
import com.vinaynalavade.expensetracker.presentation.security.PinDotsIndicator
import kotlinx.coroutines.delay

@Composable
fun DisableAppLockVerificationDialog(
    onDismiss: () -> Unit,
    onVerifyAndDisable: (pin: String, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit,
    getLockoutSeconds: () -> Long,
    onDisabledSuccess: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var lockoutSeconds by remember { mutableStateOf(getLockoutSeconds()) }

    LaunchedEffect(Unit) {
        val remaining = getLockoutSeconds()
        if (remaining > 0) {
            lockoutSeconds = remaining
            errorMessage = "Too many incorrect attempts. Try again in $remaining seconds."
        }
    }

    LaunchedEffect(lockoutSeconds) {
        if (lockoutSeconds > 0) {
            delay(1000)
            val next = getLockoutSeconds()
            lockoutSeconds = next
            if (next > 0) {
                errorMessage = "Too many incorrect attempts. Try again in $next seconds."
            } else {
                errorMessage = null
            }
        }
    }

    AlertDialog(
        onDismissRequest = {
            enteredPin = ""
            errorMessage = null
            onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        ),
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .padding(vertical = 16.dp),
        icon = {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        },
        title = {
            Text(
                text = stringResource(R.string.verify_pin_title),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.verify_pin_desc),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // PIN Dots Indicator
                PinDotsIndicator(
                    pinLength = 4,
                    filledCount = enteredPin.length,
                    isError = errorMessage != null
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Animated Error Message
                AnimatedVisibility(
                    visible = errorMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        text = errorMessage.orEmpty(),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Numeric Keypad
                NumericKeypad(
                    onDigit = { digit ->
                        if (lockoutSeconds == 0L && enteredPin.length < 4) {
                            errorMessage = null
                            val nextPin = enteredPin + digit
                            enteredPin = nextPin
                            if (nextPin.length == 4) {
                                onVerifyAndDisable(
                                    nextPin,
                                    {
                                        enteredPin = ""
                                        errorMessage = null
                                        onDisabledSuccess()
                                    },
                                    { error ->
                                        enteredPin = ""
                                        errorMessage = error
                                        val remaining = getLockoutSeconds()
                                        if (remaining > 0) {
                                            lockoutSeconds = remaining
                                        }
                                    }
                                )
                            }
                        }
                    },
                    onDelete = {
                        if (enteredPin.isNotEmpty()) {
                            enteredPin = enteredPin.dropLast(1)
                            errorMessage = null
                        }
                    },
                    enabled = lockoutSeconds == 0L,
                    showBiometric = false
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(
                    onClick = {
                        enteredPin = ""
                        errorMessage = null
                        onDismiss()
                    }
                ) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }
    )
}
