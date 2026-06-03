package com.example.webprograming

import android.Manifest
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.webprograming.db.TravelDBHelper
import com.example.webprograming.model.TravelRecord
import com.example.webprograming.util.GpsUtil
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class AddEditActivity : AppCompatActivity() {

    private lateinit var dbHelper: TravelDBHelper
    private lateinit var etTitle: EditText
    private lateinit var etDate: EditText
    private lateinit var etMemo: EditText
    private lateinit var ivPhoto: ImageView
    private lateinit var btnSelectPhoto: Button
    private lateinit var btnSave: Button

    private var recordId: Long = -1
    private var currentPhotoPath: String = ""
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0

    companion object {
        private const val REQUEST_CAMERA = 100
        private const val REQUEST_GALLERY = 101
        private const val PERMISSION_REQUEST_CODE = 200
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        dbHelper = TravelDBHelper(this)
        initViews()

        recordId = intent.getLongExtra("record_id", -1)
        if (recordId != -1L) {
            supportActionBar?.title = "기록 수정"
            loadRecord()
        } else {
            supportActionBar?.title = "새 기록"
        }

        setupListeners()
    }

    private fun initViews() {
        etTitle = findViewById(R.id.etTitle)
        etDate = findViewById(R.id.etDate)
        etMemo = findViewById(R.id.etMemo)
        ivPhoto = findViewById(R.id.ivPhoto)
        btnSelectPhoto = findViewById(R.id.btnSelectPhoto)
        btnSave = findViewById(R.id.btnSave)
    }

    private fun setupListeners() {
        etDate.setOnClickListener { showDatePicker() }
        btnSelectPhoto.setOnClickListener { showPhotoDialog() }
        btnSave.setOnClickListener { saveRecord() }
    }

    private fun loadRecord() {
        val record = dbHelper.getRecordById(recordId) ?: return
        etTitle.setText(record.title)
        etDate.setText(record.visitDate)
        etMemo.setText(record.memo)
        currentPhotoPath = record.photoPath
        latitude = record.latitude
        longitude = record.longitude

        if (currentPhotoPath.isNotEmpty() && File(currentPhotoPath).exists()) {
            val options = BitmapFactory.Options().apply { inSampleSize = 2 }
            val bitmap = BitmapFactory.decodeFile(currentPhotoPath, options)
            ivPhoto.setImageBitmap(bitmap)
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val date = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                etDate.setText(date)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showPhotoDialog() {
        val options = arrayOf("카메라로 촬영", "갤러리에서 선택")
        AlertDialog.Builder(this)
            .setTitle("사진 추가")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermissionAndLaunch()
                    1 -> openGallery()
                }
            }
            .show()
    }

    private fun checkCameraPermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA), PERMISSION_REQUEST_CODE
            )
        } else {
            openCamera()
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val photoFile = createImageFile() ?: return
        val photoUri = FileProvider.getUriForFile(
            this, "${packageName}.fileprovider", photoFile
        )
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        startActivityForResult(intent, REQUEST_CAMERA)
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_GALLERY)
    }

    private fun createImageFile(): File? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            File.createTempFile("TRAVEL_${timeStamp}_", ".jpg", storageDir).also {
                currentPhotoPath = it.absolutePath
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return

        when (requestCode) {
            REQUEST_CAMERA -> {
                loadPhotoIntoView()
                extractGps()
            }
            REQUEST_GALLERY -> {
                data?.data?.let { uri ->
                    copyUriToFile(uri)
                    loadPhotoIntoView()
                    extractGps()
                }
            }
        }
    }

    private fun copyUriToFile(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri) ?: return
            val file = createImageFile() ?: return
            file.outputStream().use { output ->
                inputStream.copyTo(output)
            }
            inputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun loadPhotoIntoView() {
        if (currentPhotoPath.isNotEmpty() && File(currentPhotoPath).exists()) {
            val options = BitmapFactory.Options().apply { inSampleSize = 2 }
            val bitmap = BitmapFactory.decodeFile(currentPhotoPath, options)
            ivPhoto.setImageBitmap(bitmap)
        }
    }

    private fun extractGps() {
        val gps = GpsUtil.extractGpsFromPhoto(currentPhotoPath)
        if (gps != null) {
            latitude = gps.latitude
            longitude = gps.longitude
            Toast.makeText(this, "GPS 위치 추출 완료", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveRecord() {
        val title = etTitle.text.toString().trim()
        val date = etDate.text.toString().trim()
        val memo = etMemo.text.toString().trim()

        if (title.isEmpty()) {
            etTitle.error = "여행지명을 입력해주세요"
            return
        }
        if (date.isEmpty()) {
            etDate.error = "날짜를 선택해주세요"
            return
        }

        val record = TravelRecord(
            id = if (recordId != -1L) recordId else 0,
            title = title,
            visitDate = date,
            memo = memo,
            photoPath = currentPhotoPath,
            latitude = latitude,
            longitude = longitude
        )

        if (recordId != -1L) {
            dbHelper.updateRecord(record)
            Toast.makeText(this, "수정되었습니다", Toast.LENGTH_SHORT).show()
        } else {
            dbHelper.insertRecord(record)
            Toast.makeText(this, "저장되었습니다", Toast.LENGTH_SHORT).show()
        }

        finish()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                Toast.makeText(this, "카메라 권한이 필요합니다", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
