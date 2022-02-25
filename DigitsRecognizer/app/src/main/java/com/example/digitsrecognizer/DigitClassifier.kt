
package com.example.digitsrecognizer

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class DigitClassifier(private val context: Context) {
    private var interpreter: Interpreter? = null;
    var isInitialized = false
    private var digitsList = ArrayList<Digit>();

    private val executorService: ExecutorService = Executors.newCachedThreadPool()

    private var inputImageWidth: Int = 0 // will be inferred from TF Lite model.
    private var inputImageHeight: Int = 0 // will be inferred from TF Lite model.
    private var modelInputSize: Int = 0 // will be inferred from TF Lite model.

    fun initialize(): Task<Void?> {
        val task = TaskCompletionSource<Void?>()
        executorService.execute {
            try {
                initializeInterpreter()
                task.setResult(null)
            } catch (e: IOException) {
                task.setException(e)
            }
        }
        return task.task
    }

    @Throws(IOException::class)
    private fun initializeInterpreter() {
        val assetManager = context.assets
        val model = loadModelFile(assetManager, "mnist.tflite")
        val interpreter = Interpreter(model)

        val inputShape = interpreter.getInputTensor(0).shape()
        inputImageWidth = inputShape[1]
        inputImageHeight = inputShape[2]
        modelInputSize = FLOAT_TYPE_SIZE * inputImageWidth * inputImageHeight * PIXEL_SIZE

        this.interpreter = interpreter

        isInitialized = true
        Log.d(TAG, "Initialized TFLite interpreter.")
    }

    @Throws(IOException::class)
    private fun loadModelFile(assetManager: AssetManager, filename: String): ByteBuffer {
        val fileDescriptor = assetManager.openFd(filename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun classify(sourceBitmap: Bitmap): String {
        check(isInitialized) { "TF Lite Interpreter is not initialized yet." }



        val resizedImage = Bitmap.createScaledBitmap(
            sourceBitmap,
            sourceBitmap.width/6,
            sourceBitmap.height/6,
            true
        )

        val grayScale2dRows = convertBitmapTo2dGrayScaleList(resizedImage)

        val numRows  = findNumbersRows(grayScale2dRows)

        val bitmaps = ArrayList<Bitmap>()

        val bitmapsRows = splitBitmapRows(resizedImage,numRows)
        Log.d(TAG, "he${bitmapsRows[0].width}   ${bitmapsRows[0].height}")
        for(bitmapRow in bitmapsRows)
        {
            val grayScale2dColumns = convertBitmapTo2dGrayScaleList(bitmapRow)
            val numColumns  = findNumbersColumns(grayScale2dColumns)
            val bitmapsRowCropped = splitBitmapColumns(bitmapRow,numColumns)

            //For better centring one more time we set it in vertical

            for(almostBitmap in bitmapsRowCropped)
            {
                val grayScale2dRows = convertBitmapTo2dGrayScaleList(almostBitmap)

                val numRows  = findNumbersRows(grayScale2dRows)

                bitmaps.add(splitBitmapColumns(almostBitmap,numRows)[0])
            }
        }

        digitsList.clear()

        for(newBitmap in bitmaps)
        {
            //Log.d(TAG, "${newBitmap.width}   ${newBitmap.height}")
            val resizedImage = Bitmap.createScaledBitmap(
                newBitmap,
                inputImageWidth,
                inputImageHeight,
                true
            )

            val byteBuffer = convertBitmapToByteBuffer(resizedImage)
            val output = Array(1) { FloatArray(OUTPUT_CLASSES_COUNT) }

            interpreter?.run(byteBuffer, output)

            val result = output[0]
            val maxIndex = result.indices.maxByOrNull { result[it] } ?: -1

            digitsList.add(Digit(maxIndex,result[maxIndex],resizedImage))


        }

        var resultString = "Prediction results:\n"

        for(digit in digitsList)
        {
            resultString+= "${digit.resultPredicted} "
        }

        return resultString
    }

    fun classifyAsync(bitmap: Bitmap): Task<String> {
        val task = TaskCompletionSource<String>()
        executorService.execute {
            val result = classify(bitmap)
            task.setResult(result)
        }
        return task.task
    }



    fun close() {
        executorService.execute {
            interpreter?.close()

        }
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(modelInputSize)
        byteBuffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(inputImageWidth * inputImageHeight)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (pixelValue in pixels) {
            val r = (pixelValue shr 16 and 0xFF)
            val g = (pixelValue shr 8 and 0xFF)
            val b = (pixelValue and 0xFF)

            // conversion from rgb to grayscale and normalization 0 to 1
            val normalizedPixelValue = (r + g + b) / 3.0f / 255.0f
            byteBuffer.putFloat(normalizedPixelValue)
        }

        return byteBuffer
    }

    /*private fun segmentBitmap(array: Array<FloatArray>): List<Array<FloatArray>> {

        val arrayToReturn = arrayOf(arrayOf<float>);
        createBitmap()
        for (i in 0..array.size) {
            for (j in 0..array[0].size) {

            }
        }
        return
    }*/


    private fun convertBitmapTo2dGrayScaleList(bitmap: Bitmap): Array<FloatArray> {
        val arrayToReturn = Array(bitmap.height) { FloatArray(bitmap.width) };

        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (row in 0 until bitmap.height) {
            for (column in 0 until bitmap.width) {
                val pixelValue = pixels[row * bitmap.width + column]
                val r = (pixelValue shr 16 and 0xFF)
                val g = (pixelValue shr 8 and 0xFF)
                val b = (pixelValue and 0xFF)
                val normalizedPixelValue = (r + g + b) / 3.0f / 255.0f
                arrayToReturn[row][column] = normalizedPixelValue
            }
        }


        return arrayToReturn

    }

    // We have to remember that everything is in negative.
    // What means that digits are white and background is dark.

    // search columns where digit is beginning and where is ending
    private fun findNumbersColumns(array: Array<FloatArray>): ArrayList<Int> {
        var columns = ArrayList<Int>()
        var background = true
        for (column in array[0].indices) {
            for (row in array.indices) {

                if (background) {
                    if (array[row][column] > 0.1) {
                        if (column != 0)
                            columns.add(column - 1)
                        else
                            columns.add(column)
                        background = false
                        break
                    }

                } else {
                    if (array[row][column] > 0.1) {
                        break
                    }
                    if (row == array.size - 1) {
                        columns.add(column + 1)
                        background = true
                    }
                }


            }
            if ((column == array.size - 1) and (!background)) {
                columns.add(column + 1)
            }

        }
        return columns
    }

    // search rows where digit is beginning and where is ending
    private fun findNumbersRows(array: Array<FloatArray>): ArrayList<Int> {
        var rows = ArrayList<Int>()
        var background = true
        for (row in array.indices) {
            for (column in array[0].indices) {

                if (background) {
                    if (array[row][column] > 0.1) {
                        if (column != 0)
                            rows.add(row - 1)
                        else
                            rows.add(row)
                        background = false
                        break
                    }

                } else {
                    if (array[row][column] > 0.1) {
                        break
                    }
                    if (column == array[0].size - 1) {
                        rows.add(row + 1)
                        background = true
                    }
                }


            }
            if ((row == array[0].size - 1) and (!background)) {
                rows.add(row + 1)
            }

        }
        return rows
    }


    private fun splitBitmapColumns(bitmap: Bitmap, columns: ArrayList<Int>): ArrayList<Bitmap> {

        var bitmaps = ArrayList<Bitmap>(columns.size/2);
        for(i in 0 until columns.size/2)
        {
            var beginX = if(columns[i*2] - bitmap.height/4 < 0 ) 0 else columns[i*2] - bitmap.height/4
            var width = if(columns[i*2+1]+ bitmap.height/2 > bitmap.width) bitmap.width - beginX else columns[i*2+1]-columns[i*2]+ bitmap.height/2
            if(width == 0)
                break

            bitmaps.add(Bitmap.createBitmap(bitmap, beginX,0, width, bitmap.height))
        }

        return bitmaps;
    }

    private fun splitBitmapRows(bitmap: Bitmap, rows: ArrayList<Int>): ArrayList<Bitmap> {

        var bitmaps = ArrayList<Bitmap>(rows.size/2);
        for(i in 0 until rows.size/2)
        {
            var height =  if(rows[i*2+1]-rows[i*2]>bitmap.height) bitmap.width - rows[i*2] else rows[i*2+1]-rows[i*2]
            if(height == 0)
                break
            bitmaps.add(Bitmap.createBitmap(bitmap, 0,rows[i*2], bitmap.width,height))
        }

        return bitmaps;
    }
    fun getDigitList():ArrayList<Digit>
    {
        return digitsList
    }


    companion object {
        private const val TAG = "DigitClassifier"

        private const val FLOAT_TYPE_SIZE = 4
        private const val PIXEL_SIZE = 1
        private const val OUTPUT_CLASSES_COUNT = 10
    }
}
