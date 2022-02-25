package com.example.digitsrecognizer

import android.Manifest
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.MotionEvent
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.divyanshu.draw.widget.DrawView
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener


import java.io.IOException


const val PREDICTED_DIGIT = "EXTRA_PREDICTED_DIGIT"

class MainActivity : AppCompatActivity() {


    private val RGB_MASK: Int = 0x00FFFFFF

    private val CAMERA_INTENT = 1
    private val GALLERY_INTENT = 2

    private var drawView: DrawView? = null
    private var clearButton: Button? = null
    private var importButton: Button? = null
    private var detailsButton: Button? = null
    private var predictedTextView: TextView? = null
    private var digitClassifier = DigitClassifier(this)
    private var digitList = ArrayList<Digit>()

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawView = findViewById(R.id.draw_view)
        drawView?.setStrokeWidth(40.0f)
        drawView?.setColor(Color.WHITE)
        drawView?.setBackgroundColor(Color.BLACK)


        clearButton = findViewById(R.id.clear_button)
        importButton = findViewById(R.id.import_button)
        detailsButton = findViewById(R.id.details_button)

        predictedTextView = findViewById(R.id.predicted_text)

        clearButton?.setOnClickListener {
            drawView?.clearCanvas()
            predictedTextView?.text = getString(R.string.prediction_text_placeholder)
        }
        detailsButton?.setOnClickListener {
            digitList = digitClassifier.getDigitList()
            val intent = Intent(this, DetailsActivity::class.java)
            val extras = Bundle()
            extras.putParcelableArrayList(PREDICTED_DIGIT, digitList)
            intent.putExtras(extras)
            startActivity(intent)
        }

        importButton?.setOnClickListener {
            val pictureDialog = AlertDialog.Builder(this)
            pictureDialog.setTitle("SELECT ACTION")
            val pictureItems = arrayOf("Select photo from gallery", "Take a Photo")
            pictureDialog.setItems(pictureItems) { _, which ->
                when (which) {
                    0 -> choosePhoto()
                    1 -> chooseCamera()
                }
            }
            pictureDialog.show()
        }


        drawView?.setOnTouchListener { _, event ->
            drawView?.onTouchEvent(event)

            // after end touch event, run classification
            if (event.action == MotionEvent.ACTION_UP) {
                classifyDrawing()
            }

            true
        }

        digitClassifier
            .initialize()
            .addOnFailureListener { e -> Log.e(TAG, "Error to setting up digit classifier.", e) }
    }


    override fun onDestroy() {
        digitClassifier.close()
        super.onDestroy()
    }

    private fun classifyDrawing() {
        val bitmap = drawView?.getBitmap()

        if ((bitmap != null) && (digitClassifier.isInitialized)) {
            digitClassifier
                .classifyAsync(bitmap)
                .addOnSuccessListener { resultText -> predictedTextView?.text = resultText }
                .addOnFailureListener { e ->
                    predictedTextView?.text = getString(
                        R.string.classification_error_message,
                        e.localizedMessage
                    )
                    Log.e(TAG, "Error classifying drawing.", e)
                }


        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (data != null) {
                when (requestCode) {
                    CAMERA_INTENT -> {
                        val bitmap: Bitmap = invert(data.extras?.get("data") as Bitmap)
                        if ((bitmap != null) && (digitClassifier.isInitialized)) {
                            digitClassifier
                                .classifyAsync(bitmap)
                                .addOnSuccessListener { resultText ->
                                    predictedTextView?.text = resultText
                                }
                                .addOnFailureListener { e ->
                                    predictedTextView?.text = getString(
                                        R.string.classification_error_message,
                                        e.localizedMessage
                                    )
                                    Log.e(TAG, "Error classifying drawing.", e)
                                }


                        }
                    }
                    GALLERY_INTENT -> {
                        val contentUri = data.data
                        try {
                            if (Build.VERSION.SDK_INT >= 29) {

                                val bitmap = invert(MediaStore.Images.Media.getBitmap(
                                    this.contentResolver,
                                    contentUri
                                ))
                                if ((bitmap != null) && (digitClassifier.isInitialized)) {
                                    digitClassifier
                                        .classifyAsync(bitmap)
                                        .addOnSuccessListener { resultText ->
                                            predictedTextView?.text = resultText
                                        }
                                        .addOnFailureListener { e ->
                                            predictedTextView?.text = getString(
                                                R.string.classification_error_message,
                                                e.localizedMessage
                                            )
                                            Log.e(TAG, "Error classifying drawing.", e)
                                        }


                                }

                            }
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    private fun chooseCamera() {
        Dexter.withContext(this).withPermission(
            Manifest.permission.CAMERA
        ).withListener(object : PermissionListener {
            override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(intent, CAMERA_INTENT)
            }

            override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
            }

            override fun onPermissionRationaleShouldBeShown(
                p0: PermissionRequest?,
                p1: PermissionToken?
            ) {
                showRationaleDialog()
            }

        }).onSameThread().check()
    }

    private fun choosePhoto() {
        Dexter.withContext(this).withPermission(
            READ_EXTERNAL_STORAGE,
        ).withListener(object : PermissionListener {

            override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                val intent = Intent(Intent.ACTION_PICK)
                intent.type = "image/*"
                startActivityForResult(intent, GALLERY_INTENT)
            }

            override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
            }

            override fun onPermissionRationaleShouldBeShown(
                p0: PermissionRequest?,
                p1: PermissionToken?
            ) {
                showRationaleDialog()
            }

        }).onSameThread().check()
    }

    private fun showRationaleDialog() {
        AlertDialog.Builder(this)
            .setMessage("Permissions required for this functionality")
            .setPositiveButton("Ask me") { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", this.packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }.show()
    }


    fun invert(original: Bitmap): Bitmap {
        val inversion = original.copy(Bitmap.Config.ARGB_8888, true)

        // Get info about Bitmap
        val width = inversion.width
        val height = inversion.height
        val pixels = width * height

        // Get original pixels
        val pixel = IntArray(pixels)
        inversion.getPixels(pixel, 0, width, 0, 0, width, height)

        for (i in 0 until pixels)
            pixel[i] = pixel[i] xor RGB_MASK
        inversion.setPixels(pixel, 0, width, 0, 0, width, height)

        return inversion
    }

    //for debuging
    companion object {
        private const val TAG = "MainActivity"
    }
}
