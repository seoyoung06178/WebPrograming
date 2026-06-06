package com.example.webprograming.fragment

import android.app.AlertDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.webprograming.AddEditActivity
import com.example.webprograming.R
import com.example.webprograming.db.TravelDBHelper
import com.example.webprograming.model.TravelRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class DetailFragment : Fragment() {

    private var recordId: Long = -1
    private lateinit var dbHelper: TravelDBHelper

    companion object {
        private const val ARG_RECORD_ID = "record_id"

        fun newInstance(recordId: Long): DetailFragment {
            val fragment = DetailFragment()
            val args = Bundle()
            args.putLong(ARG_RECORD_ID, recordId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        recordId = arguments?.getLong(ARG_RECORD_ID, -1) ?: -1
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dbHelper = TravelDBHelper(requireContext())
        loadDetailAsync(view)
    }

    override fun onResume() {
        super.onResume()
        view?.let { loadDetailAsync(it) }
    }

    private fun loadDetailAsync(view: View) {
        val ivPhoto: ImageView = view.findViewById(R.id.ivDetailPhoto)
        val tvTitle: TextView = view.findViewById(R.id.tvDetailTitle)
        val tvDate: TextView = view.findViewById(R.id.tvDetailDate)
        val tvMemo: TextView = view.findViewById(R.id.tvDetailMemo)
        val tvLocation: TextView = view.findViewById(R.id.tvDetailLocation)
        val layoutLocation: LinearLayout = view.findViewById(R.id.layoutLocation)
        val btnEdit: Button = view.findViewById(R.id.btnEdit)
        val btnDelete: Button = view.findViewById(R.id.btnDelete)
        val progressBar: ProgressBar = view.findViewById(R.id.progressBarDetail)

        viewLifecycleOwner.lifecycleScope.launch {
            progressBar.visibility = View.VISIBLE

            val record = withContext(Dispatchers.IO) {
                dbHelper.getRecordById(recordId)
            }

            progressBar.visibility = View.GONE

            if (record == null) {
                Toast.makeText(requireContext(), "기록을 찾을 수 없습니다", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
                return@launch
            }

            tvTitle.text = record.title
            tvDate.text = record.visitDate
            tvMemo.text = if (record.memo.isNotEmpty()) record.memo else "메모가 없습니다."

            if (record.latitude != 0.0 && record.longitude != 0.0) {
                tvLocation.text = "${String.format("%.4f", record.latitude)}, ${String.format("%.4f", record.longitude)}"
                layoutLocation.visibility = View.VISIBLE
            } else {
                layoutLocation.visibility = View.GONE
            }

            if (record.photoPath.isNotEmpty() && File(record.photoPath).exists()) {
                val bitmap = withContext(Dispatchers.IO) {
                    try {
                        val options = BitmapFactory.Options().apply { inSampleSize = 2 }
                        BitmapFactory.decodeFile(record.photoPath, options)
                    } catch (e: Exception) { null }
                }
                if (bitmap != null) {
                    ivPhoto.setImageBitmap(bitmap)
                }
            } else {
                ivPhoto.setImageResource(R.drawable.ic_placeholder)
            }

            btnEdit.setOnClickListener {
                val intent = Intent(requireContext(), AddEditActivity::class.java)
                intent.putExtra("record_id", record.id)
                startActivity(intent)
            }

            btnDelete.setOnClickListener {
                showDeleteDialog(record)
            }
        }
    }

    private fun showDeleteDialog(record: TravelRecord) {
        AlertDialog.Builder(requireContext())
            .setTitle("삭제 확인")
            .setMessage("'${record.title}' 기록을 삭제하시겠습니까?")
            .setPositiveButton("삭제") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        dbHelper.deleteRecord(record.id)
                    }
                    Toast.makeText(requireContext(), "삭제되었습니다", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }
}
