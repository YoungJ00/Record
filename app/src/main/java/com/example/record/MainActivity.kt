package com.example.record

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import android.text.InputType
import android.util.Log
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

    companion object {
        private const val PERMISSION_REQUEST_CODE = 101
        private const val PICK_AUDIO_FILE_CODE = 102  // 오디오 파일 선택을 위한 새 요청 코드
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!checkPermissions()) {
            requestPermissions()
        } else {
            // 권한이 이미 승인되었다면, 필요한 기능을 실행합니다.
            setupUI()
        }
    }


    private fun checkPermissions(): Boolean {
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_MEDIA_AUDIO
        )
        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }


    private fun requestPermissions() {
        Log.d("Permissions", "Requesting permissions...")
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_MEDIA_AUDIO
            ),
            PERMISSION_REQUEST_CODE
        )
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d("Permissions", "PermissionsResult: requestCode=$requestCode, permissions=${permissions.joinToString()}, grantResults=${grantResults.joinToString()}")


        if (requestCode == PERMISSION_REQUEST_CODE) {
            val permissionsMap = permissions.zip(grantResults.toTypedArray()).toMap()

            var allPermissionsGranted = true
            for ((perm, grantResult) in permissionsMap) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false
                    // 사용자가 권한 요청을 거부했을 경우, 권한 필요성 설명
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, perm)) {
                        explainPermissionRationale(perm)
                    } else {
                        // 사용자가 권한 요청을 다시 보지 않기로 선택했을 때
                        Toast.makeText(this, "You need to enable permissions from settings", Toast.LENGTH_LONG).show()
                        // 설정으로 유도하는 등의 추가 조치를 고려할 수 있습니다.
                    }
                }
            }

            if (allPermissionsGranted) {
                setupUI()  // 모든 권한이 승인되었다면 UI 설정
            } else {
                Toast.makeText(this, "All permissions are required to use this app", Toast.LENGTH_LONG).show()
                // 필요하면 앱을 종료하거나, 권한이 없는 상태에서 할 수 있는 기능 제한
            }
        }
    }

    private fun explainPermissionRationale(permission: String) {
        val message = when (permission) {
            Manifest.permission.RECORD_AUDIO -> "Recording audio is necessary to record your voice along with the music."
            Manifest.permission.WRITE_EXTERNAL_STORAGE -> "Writing to storage is necessary to save the recordings."
            Manifest.permission.READ_EXTERNAL_STORAGE -> "Reading storage is necessary to pick audio files."
            else -> "This permission is necessary."
        }

        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(permission),
                    PERMISSION_REQUEST_CODE
                )
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(this, "Permission denied. Functionality limited.", Toast.LENGTH_SHORT).show()
            }
            .create()
            .show()
    }


    private fun setupUI() {
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



    private fun pickAudioFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "audio/*"
        }
        startActivityForResult(intent, PICK_AUDIO_FILE_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_AUDIO_FILE_CODE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                val documentFile = DocumentFile.fromSingleUri(this, uri)
                if (documentFile != null && documentFile.exists() && documentFile.isFile) {
                    try {
                        audioPlayer = MediaPlayer().apply {
                            setAudioAttributes(AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                            setDataSource(this@MainActivity, uri)
                            prepare()
                        }
                        Log.d("FileAccess", "File access granted for URI: $uri")
                    } catch (e: Exception) {
                        Log.e("FileAccess", "Failed to load selected song: ${e.message}")
                        Toast.makeText(this, "Failed to load selected song: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } else{
                    Log.e("FileAccess", "DocumentFile is null or does not exist.")
                }
            }
        }
    }



    private fun playAndRecord() {
        // 외부 저장소의 Music 디렉토리에 파일을 저장합니다.
        val musicDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        audioFilename = "${musicDir?.absolutePath}/${System.currentTimeMillis()}.m4a"
        audioRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(48000) // Sampling rate 설정
            setOutputFile(audioFilename)
            prepare()
            start()
            isRecording = true
        }
        audioPlayer?.start() // 음악 재생 시작
    }


    private fun stopAndSave() {
        audioPlayer?.stop()
        if (isRecording) {
            audioRecorder?.apply {
                stop()
                release()
                isRecording = false
                audioFilename?.let { filename ->
                    val recordedFile = File(filename)
                    promptForFileName(recordedFile) // Use audioFilename here
                }
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
                val newPath = File(recordedFile.parent, "$newName.m4a")
                try {
                    recordedFile.renameTo(newPath)
                    Toast.makeText(this, "File saved to Music directory: ${newPath.absolutePath}", Toast.LENGTH_LONG).show()
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