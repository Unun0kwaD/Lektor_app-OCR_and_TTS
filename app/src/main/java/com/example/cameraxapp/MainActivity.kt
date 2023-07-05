package com.example.cameraxapp

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageCapture
import androidx.core.content.ContextCompat
import com.example.cameraxapp.databinding.ActivityMainBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.core.Preview
import androidx.camera.core.CameraSelector
import android.util.Log
import android.view.View
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCaptureException
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.text.SimpleDateFormat
import java.util.Locale


//typealias LumaListener = (luma: Double) -> Unit

@ExperimentalGetImage class MainActivity : AppCompatActivity(),TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var textFromScreen : String = ""
    var textForSpeech : String = ""
    lateinit var viewBinding: ActivityMainBinding

    private var imageCapture: ImageCapture? = null


    private lateinit var cameraExecutor: ExecutorService


    private val activityResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions())
        { permissions ->
            // Handle Permission granted/rejected
            var permissionGranted = true
            permissions.entries.forEach {
                if (it.key in REQUIRED_PERMISSIONS && !it.value)
                    permissionGranted = false
            }
            if (!permissionGranted) {
                Toast.makeText(baseContext,
                    "Permission request denied",
                    Toast.LENGTH_SHORT).show()
            } else {
                startCamera()
            }
        }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            var result = tts!!.setLanguage(Locale.US)

            for(locale in Locale.getAvailableLocales()) {
                //Log.d("TTS", locale.toString())
                if (locale.language == "pl") {
                    result = tts!!.setLanguage(locale)
                    break
                }
            }
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS","The Language not supported!")
            } else {
                Log.d("TTS", "Language supported.")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions()
        }
        tts = TextToSpeech(this, this)

        // Set up the listeners for take photo and video capture buttons
        viewBinding.imageCaptureButton.setOnClickListener { takePhoto() }
        viewBinding.ttsButton.setOnClickListener { tts() }
        viewBinding.ocrButton.setOnClickListener { saveOcr() }
        //viewBinding.textView.visibility = View.INVISIBLE

        cameraExecutor = Executors.newSingleThreadExecutor()


    }

    private fun saveOcr() {
        textForSpeech = textFromScreen

    }

    private fun tts() {
        tts!!.speak(textForSpeech, TextToSpeech.QUEUE_FLUSH, null,"")
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onPause() {
        super.onPause()
        if (tts != null) {
            tts!!.stop()
            tts!!.shutdown()
        }
        cameraExecutor.shutdown()
    }

    override fun onResume() {
        super.onResume()
        cameraExecutor = Executors.newSingleThreadExecutor()

    }
    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time stamped name and MediaStore entry.
        val name = SimpleDateFormat(MainActivity.FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
        }

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
            .build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(MainActivity.TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults){
                    val msg = "Photo capture succeeded: ${output.savedUri}"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(MainActivity.TAG, msg)

                    val image = InputImage.fromFilePath(baseContext,output.savedUri!!)
                    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    val result = recognizer.process(image)
                        .addOnSuccessListener { visionText ->
                            // Task completed successfully
                            textForSpeech=visionText.text
                            viewBinding.textView.text = visionText.text
                            tts()

                            /*
                            for (block in visionText.textBlocks) {
                                val boundingBox = block.boundingBox
                                val cornerPoints = block.cornerPoints
                                // println(text)\
                                //display text on the preview
                            }
                             */

                        }
                        .addOnFailureListener { e ->
                            println(e)
                        }

                }
            }
        )
    }


    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .build()

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
              //  .setTargetResolution(Size(1920, 1080))
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, TextAnalyzer { luna  ->
                        Log.d(TAG, "TEXT: $luna")
                        textFromScreen=luna
                       // viewBinding.textView.visibility = View.VISIBLE
                        viewBinding.textView.text = luna
                    })
                }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, imageAnalyzer)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        if (tts != null) {
            tts!!.stop()
            tts!!.shutdown()
        }
        cameraExecutor.shutdown()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "Lektor"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private val REQUIRED_PERMISSIONS =
            mutableListOf (
                Manifest.permission.CAMERA
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }

    fun onClickShare(view: View) {
        val sendIntent: Intent = Intent(Intent.ACTION_SEND).apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, textForSpeech)
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, null)
        val share = startActivity(shareIntent)

        if (share != null) {
            val text: CharSequence = "Udostępnianie tekstu"
            val duration = Snackbar.LENGTH_SHORT
            val contextView = findViewById<View>(R.id.coordinator)
            val snackbar = Snackbar.make(contextView, text, duration)


            snackbar.setAction("Wróć") {
                val toast = Toast.makeText(this, "Wróć", Toast.LENGTH_SHORT)
                toast.show()
            }
            snackbar.show()
        }
    }
}
