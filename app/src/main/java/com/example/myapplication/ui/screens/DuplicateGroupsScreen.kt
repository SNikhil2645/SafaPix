package com.example.myapplication.ui.screens

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.model.DuplicateGroup
import com.example.myapplication.ui.components.DeleteConfirmationDialog
import com.example.myapplication.ui.components.DuplicateGroupCard
import com.example.myapplication.viewmodel.DuplicateDetectorViewModel

@Composable
fun DuplicateGroupsScreen(
    duplicateGroups: List<DuplicateGroup>,
    viewModel: DuplicateDetectorViewModel
) {
    var showDeleteDialog by remember { mutableStateOf<DuplicateGroup?>(null) }
    val context = LocalContext.current

    val totalDuplicates = duplicateGroups.sumOf { it.duplicates.size }
    val totalSpaceToSave = duplicateGroups.sumOf { group -> group.duplicates.sumOf { it.size } }
    val formattedTotalSize = Formatter.formatShortFileSize(context, totalSpaceToSave)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F2027),
                        Color(0xFF203A43),
                        Color(0xFF2C5364)
                    )
                )
            )
    ) {
        // Summary Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .background(
                    color = Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(32.dp)
                )
                .padding(24.dp)
        ) {
            Column {
                Text(
                    text = "Summary",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "$totalDuplicates Duplicates",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Found in ${duplicateGroups.size} groups",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                    
                    Text(
                        text = formattedTotalSize,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF00D2FF)
                    )
                }
            }
        }

        // List of Duplicate Groups
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            items(duplicateGroups, key = { it.hash }) { group ->
                DuplicateGroupCard(
                    group = group,
                    onDeleteClicked = { showDeleteDialog = it }
                )
            }
        }
    }

    // Delete Confirmation Dialog
    showDeleteDialog?.let { group ->
        DeleteConfirmationDialog(
            group = group,
            onConfirm = {
                viewModel.deleteDuplicates(listOf(group))
                showDeleteDialog = null
            },
            onDismiss = { showDeleteDialog = null }
        )
    }
}
