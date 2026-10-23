package com.lucas.guitartuner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.lucas.guitartuner.ui.TunerScreen
import com.lucas.guitartuner.ui.TunerTheme

class MainActivity : ComponentActivity() {
    private val viewModel: TunerViewModel by viewModels()

    private val requestMic = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TunerTheme {
                TunerScreen(
                    viewModel = viewModel,
                    onStart = ::startWithPermission,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasMicPermission()) viewModel.start()
    }

    override fun onPause() {
        super.onPause()
        viewModel.stop()
    }

    private fun startWithPermission() {
        if (hasMicPermission()) viewModel.start() else requestMic.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun hasMicPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
}
