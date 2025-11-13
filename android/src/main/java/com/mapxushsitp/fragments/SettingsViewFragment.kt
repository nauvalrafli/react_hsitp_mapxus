package com.mapxushsitp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import androidx.fragment.app.Fragment
import com.mapxushsitp.R

class SettingsViewFragment : Fragment() {

    private lateinit var motionSensorSwitch: Switch

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings_view, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupClickListeners()
    }

    private fun initializeViews(view: View) {
        motionSensorSwitch = view.findViewById(R.id.motion_sensor_switch)
    }

    private fun setupClickListeners() {
        motionSensorSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // TODO: Start motion sensor
                onMotionSensorEnabled()
            } else {
                // TODO: Stop motion sensor
                onMotionSensorDisabled()
            }
        }
    }

    private fun onMotionSensorEnabled() {
        // TODO: Implement motion sensor start logic
    }

    private fun onMotionSensorDisabled() {
        // TODO: Implement motion sensor stop logic
    }

    fun setMotionSensorEnabled(enabled: Boolean) {
        motionSensorSwitch.isChecked = enabled
    }
}
