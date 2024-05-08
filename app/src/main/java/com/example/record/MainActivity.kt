package com.example.record
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import java.io.File

class MainActivity : AppCompatActivity() {
    private var audioPlayer: MediaPlayer? = null
    private var audioRecorder: MediaRecorder? = null
    private var isRecording = false
    private var audioFilename: String? = null // Declare audioFilename as a member variable

    private val REQUEST_CODE_PICK_AUDIO = 101
    private val REQUEST_CODE_PERMISSIONS = 102
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupPermissions()

        findViewById<Button>(R.id.select_button).setOnClickListener {
            pickAudioFile()
        }

        findViewById<Button>(R.id.play_record_button).setOnClickListener {
            playAndRecord()
        }

        findViewById<Button>(R.id.stop_save_button).setOnClickListener {
            stopAndSave()
        }
    }

    private fun setupPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        for (permission in REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission)
            }
        }
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), REQUEST_CODE_PERMISSIONS)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.isNotEmpty()) {
                for (result in grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "Permission required for app functionality", Toast.LENGTH_SHORT).show()
                        finish()
                        return
                    }
                }
            }
        }
    }

    private fun pickAudioFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "audio/wav" // .wav 파일만 선택 가능하도록 타입 변경
        }
        startActivityForResult(intent, REQUEST_CODE_PICK_AUDIO)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PICK_AUDIO && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                val documentFile = DocumentFile.fromSingleUri(this, uri)
                if (documentFile != null && documentFile.exists() && documentFile.isFile) {
                    try {
                        audioPlayer = MediaPlayer().apply {
                            setDataSource(this@MainActivity, uri)
                            setAudioAttributes(AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                            prepare()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this, "Failed to load selected song: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }


    private fun playAndRecord() {
        audioFilename = "${externalCacheDir?.absolutePath}/temporaryRecording.m4a"
        audioPlayer?.let { player ->
            audioRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                audioFilename?.let { setOutputFile(it) } // Use audioFilename here
                try {
                    prepare()
                    start()
                    player.start()
                    isRecording = true
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Failed to start recording: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun stopAndSave() {
        audioPlayer?.stop()
        if (isRecording) {
            audioRecorder?.apply {
                stop()
                release()
                isRecording = false
                audioFilename?.let { promptForFileName(File(it)) } // Use audioFilename here
            }
        }
    }

    private fun promptForFileName(recordedFile: File) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Save Recording")
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setView(input)
        builder.setPositiveButton("Save") { dialog, _ ->
            val newName = input.text.toString().trim()
            if (newName.isNotEmpty()) {
                val parentDirectory = recordedFile.parentFile
                val newPath = File(parentDirectory, "$newName.m4a")
                try {
                    recordedFile.renameTo(newPath)
                } catch (e: Exception) {
                    Toast.makeText(this, "Failed to save recording: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please enter a valid filename", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }
        builder.show()
    }
}


