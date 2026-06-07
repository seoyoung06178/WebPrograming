package com.example.webprograming

import android.Manifest
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.webprograming.db.TravelDBHelper
import com.example.webprograming.model.TravelRecord
import com.example.webprograming.util.GpsUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private lateinit var progressBar: ProgressBar

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

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbarAddEdit)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        dbHelper = TravelDBHelper(this)
        initViews()

        recordId = intent.getLongExtra("record_id", -1)
        if (recordId != -1L) {
            toolbar.title = "기록 수정"
            loadRecordAsync()
        } else {
            toolbar.title = "새 기록"
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
        progressBar = findViewById(R.id.progressBarAddEdit)
    }

    private fun setupListeners() {
        etDate.setOnClickListener { showDatePicker() }
        btnSelectPhoto.setOnClickListener { showPhotoDialog() }
        btnSave.setOnClickListener { saveRecordAsync() }
    }

    private fun loadRecordAsync() {
        lifecycleScope.launch {
            progressBar.visibility = View.VISIBLE

            val record = withContext(Dispatchers.IO) {
                dbHelper.getRecordById(recordId)
            }

            progressBar.visibility = View.GONE

            if (record == null) return@launch
            etTitle.setText(record.title)
            etDate.setText(record.visitDate)
            etMemo.setText(record.memo)
            currentPhotoPath = record.photoPath
            latitude = record.latitude
            longitude = record.longitude

            if (currentPhotoPath.isNotEmpty() && File(currentPhotoPath).exists()) {
                loadImageAsync(currentPhotoPath)
            }
        }
    }

    private fun loadImageAsync(path: String) {
        lifecycleScope.launch {
            progressBar.visibility = View.VISIBLE

            val bitmap = withContext(Dispatchers.IO) {
                decodeSampledBitmap(path, 800, 600)
            }

            progressBar.visibility = View.GONE
            if (bitmap != null) {
                ivPhoto.setImageBitmap(bitmap)
            }
        }
    }

    private fun decodeSampledBitmap(path: String, reqWidth: Int, reqHeight: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, options)

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false
            BitmapFactory.decodeFile(path, options)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int
    ): Int {
        val (height, width) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
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
        @Suppress("DEPRECATION")
        startActivityForResult(intent, REQUEST_CAMERA)
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        @Suppress("DEPRECATION")
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

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return

        when (requestCode) {
            REQUEST_CAMERA -> {
                loadImageAsync(currentPhotoPath)
                extractGpsAsync()
            }
            REQUEST_GALLERY -> {
                data?.data?.let { uri ->
                    copyUriToFileAsync(uri)
                }
            }
        }
    }

    private fun copyUriToFileAsync(uri: Uri) {
        lifecycleScope.launch {
            progressBar.visibility = View.VISIBLE

            withContext(Dispatchers.IO) {
                try {
                    val inputStream = contentResolver.openInputStream(uri) ?: return@withContext
                    val file = createImageFile() ?: return@withContext
                    file.outputStream().use { output ->
                        inputStream.copyTo(output)
                    }
                    inputStream.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }

            progressBar.visibility = View.GONE
            loadImageAsync(currentPhotoPath)
            extractGpsAsync()
        }
    }

    private fun extractGpsAsync() {
        lifecycleScope.launch {
            val gps = withContext(Dispatchers.IO) {
                GpsUtil.extractGpsFromPhoto(currentPhotoPath)
            }
            if (gps != null) {
                latitude = gps.latitude
                longitude = gps.longitude
                Toast.makeText(
                    this@AddEditActivity,
                    "GPS 위치 추출: ${String.format("%.4f", gps.latitude)}, ${String.format("%.4f", gps.longitude)}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun saveRecordAsync() {
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

        lifecycleScope.launch {
            progressBar.visibility = View.VISIBLE
            btnSave.isEnabled = false

            withContext(Dispatchers.IO) {
                if (recordId != -1L) {
                    dbHelper.updateRecord(record)
                } else {
                    dbHelper.insertRecord(record)
                }
            }

            progressBar.visibility = View.GONE
            val msg = if (recordId != -1L) "수정되었습니다" else "저장되었습니다"
            Toast.makeText(this@AddEditActivity, msg, Toast.LENGTH_SHORT).show()
            finish()
        }
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
