package com.bilalazdad.ml.SelfDrivingAI

/*
 * Copyright 2021 Shubham Panchal
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

import android.graphics.*
import android.media.Image
import java.io.ByteArrayOutputStream


class BitmapUtils {

    companion object {
        // Rotates a given bitmap by a specified degree
        fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
            val matrix = Matrix()
            matrix.postRotate(degrees)
            return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, false)
        }


        // Converts a float array (representing an image) into a bitmap
        fun byteBufferToBitmap(imageArray: FloatArray, imageDim: Int): Bitmap {
            val pixels = imageArray.map { it.toInt() }.toIntArray()
            val bitmap = Bitmap.createBitmap(imageDim, imageDim, Bitmap.Config.RGB_565)
            for (i in 0 until imageDim) {
                for (j in 0 until imageDim) {
                    val p = pixels[i * imageDim + j]
                    bitmap.setPixel(j, i, Color.rgb(p, p, p))
                }
            }
            return bitmap
        }

        // Converts an image from YUV format to bitmap and applies rotation if necessary
        fun imageToBitmap(image: Image, rotationDegrees: Int): Bitmap {
            val yBuffer = image.planes[0].buffer
            val uBuffer = image.planes[1].buffer
            val vBuffer = image.planes[2].buffer
            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()
            val nv21 = ByteArray(ySize + uSize + vSize)
            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)
            val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 100, out)
            val yuv = out.toByteArray()
            return rotateBitmap(BitmapFactory.decodeByteArray(yuv, 0, yuv.size), rotationDegrees.toFloat())
        }
    }
}
