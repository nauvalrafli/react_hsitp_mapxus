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
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow

class ARNavigationViewModel : ViewModel() {
    private val _instructionIndex = MutableLiveData(0)
    val instructionIndex: LiveData<Int> get() = _instructionIndex
    var alignedIndexes: java.util.NavigableSet<Int> =
    java.util.TreeSet<Int>().apply { add(0) }
    var hasAlignedOnce: MutableState<Boolean> = mutableStateOf(false)
    var waitingLogShown: MutableState<Boolean> = mutableStateOf(false)

    var isShowingRotatingPhoneMessage: MutableState<Boolean> = mutableStateOf(false)

    var initialCorrectedHeading: Double? = null

    var isShowingARNavigation: MutableState<Boolean> = mutableStateOf(false)
    var isActivatingAR: MutableState<Boolean> = mutableStateOf(false)
    var isShowingOpeningAndClosingARButton: MutableState<Boolean> = mutableStateOf(false)
    var isShowingAndClosingARNavigation: MutableState<Boolean> = mutableStateOf(false)
    var isSelectingGPSCurrentLocation: MutableState<Boolean> = mutableStateOf(false)

    var mutableSharedFlow = MutableSharedFlow<Int>(
        replay = 1,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    // Function to increment the index
    fun nextInstruction(maxIndex: Int) {
        val current = _instructionIndex.value ?: 0
        if (current < maxIndex) {
            _instructionIndex.value = current + 1
        }
    }

    fun prevInstruction() {
        val current = _instructionIndex.value ?: 0
        if (current > 0) {
            _instructionIndex.value = current - 1
        }
    }

    fun setInstructionIndex(index: Int) {
        _instructionIndex.value = index
    }

    // Optionally reset
    fun resetInstruction() {
        _instructionIndex.value = 0
    }
}
