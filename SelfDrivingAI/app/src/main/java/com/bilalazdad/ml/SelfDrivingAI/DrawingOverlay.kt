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
import android.graphics.*
import android.util.AttributeSet
import android.view.View

// Custom View class to display detected objects and overlay information on the screen
class DrawingOverlay(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f
        textSize = 40f
        isAntiAlias = true
    }
    private val textPaint = Paint().apply {
        color = Color.YELLOW
        textSize = 40f
        isAntiAlias = true
    }
    var isFrontCameraOn: Boolean = false
    var detections: List<DetectionResult> = listOf()

    // Define colors for each object class
    private val classColors = mapOf(
        "person" to Color.RED,
        "bicycle" to Color.GREEN,
        "car" to Color.BLUE,
        "motorcycle" to Color.YELLOW

    )

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        detections.forEach { detection ->
            val rect = detection.boundingBox

            // Select color based on detected class (label)
            paint.color = classColors[detection.label] ?: Color.WHITE  // Default to white if label not found

            if (isFrontCameraOn) {
                // Adjust for front camera mirroring
                val mirroredRect = Rect(
                    width - rect.right,
                    rect.top,
                    width - rect.left,
                    rect.bottom
                )

                // Draw bounding box and depth info
                canvas.drawRect(mirroredRect, paint)
                canvas.drawText("${detection.label} (${detection.depth} m)", mirroredRect.left.toFloat(), mirroredRect.top.toFloat(), textPaint)
            } else {
                // Draw bounding box and depth info for rear camera
                canvas.drawRect(rect, paint)
                canvas.drawText("${detection.label} (${detection.depth} m)", rect.left.toFloat(), rect.top.toFloat(), textPaint)
            }
        }
    }

    fun updateDetections(detections: List<DetectionResult>) {
        this.detections = detections
        invalidate()  // Request a redraw
    }
}
