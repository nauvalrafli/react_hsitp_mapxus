package com.mapxushsitp.view.settingsComponents

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapxushsitp.arComponents.ARNavigationViewModel
import com.mapxushsitp.motionSensor.MotionSensorViewModel

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun SettingsView(arNavigationViewModel: ARNavigationViewModel, motionSensorViewModel: MotionSensorViewModel) {
    var isEnablingMotionSensor by rememberSaveable { mutableStateOf(false) }

    Scaffold { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Motion Sensor", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                androidx.compose.material3.Switch(
                    checked = isEnablingMotionSensor,
                    onCheckedChange = {
                        isEnablingMotionSensor = it
                        if (it) {
                            motionSensorViewModel.start()
                        } else {
                            motionSensorViewModel.stop()
                        }
                    }
                )
            }
        }
    }
}
