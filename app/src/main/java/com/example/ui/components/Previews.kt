package com.example.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.getMantraColor

@Preview(name = "Nav Bar - Font Scale 1.0", fontScale = 1.0f, showBackground = true)
@Preview(name = "Nav Bar - Font Scale 2.0", fontScale = 2.0f, showBackground = true)
@Composable
fun PreviewFloatingNavBar() {
    MyApplicationTheme {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 58.dp)
                    .padding(6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val items = listOf("Today", "History", "Settings")
                val icons = listOf(Icons.Default.CalendarToday, Icons.Default.History, Icons.Default.Settings)
                items.forEachIndexed { idx, label ->
                    val isSelected = idx == 0
                    Surface(
                        modifier = Modifier
                            .weight(if (isSelected) 1.6f else 1.0f)
                            .heightIn(min = 48.dp),
                        shape = MaterialTheme.shapes.medium,
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Icon(icons[idx], contentDescription = label, tint = MaterialTheme.colorScheme.primary)
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                    softWrap = false,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "Counter Card - Font Scale 1.0", fontScale = 1.0f, showBackground = true)
@Preview(name = "Counter Card - Font Scale 2.0", fontScale = 2.0f, showBackground = true)
@Composable
fun PreviewCounterCard() {
    MyApplicationTheme {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = getMantraColor("Saffron").copy(alpha = 0.12f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Morning (Pratah) Sandhya",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Today Count: 108",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Target: 108",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = getMantraColor("Saffron")
                    )
                }
            }
        }
    }
}

@Preview(name = "Dialog - Font Scale 2.0", fontScale = 2.0f, showBackground = true)
@Composable
fun PreviewEditDialogContent() {
    MyApplicationTheme {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Configure Practice", style = MaterialTheme.typography.titleMedium) },
            text = {
                Column {
                    Text("Enter custom target count below:", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = "108",
                        onValueChange = {},
                        label = { Text("Daily Target") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {},
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {},
                    modifier = Modifier.heightIn(min = 44.dp)
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
