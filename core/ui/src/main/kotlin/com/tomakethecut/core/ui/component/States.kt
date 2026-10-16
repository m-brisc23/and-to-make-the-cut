package com.tomakethecut.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tomakethecut.core.ui.R
import com.tomakethecut.core.ui.state.ErrorKind
import com.tomakethecut.core.ui.theme.ToMakeTheCutTheme

@Composable
fun ErrorKind.message(): String = stringResource(
    when (this) {
        ErrorKind.NETWORK -> R.string.core_ui_error_network
        ErrorKind.NOT_FOUND -> R.string.core_ui_error_not_found
        ErrorKind.SERVER -> R.string.core_ui_error_server
        ErrorKind.UNKNOWN -> R.string.core_ui_error_unknown
    },
)

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.core_ui_loading)
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = description })
    }
}

@Composable
fun ErrorState(kind: ErrorKind, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = kind.message(),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FilledTonalButton(onClick = onRetry) { Text(stringResource(R.string.core_ui_retry)) }
    }
}

@Composable
fun EmptyState(message: String, modifier: Modifier = Modifier) {
    Text(
        text = message,
        modifier = modifier.fillMaxWidth().padding(24.dp),
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Preview(showBackground = true)
@Composable
private fun ErrorStatePreview() {
    ToMakeTheCutTheme { ErrorState(ErrorKind.NETWORK, onRetry = {}) }
}
