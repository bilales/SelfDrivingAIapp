/*
 * Copyright 2024 Bilal Azdad
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.bilalazdad.ml.SelfDrivingAI

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Size
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors


// MainActivity class that handles the camera preview, switching cameras,
// and processing frames for object detection and depth estimation
class MainActivity : AppCompatActivity() {

    // UI components for camera preview and overlay
    private lateinit var previewView: PreviewView
    private lateinit var drawingOverlay: DrawingOverlay

    // Camera preview and frame analysis components
    private var preview: Preview? = null
    private lateinit var cameraProviderListenableFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var frameAnalyser: FrameProcessing
    private var frameAnalysis: ImageAnalysis? = null

    // Flag to track which camera is being used (front or back)
    private var isFrontCameraOn = true

    // Called when the activity is created
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set the activity to full-screen mode
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        setContentView(R.layout.activity_main)

        // Initialize UI components
        previewView = findViewById(R.id.camera_preview_view)
        drawingOverlay = findViewById(R.id.camera_drawing_overlay)
        drawingOverlay.setWillNotDraw(false) // Ensure the overlay is drawn

        // Initialize the depth estimation and object detection models
        val depthEstimationModel = MiDASModel(this)
        val objectDetector = ObjectDetector(this, "2.tflite")

        // Initialize the frame analyser with the models and overlay
        frameAnalyser = FrameProcessing(depthEstimationModel, objectDetector, drawingOverlay)

        // Set up the FloatingActionButton for switching cameras (front/back)
        val flipCameraFAB = findViewById<FloatingActionButton>(R.id.flip_camera_fab)
        flipCameraFAB.setOnClickListener {
            // Toggle the camera between front and back
            when (isFrontCameraOn) {
                true -> setupCameraProvider(CameraSelector.LENS_FACING_BACK)
                false -> setupCameraProvider(CameraSelector.LENS_FACING_FRONT)
            }
            // Update the overlay to know which camera is being used
            drawingOverlay.isFrontCameraOn = !isFrontCameraOn
            isFrontCameraOn = !isFrontCameraOn // Toggle the camera flag
        }

        // Check if the camera permission is granted; if not, request it
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission()
        } else {
            // If permission is granted, set up the front camera by default
            setupCameraProvider(CameraSelector.LENS_FACING_FRONT)
        }
    }

    // Request camera permission from the user
    private fun requestCameraPermission() {
        requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // Callback for handling the result of the camera permission request
    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            // If permission is granted, set up the front camera
            setupCameraProvider(CameraSelector.LENS_FACING_FRONT)
        } else {
            // If permission is denied, show a dialog explaining the need for permission
            val alertDialog = AlertDialog.Builder(this).apply {
                setTitle("Permissions")
                setMessage("The app requires the camera permission to function.")
                setPositiveButton("GRANT") { dialog, _ ->
                    dialog.dismiss()
                    requestCameraPermission() // Retry requesting the permission
                }
                setNegativeButton("CLOSE") { dialog, _ ->
                    dialog.dismiss()
                    finish() // Close the app if permission is denied
                }
                setCancelable(false)
                create()
            }
            alertDialog.show()
        }
    }

    // Sets up the camera provider to manage the camera lifecycle and configure the camera
    private fun setupCameraProvider(cameraFacing: Int) {
        // Get an instance of the camera provider asynchronously
        cameraProviderListenableFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderListenableFuture.addListener({
            try {
                // Retrieve the camera provider and bind the preview and analysis
                val cameraProvider: ProcessCameraProvider = cameraProviderListenableFuture.get()
                bindPreview(cameraProvider, cameraFacing)
            } catch (e: ExecutionException) {
                e.printStackTrace() // Handle execution exception
            } catch (e: InterruptedException) {
                e.printStackTrace() // Handle interruption exception
            }
        }, ContextCompat.getMainExecutor(this)) // Execute on the main thread
    }

    // Binds the camera preview and frame analysis to the camera lifecycle
    private fun bindPreview(cameraProvider: ProcessCameraProvider, lensFacing: Int) {
        // Unbind any existing preview and frame analysis to avoid conflicts
        if (preview != null && frameAnalysis != null) {
            cameraProvider.unbind(preview, frameAnalysis)
        }

        // Set up the camera preview
        preview = Preview.Builder().build()
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing) // Specify front or back camera
            .build()

        // Connect the preview to the surface provider (the previewView)
        preview!!.setSurfaceProvider(previewView.surfaceProvider)

        // Get the screen size to adjust the frame analysis resolution
        val displayMetrics = resources.displayMetrics
        val screenSize = Size(displayMetrics.widthPixels, displayMetrics.heightPixels)

        // Set up the image analysis for frame processing with the appropriate resolution
        frameAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(screenSize) // Set resolution to screen size
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) // Drop old frames
            .build()

        // Set the analyzer that will process each frame using a background thread
        frameAnalysis!!.setAnalyzer(Executors.newSingleThreadExecutor(), frameAnalyser)

        // Bind the preview and frame analysis to the camera lifecycle
        cameraProvider.bindToLifecycle(
            this as LifecycleOwner, // The current activity lifecycle
            cameraSelector,         // The selected camera (front or back)
            frameAnalysis,          // The frame analysis for object detection and depth
            preview                 // The camera preview
        )
    }
}
