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

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.system.measureTimeMillis

// FrameProcessing class handles image analysis, depth estimation, and object detection
class FrameProcessing(
    private var depthEstimationModel: MiDASModel, //depth estimation model
    private var objectDetector: ObjectDetector,   // object detection model
    private var drawingOverlay: DrawingOverlay    // overlay to display the results
) : ImageAnalysis.Analyzer {

    private var frameBitmap: Bitmap? = null  // Holds the current frame as bitmap
    private var isFrameProcessing = false  // Flag to avoid concurrent processing


    override fun analyze(image: ImageProxy) {
        if (isFrameProcessing) { // Skip processing if the current frame is still being processed
            image.close()
            return
        }
        isFrameProcessing = true

        // If the frame has image data, convert it to bitmap and process it
        if (image.image != null) {
            frameBitmap = BitmapUtils.imageToBitmap(image.image!!, image.imageInfo.rotationDegrees)
            image.close()
            CoroutineScope(Dispatchers.Main).launch {
                runModels(frameBitmap!!)
            }
        }
    }

    // Executes both the depth estimation and object detection models asynchronously
    private suspend fun runModels(inputImage: Bitmap) = withContext(Dispatchers.Default) {
        val originalWidth = inputImage.width
        val originalHeight = inputImage.height

        var depthMap: Bitmap
        var detections: List<DetectionResult>

        var depthInferenceTime: Long
        var detectionInferenceTime: Long

        depthInferenceTime = measureTimeMillis {
            depthMap = depthEstimationModel.getDepthMap(inputImage)
        }

        detectionInferenceTime = measureTimeMillis {
            detections = objectDetector.detectObjects(inputImage)
        }

        Log.i("Depth_Estimation_App", "MiDaS inference speed: $depthInferenceTime")
        Log.i("Depth_Estimation_App", "SSD inference speed: $detectionInferenceTime")
        Log.d("FrameAnalyser", "Detections: $detections")

        // Scale factors to adjust bounding boxes to camera feed size
        val scaleDetectionX = originalWidth.toFloat() / 300
        val scaleDetectionY = originalHeight.toFloat() / 300

        // Scale factors to adjust coordinates to depth map size
        val scaleToDepthMapX = 256f / originalWidth
        val scaleToDepthMapY = 256f / originalHeight

        // Get the min and max depth values from the depth map
        val (dmin, dmax) = getDepthMinMax(depthMap)

        withContext(Dispatchers.Main) {
            isFrameProcessing = false
            // Adjust detection results based on scale factors and depth map values
            drawingOverlay.detections = detections.map { detection ->
                val boundingBox = detection.boundingBox


                // Fixed offsets for bounding box position adjustment, although this part needs work
                val offsetX = -10  // Shift bounding box 10 pixels to the left
                val offsetY = 0  // No vertical shift

                // Scale and adjust bounding box dimensions, factor found empirically, this part needs work
                val adjustedRect = Rect(
                    (boundingBox.left * scaleDetectionX * 0.7 + offsetX).toInt(),
                    (boundingBox.top * scaleDetectionY * 0.7 + offsetY).toInt(),
                    (boundingBox.right * scaleDetectionX * 0.7 + offsetX).toInt(),
                    (boundingBox.bottom * scaleDetectionY * 0.7 + offsetY).toInt()
                )

                // Calculate the center of the bounding box for depth calculation
                val centerX = ((adjustedRect.left + adjustedRect.right) / 2) * scaleToDepthMapX
                val centerY = ((adjustedRect.top + adjustedRect.bottom) / 2) * scaleToDepthMapY

                // Calculate average depth value at the center of the bounding box
                val depthValue = calculateAverageDepth(centerX.toInt(), centerY.toInt(), depthMap)

                // Calibrate the depth value to a certain range
                val calibratedDepth = calibrateDepth(depthValue, dmin, dmax, 0f, 20f)

                // Return updated detection result with bounding box and depth information
                detection.copy(boundingBox = adjustedRect, depth = calibratedDepth)
            }
            drawingOverlay.invalidate()  // Redraw the overlay with updated detection results
        }
    }

    // Calculates average depth from a small neighborhood around the center point
    private fun calculateAverageDepth(centerX: Int, centerY: Int, depthMap: Bitmap): Float {
        var sumDepth = 0f
        var count = 0

        for (dx in -1..1) {
            for (dy in -1..1) {
                val x = centerX + dx
                val y = centerY + dy

                if (x >= 0 && x < 256 && y >= 0 && y < 256) {
                    sumDepth += depthMap.getPixel(x, y).toFloat()
                    count++
                }
            }
        }

        return if (count > 0) sumDepth / count else 0f
    }

    // Finds the minimum and maximum depth values in the depth map
    private fun getDepthMinMax(depthMap: Bitmap): Pair<Float, Float> {
        var min = Float.MAX_VALUE
        var max = Float.MIN_VALUE

        for (x in 0 until depthMap.width) {
            for (y in 0 until depthMap.height) {
                val depthValue = depthMap.getPixel(x, y).toFloat()
                if (depthValue < min) min = depthValue
                if (depthValue > max) max = depthValue
            }
        }

        return Pair(min, max)
    }

    // Normalizes the depth value to a specified range
    private fun calibrateDepth(depth: Float, dmin: Float, dmax: Float, newMin: Float, newMax: Float): Float {
        // Normaliser la profondeur entre 0 et 1
        val normalizedDepth = (depth - dmin) / (dmax - dmin)
        // Recalculer la profondeur dans la nouvelle plage [newMin, newMax]

        return (normalizedDepth * (newMax - newMin) + newMin)

    }
}

