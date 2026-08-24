package com.vinaynalavade.expensetracker.presentation.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vinaynalavade.expensetracker.R
import com.vinaynalavade.expensetracker.domain.model.GoogleAccountInfo
import com.vinaynalavade.expensetracker.domain.model.GoogleBackupState
import com.vinaynalavade.expensetracker.domain.model.UserPreferences
import com.vinaynalavade.expensetracker.presentation.theme.CardShape
import com.vinaynalavade.expensetracker.presentation.theme.PillShape
import com.vinaynalavade.expensetracker.presentation.theme.spacing

/**
 * Premium Profile Card displayed at the top of the Settings screen.
 * Displays user avatar, custom or Google display name, account status, and actions.
 */
@Composable
fun ProfileCard(
    userPreferences: UserPreferences,
    googleBackupState: GoogleBackupState,
    onAvatarClick: () -> Unit,
    onEditNameClick: () -> Unit,
    onConnectGoogleClick: () -> Unit,
    onDisconnectGoogleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isConnected = googleBackupState is GoogleBackupState.Connected
    val connectedAccount: GoogleAccountInfo? = (googleBackupState as? GoogleBackupState.Connected)?.account

    val effectiveDisplayName = when {
        !userPreferences.userName.isNullOrBlank() -> userPreferences.userName
        isConnected && !connectedAccount?.displayName.isNullOrBlank() -> connectedAccount?.displayName!!
        else -> stringResource(R.string.profile_default_local_name)
    }

    Card(
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = CardShape
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile Avatar
                ProfileAvatar(
                    imageUri = userPreferences.profileImageUri,
                    displayName = effectiveDisplayName,
                    size = 64.dp,
                    showEditBadge = true,
                    onEditClick = onAvatarClick
                )

                Spacer(modifier = Modifier.width(MaterialTheme.spacing.md))

                // Name & Account Status
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = effectiveDisplayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        IconButton(
                            onClick = onEditNameClick,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.profile_edit_name_title),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = if (isConnected) {
                            connectedAccount?.email ?: stringResource(R.string.profile_connected_google)
                        } else {
                            stringResource(R.string.profile_local_account_desc)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Account State Pill Badge
                    AccountStateBadge(isConnected = isConnected)
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isConnected) {
                    OutlinedButton(
                        onClick = onDisconnectGoogleClick,
                        shape = PillShape,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.profile_btn_disconnect),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                } else {
                    Button(
                        onClick = onConnectGoogleClick,
                        shape = PillShape,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.profile_btn_connect_google),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountStateBadge(isConnected: Boolean) {
    val bgColor = if (isConnected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
    }
    val contentColor = if (isConnected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = PillShape,
        color = bgColor,
        modifier = Modifier.border(
            width = 0.5.dp,
            color = contentColor.copy(alpha = 0.3f),
            shape = PillShape
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (isConnected) Icons.Default.CloudDone else Icons.Default.PhoneAndroid,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = if (isConnected) {
                    stringResource(R.string.profile_status_google_connected)
                } else {
                    stringResource(R.string.profile_status_local_account)
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = contentColor
            )
        }
    }
}
