package com.example.myapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.model.DuplicateGroup
import com.example.myapplication.model.ImageData
import com.example.myapplication.repository.ImageRepository
import com.example.myapplication.repository.ScanProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

sealed interface ScanState {
    object Idle : ScanState
    data class Scanning(val progress: Int, val total: Int) : ScanState
    data class Hashing(val progress: Int, val total: Int) : ScanState
    data class Deleting(val progress: Int, val total: Int) : ScanState
    data class Complete(val duplicateGroups: List<DuplicateGroup>, val spaceFreed: Long) : ScanState
}

class DuplicateDetectorViewModel(private val imageRepository: ImageRepository) : ViewModel() {

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val _duplicateGroups = MutableStateFlow<List<DuplicateGroup>>(emptyList())
    val duplicateGroups: StateFlow<List<DuplicateGroup>> = _duplicateGroups.asStateFlow()

    fun startScan() {
        if (_scanState.value !is ScanState.Idle) return

        viewModelScope.launch {
            imageRepository.scanGalleryImages().collect { progress ->
                when (progress) {
                    is ScanProgress.InProgress -> {
                        _scanState.value = ScanState.Scanning(progress.scanned, progress.total)
                    }
                    is ScanProgress.Completed -> {
                        detectDuplicatesInParallel(progress.images)
                    }
                }
            }
        }
    }

    private suspend fun detectDuplicatesInParallel(images: List<ImageData>) {
        val totalImages = images.size
        val hashedCount = AtomicInteger(0)

        _scanState.value = ScanState.Hashing(0, totalImages)

        val imagesWithHashes = withContext(Dispatchers.IO) {
            images.map { imageData ->
                async {
                    val hash = imageRepository.hashImage(imageData.uri)
                    val currentProgress = hashedCount.incrementAndGet()
                    _scanState.value = ScanState.Hashing(currentProgress, totalImages)
                    if (hash != null) imageData.copy(hash = hash) else null
                }
            }.awaitAll().filterNotNull()
        }

        val imagesByHash = imagesWithHashes.groupBy { it.hash }

        val duplicates = imagesByHash
            .filter { it.value.size > 1 }
            .map { (hash, group) ->
                val sortedGroup = group.sortedBy { it.dateAdded }
                DuplicateGroup(hash, sortedGroup.first(), sortedGroup.drop(1))
            }

        _duplicateGroups.value = duplicates
        val totalSpaceToSave = duplicates.sumOf { group -> group.duplicates.sumOf { it.size } }
        _scanState.value = ScanState.Complete(duplicates, totalSpaceToSave)
    }

    fun deleteDuplicates(groupsToDelete: List<DuplicateGroup>) {
        val duplicatesToDelete = groupsToDelete.flatMap { it.duplicates }
        if (duplicatesToDelete.isEmpty()) return

        viewModelScope.launch {
            val totalToDelete = duplicatesToDelete.size
            val deletedCount = AtomicInteger(0)
            _scanState.value = ScanState.Deleting(0, totalToDelete)

            val successfullyDeleted = mutableListOf<ImageData>()

            withContext(Dispatchers.IO) {
                duplicatesToDelete.map { duplicate ->
                    async {
                        val group = _duplicateGroups.value.find { g -> g.duplicates.contains(duplicate) }
                        if (group != null && verifyBeforeDelete(duplicate, group.original, group.hash)) {
                            if (imageRepository.deleteImage(duplicate.uri)) {
                                successfullyDeleted.add(duplicate)
                            }
                        }
                        _scanState.value = ScanState.Deleting(deletedCount.incrementAndGet(), totalToDelete)
                    }
                }.awaitAll()
            }

            val updatedGroups = _duplicateGroups.value.mapNotNull { group ->
                val remainingDuplicates = group.duplicates.filterNot { successfullyDeleted.contains(it) }
                if (remainingDuplicates.isEmpty()) null else group.copy(duplicates = remainingDuplicates)
            }

            _duplicateGroups.value = updatedGroups
            val totalSpaceSaved = updatedGroups.sumOf { group -> group.duplicates.sumOf { it.size } }
            _scanState.value = ScanState.Complete(updatedGroups, totalSpaceSaved)
        }
    }

    private suspend fun verifyBeforeDelete(image: ImageData, original: ImageData, expectedHash: String): Boolean {
        // LAYER 1: Hash Verification
        if (imageRepository.hashImage(image.uri) != expectedHash) return false
        
        // LAYER 2: Existence Verification
        if (!imageRepository.verifyImageExists(image.uri) || !imageRepository.verifyImageExists(original.uri)) return false
        
        // LAYER 3: Integrity Verification
        if (image.uri == original.uri) return false

        return true
    }

    fun resetScan() {
        _scanState.value = ScanState.Idle
        _duplicateGroups.value = emptyList()
    }
}
