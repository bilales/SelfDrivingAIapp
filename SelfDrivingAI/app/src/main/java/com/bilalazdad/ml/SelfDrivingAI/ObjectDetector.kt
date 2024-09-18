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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import org.tensorflow.lite.task.vision.detector.ObjectDetector.ObjectDetectorOptions
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.ops.ResizeOp

/**
 * A class for detecting objects in images using a TensorFlow Lite model.
 *
 * @property context The context required to load the TensorFlow Lite model.
 * @property modelPath The file path of the TensorFlow Lite model.
 */
class ObjectDetector(context: Context, modelPath: String) {
    // The ObjectDetector instance created from the TensorFlow Lite model and options.
    private val detector: ObjectDetector
    // The width and height used to resize the input image for the model.
    private val inputImageWidth = 300  // width of the input image expected by the model
    private val inputImageHeight = 300 // height of the input image expected by the model

    /**
     * Initializes the ObjectDetector by setting up the model and options.
     */
    init {
        // Create options for the object detector.
        val options = ObjectDetectorOptions.builder()
            .setScoreThreshold(0.5f)  // Minimum score threshold for detected objects
            .setMaxResults(10)        // Maximum number of objects to detect
            .build()

        // Create the ObjectDetector instance with the specified model and options.
        detector = ObjectDetector.createFromFileAndOptions(context, modelPath, options)
    }

    /**
     * Detects objects in the given image.
     *
     * @param image The Bitmap image to process.
     * @return A list of DetectionResult objects representing detected objects.
     */
    fun detectObjects(image: Bitmap): List<DetectionResult> {
        // Convert the Bitmap image to a TensorImage.
        val tensorImage = TensorImage.fromBitmap(image)

        // Set up an image processor to resize and normalize the image.
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(inputImageWidth, inputImageHeight, ResizeOp.ResizeMethod.BILINEAR)) // Resize image
            .add(NormalizeOp(0f, 1f))  // Normalize pixel values to the range [0, 1]
            .build()

        // Process the TensorImage using the image processor.
        val processedImage = imageProcessor.process(tensorImage)

        // Perform object detection on the processed image.
        val detections = detector.detect(processedImage)

        // Map detection results to DetectionResult objects, filtering by allowed labels.
        return detections.mapNotNull { detection ->
            // Get the first category of detected objects.
            val category = detection.categories.firstOrNull()
            val label = category?.label
            // Check if the detected label is in the list of allowed labels.
            if (label != null && label in allowedLabels) {
                // Convert the bounding box coordinates to an integer rectangle.
                val boundingBox = detection.boundingBox
                val rect = Rect(
                    boundingBox.left.toInt(),
                    boundingBox.top.toInt(),
                    boundingBox.right.toInt(),
                    boundingBox.bottom.toInt()
                )
                // Create a DetectionResult object for the detected object.
                DetectionResult(rect, label, category.score)
            } else {
                // If the label is not allowed, return null.
                null
            }
        }
    }

    companion object {
        // List of labels that are allowed for detection.
        private val allowedLabels = listOf("person", "bicycle", "car", "motorcycle")
    }
}

/**
 * A data class representing the result of an object detection.
 *
 * @property boundingBox The bounding box of the detected object.
 * @property label The label or category of the detected object.
 * @property depth The score or confidence of the detection.
 */
data class DetectionResult(val boundingBox: Rect, val label: String, var depth: Float)
