package com.example.record

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private val REQUEST_PERMISSION_CODE = 100
    private var isRecording = false
    private lateinit var selectButton: Button
    private lateinit var playRecordButton: Button
    private lateinit var stopSaveButton: Button
    private lateinit var nameTextView: TextView
    private var outputFile: File? = null

    private val recordAudioPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                setupAudioRecording()
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        selectButton = findViewById(R.id.select_button)
        playRecordButton = findViewById(R.id.play_record_button)
        stopSaveButton = findViewById(R.id.stop_save_button)
        nameTextView = findViewById(R.id.name)

        selectButton.setOnClickListener {
            openFileSelection()
        }

        playRecordButton.setOnClickListener {
            if (!isRecording) {
                startRecording()
            } else {
                stopRecording()
            }
        }

        stopSaveButton.setOnClickListener {
            stopRecording()
            outputFile?.let { saveRecording(it) }
        }

        // Permission check and request
        if (!checkPermissionFromDevice())
            requestPermission()
        else
            setupAudioRecording()
    }

    private fun openFileSelection() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "audio/*"
        }
        startActivityForResult(intent, REQUEST_FILE_SELECTION)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_FILE_SELECTION && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                val path = FileUtils.getPath(this, uri)
                nameTextView.text = path
                outputFile = File(path)
            }
        }
    }

    private fun startRecording() {
        setupAudioRecording()
        isRecording = true
    }

    private fun stopRecording() {
        isRecording = false
    }

    private fun saveRecording(file: File) {
        // You can implement your saving logic here
        Toast.makeText(this, "Recording saved: ${file.absolutePath}", Toast.LENGTH_SHORT).show()
    }

    private fun setupAudioRecording() {
        val bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        val audioRecorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        val data = ByteArray(bufferSize)

        Thread {
            val wavFile = File(Environment.getExternalStorageDirectory().absolutePath, "recorded_audio.wav")

            val fileOutputStream = FileOutputStream(wavFile)

            audioRecorder.startRecording()

            while (isRecording) {
                val numBytesRead = audioRecorder.read(data, 0, bufferSize)
                if (numBytesRead != AudioRecord.ERROR_INVALID_OPERATION) {
                    try {
                        fileOutputStream.write(data)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }

            audioRecorder.stop()
            audioRecorder.release()
            fileOutputStream.close()
        }.start()
    }

    private fun checkPermissionFromDevice(): Boolean {
        val writeExternalStorage = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        val recordAudio = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        )
        return writeExternalStorage == PackageManager.PERMISSION_GRANTED &&
                recordAudio == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    companion object {
        private const val SAMPLE_RATE = 44100
        private const val REQUEST_FILE_SELECTION = 101
    }
}
