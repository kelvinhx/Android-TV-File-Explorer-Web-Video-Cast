package com.example

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object TvNavigationManager {
    // Current browsing path
    private val _currentPath = MutableStateFlow("/storage/emulated/0")
    val currentPath = _currentPath.asStateFlow()

    // History stack of visited paths (to handle back events)
    private val pathHistory = ArrayList<String>()

    // Last focused item names by parent path
    private val lastFocusedNames = mutableMapOf<String, String>()
    // Last focused index by parent path
    private val lastFocusedIndexes = mutableMapOf<String, Int>()

    // Target focus configuration
    private val _targetFocusIdentifier = MutableStateFlow<FocusTarget?>(FocusTarget(TargetType.FIRST_ITEM))
    val targetFocusIdentifier = _targetFocusIdentifier.asStateFlow()

    data class FocusTarget(
        val type: TargetType,
        val itemName: String? = null,
        val index: Int = -1
    )

    enum class TargetType {
        FIRST_ITEM,
        SPECIFIC_ITEM,
        BACK_BUTTON
    }

    /**
     * Resets the navigation state to a base path
     */
    fun resetToPath(path: String) {
        pathHistory.clear()
        lastFocusedNames.clear()
        lastFocusedIndexes.clear()
        _currentPath.value = path
        _targetFocusIdentifier.value = FocusTarget(TargetType.FIRST_ITEM)
        Logger.log("[Navigation] Reset to $path")
    }

    /**
     * Navigate deeper into a sub-directory
     */
    fun enterDirectory(parentPath: String, currentItemName: String, currentItemIndex: Int, targetPath: String) {
        lastFocusedNames[parentPath] = currentItemName
        lastFocusedIndexes[parentPath] = currentItemIndex
        if (!pathHistory.contains(parentPath)) {
            pathHistory.add(parentPath)
        }
        
        _currentPath.value = targetPath
        _targetFocusIdentifier.value = FocusTarget(TargetType.FIRST_ITEM)
        Logger.log("[Navigation] Entering directory: $targetPath (Saved focus target for $parentPath: $currentItemName @ idx $currentItemIndex)")
    }

    /**
     * Navigate back to the parent directory (or return false if base path reached)
     */
    fun navigateBack(context: Context, onBackToDashboard: () -> Unit) {
        if (pathHistory.isNotEmpty()) {
            val previousPath = pathHistory.removeAt(pathHistory.size - 1)
            val savedName = lastFocusedNames[previousPath]
            val savedIndex = lastFocusedIndexes[previousPath] ?: 0

            _currentPath.value = previousPath
            _targetFocusIdentifier.value = FocusTarget(
                type = TargetType.SPECIFIC_ITEM,
                itemName = savedName,
                index = savedIndex
            )
            Logger.log("[Navigation] Navigating back to: $previousPath (Restoring focus to: $savedName @ idx $savedIndex)")
        } else {
            // Manual check if current path can be popped manually
            val current = _currentPath.value
            if (current != "/storage/emulated/0" && current != "/storage") {
                val parent = if (current.endsWith("/")) {
                    current.substringBeforeLast('/').substringBeforeLast('/')
                } else {
                    current.substringBeforeLast('/')
                }
                
                val safeParent = if (parent.isNotEmpty()) parent else "/storage/emulated/0"
                _currentPath.value = safeParent
                _targetFocusIdentifier.value = FocusTarget(TargetType.FIRST_ITEM)
                Logger.log("[Navigation] Manual back to parent: $safeParent")
            } else {
                Logger.log("[Navigation] Root path reached. Backing to dashboard.")
                onBackToDashboard()
            }
        }
    }

    /**
     * Called by UI when an item is focused to update active tracking state
     */
    fun updateActiveFocus(path: String, itemName: String, index: Int) {
        lastFocusedNames[path] = itemName
        lastFocusedIndexes[path] = index
    }
}
