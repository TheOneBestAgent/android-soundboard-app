package com.soundboard.android.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.soundboard.android.data.model.ConnectionHistory
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionDialog(
    onDismiss: () -> Unit,
    onConnect: (ipAddress: String, port: Int) -> Unit,
    isConnecting: Boolean = false,
    currentIpAddress: String = "",
    currentPort: Int = 8080,
    connectionHistory: List<ConnectionHistory> = emptyList()
) {
    var ipAddress by remember { mutableStateOf(TextFieldValue(currentIpAddress)) }
    var port by remember { mutableStateOf(TextFieldValue(currentPort.toString())) }
    var ipError by remember { mutableStateOf<String?>(null) }
    var portError by remember { mutableStateOf<String?>(null) }
    var showHistory by remember { mutableStateOf(false) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with toggle button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (showHistory) "Recent Connections" else "Connect to Server",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    
                    if (connectionHistory.isNotEmpty()) {
                        IconButton(
                            onClick = { showHistory = !showHistory }
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Connection History"
                            )
                        }
                    }
                }
                
                if (showHistory && connectionHistory.isNotEmpty()) {
                    // Connection History List
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(connectionHistory) { connection ->
                            ConnectionHistoryItem(
                                connection = connection,
                                onClick = {
                                    ipAddress = TextFieldValue(connection.ipAddress)
                                    port = TextFieldValue(connection.port.toString())
                                    showHistory = false
                                }
                            )
                        }
                    }
                } else {
                    // Manual Connection Form
                    Text(
                        text = "Enter the IP address and port of your computer running the soundboard server.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // IP Address field
                    OutlinedTextField(
                        value = ipAddress,
                        onValueChange = { 
                            ipAddress = it
                            ipError = null
                        },
                        label = { Text("IP Address") },
                        placeholder = { Text("192.168.1.100") },
                        singleLine = true,
                        enabled = !isConnecting,
                        isError = ipError != null,
                        supportingText = if (ipError != null) {
                            { Text(ipError!!) }
                        } else null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Port field
                    OutlinedTextField(
                        value = port,
                        onValueChange = { 
                            port = it
                            portError = null
                        },
                        label = { Text("Port") },
                        placeholder = { Text("8080") },
                        singleLine = true,
                        enabled = !isConnecting,
                        isError = portError != null,
                        supportingText = if (portError != null) {
                            { Text(portError!!) }
                        } else null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Quick connect options
                    Text(
                        text = "Quick Connect",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                ipAddress = TextFieldValue("192.168.3.39")
                                port = TextFieldValue("8080")
                            },
                            enabled = !isConnecting,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Mac Server")
                        }
                        
                        OutlinedButton(
                            onClick = {
                                ipAddress = TextFieldValue("10.0.2.2")
                                port = TextFieldValue("8080")
                            },
                            enabled = !isConnecting,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Emulator")
                        }
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                }
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showHistory) {
                        TextButton(
                            onClick = { showHistory = false },
                            enabled = !isConnecting
                        ) {
                            Text("Manual")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isConnecting
                    ) {
                        Text("Cancel")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            // Validate inputs
                            var hasError = false
                            
                            if (ipAddress.text.isBlank()) {
                                ipError = "IP address is required"
                                hasError = true
                            }
                            
                            val portInt = port.text.toIntOrNull()
                            if (portInt == null || portInt !in 1..65535) {
                                portError = "Port must be between 1 and 65535"
                                hasError = true
                            }
                            
                            if (!hasError) {
                                onConnect(ipAddress.text.trim(), portInt!!)
                            }
                        },
                        enabled = !isConnecting
                    ) {
                        if (isConnecting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(if (isConnecting) "Connecting..." else "Connect")
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionHistoryItem(
    connection: ConnectionHistory,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = connection.computerName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${connection.ipAddress}:${connection.port}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Last used: ${formatLastUsed(connection.lastConnected)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (connection.isFavorite) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Favorite",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private fun formatLastUsed(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60 * 1000 -> "Just now"
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)} minutes ago"
        diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)} hours ago"
        else -> {
            val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            formatter.format(Date(timestamp))
        }
    }
} 