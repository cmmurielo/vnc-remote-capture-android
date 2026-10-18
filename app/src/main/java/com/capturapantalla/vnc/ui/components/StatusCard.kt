package com.capturapantalla.vnc.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.capturapantalla.vnc.ui.theme.StatusError
import com.capturapantalla.vnc.ui.theme.StatusSuccess

@Composable
fun StatusCard(
    isLoading: Boolean,
    progressMessage: String,
    errorMessage: String?,
    successMessage: String?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(visible = isLoading || errorMessage != null || successMessage != null) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    errorMessage != null -> StatusError.copy(alpha = 0.15f)
                    successMessage != null -> StatusSuccess.copy(alpha = 0.15f)
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                }
            ),
            border = BorderStroke(
                width = 1.dp,
                color = when {
                    errorMessage != null -> StatusError.copy(alpha = 0.5f)
                    successMessage != null -> StatusSuccess.copy(alpha = 0.5f)
                    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = progressMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    errorMessage != null -> {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = StatusError,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = StatusError
                        )
                    }
                    successMessage != null -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = StatusSuccess,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = successMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SecurityBadge(modifier: Modifier = Modifier) {
    SuggestionChip(
        onClick = {},
        label = { Text("View-Only: Sin entrada remota") },
        icon = {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        },
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            iconContentColor = MaterialTheme.colorScheme.secondary
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier
    )
}
