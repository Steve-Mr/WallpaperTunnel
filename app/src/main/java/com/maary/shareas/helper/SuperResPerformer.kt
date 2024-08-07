package com.maary.shareas.helper

import ai.onnxruntime.OnnxJavaType
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.Collections

internal data class Result(
    var outputBitmap: Bitmap? = null
)

internal class SuperResPerformer {

    fun upscale(inputStream: InputStream, ortEnv: OrtEnvironment, ortSession: OrtSession): Result {
        val result = Result()

        // Step 1: convert image into byte array (raw image bytes)
        val rawImageBytes = inputStream.readBytes()

        // Step 2: get the shape of the byte array and make ort tensor
        val shape = longArrayOf(rawImageBytes.size.toLong())

        val inputTensor = OnnxTensor.createTensor(
            ortEnv,
            ByteBuffer.wrap(rawImageBytes),
            shape,
            OnnxJavaType.UINT8
        )

        inputTensor.use {
            // Step 3: call ort inferenceSession run
            Log.v("WVM", "RUN")

            val output = ortSession.run(Collections.singletonMap("image", inputTensor))
            Log.v("WVM", "RUN FINISHED")


            // Step 4: output analysis
            output.use {
                val rawOutput = (output?.get(0)?.value) as ByteArray
                val outputImageBitmap =
                    byteArrayToBitmap(rawOutput)

                // Step 5: set output result
                result.outputBitmap = outputImageBitmap
            }
        }
        return result
    }

    private fun byteArrayToBitmap(data: ByteArray): Bitmap {
        return BitmapFactory.decodeByteArray(data, 0, data.size)
    }
}