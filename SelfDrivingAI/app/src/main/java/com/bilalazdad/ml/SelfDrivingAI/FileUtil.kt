package com.bilalazdad.ml.SelfDrivingAI

import android.content.Context

object FileUtil {
    fun loadLabels(context: Context, fileName: String): List<String> {
        return context.assets.open(fileName).bufferedReader().useLines { it.toList() }
    }
}
