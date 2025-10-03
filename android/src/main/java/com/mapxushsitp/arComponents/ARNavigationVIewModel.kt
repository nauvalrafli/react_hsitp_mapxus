package com.mapxushsitp.arComponents

import android.util.Log
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ARNavigationViewModel : ViewModel() {

    // Backing field for the current instruction index
    private val _instructionIndex = MutableLiveData(0)
    val instructionIndex: LiveData<Int> get() = _instructionIndex

    // AR with Compass
    var arHeadingOffset: Float = 0f
//    var alignedIndexes: MutableList<Int> = mutableListOf(0)
    var alignedIndexes: java.util.NavigableSet<Int> =
    java.util.TreeSet<Int>().apply { add(0) }
    var hasAlignedOnce: MutableState<Boolean> = mutableStateOf(false)
    var waitingLogShown: MutableState<Boolean> = mutableStateOf(false)
    var checkingCompassAlignment: MutableState<Boolean> = mutableStateOf(false)
    var isShowingRotatingPhoneMessage: MutableState<Boolean> = mutableStateOf(false)
    val lastPlacedIndex = mutableStateOf(-1)
    var normalAlignedIndex by mutableStateOf(-1)
        private set

    // AR Heading
    // Keep a nullable var at class level (or ViewModel)
    var initialCorrectedHeading: Double? = null

    // MutableState<Boolean> to track AR view visibility
    var isShowingARNavigation: MutableState<Boolean> = mutableStateOf(false)
    var isActivatingAR: MutableState<Boolean> = mutableStateOf(false)
    var isShowingOpeningAndClosingARButton: MutableState<Boolean> = mutableStateOf(false)
    var isShowingAndClosingARNavigation: MutableState<Boolean> = mutableStateOf(false)
    var isShowingAndClosingMapxusMap: MutableState<Boolean> = mutableStateOf(false)
    var isShowingToolTipBoxMessage: MutableState<Boolean> = mutableStateOf(false)
    var isShowingCountdownScreen: MutableState<Boolean> = mutableStateOf(false)
    var isSelectingGPSCurrentLocation: MutableState<Boolean> = mutableStateOf(false)

    // Function to increment the index
    fun nextInstruction(maxIndex: Int) {
        val current = _instructionIndex.value ?: 0
        if (current < maxIndex) {
            _instructionIndex.value = current + 1
        }
    }

    fun updateAlignedIndex(newIndex: Int) {
        normalAlignedIndex = newIndex
    }

    // Optionally reset
    fun resetInstruction() {
        _instructionIndex.value = 0
    }

//    fun resetPreviousNavigationState(index: Int) {
////        alignedIndexes.removeAll { it > index }
////        alignedIndexes.maxOrNull()?.let { alignedIndexes.remove(it) }
////        alignedIndexes.remove(index)
//    }

//    fun resetPreviousNavigationState(index: Int) {
//        alignedIndexes = alignedIndexes.map { it - index }.toMutableSet()
//        alignedIndexes.forEach {
//            Log.w("ARCoreHeadingDebug", "ar view model aligned index at: $it")
//        }
//    }

    // Adds if missing; sorted, unique
    fun addAlignedIndex(index: Int) {
        val added = alignedIndexes.add(index)
        if (added) {
            Log.d("ARCoreHeadingDebug", "✅ Added aligned index: $index | current=$alignedIndexes")
        } else {
            Log.d("ARCoreHeadingDebug", "🔁 Index $index already present | current=$alignedIndexes")
        }
    }

    // Removes if present; logs before & after so it’s clear what happened
    fun removeAlignedIndex(index: Int) {
        val before = ArrayList(alignedIndexes) // snapshot for logging
        val removed = alignedIndexes.remove(index)
        if (removed) {
            Log.d("ARCoreHeadingDebug",
                "🗑 Removed aligned index: $index | before=$before, after=$alignedIndexes")
        } else {
            Log.d("ARCoreHeadingDebug",
                "⚠️ Index $index not found | current=$alignedIndexes")
        }
    }

    // ✅ Call this whenever you press Start Navigation
    fun resetNavigationState() {
        hasAlignedOnce.value = false
        waitingLogShown.value = false
        alignedIndexes.clear()
    }
}
