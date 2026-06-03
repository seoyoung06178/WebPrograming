package com.example.webprograming.fragment

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.webprograming.AddEditActivity
import com.example.webprograming.R
import com.example.webprograming.db.TravelDBHelper
import com.example.webprograming.model.TravelRecord
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

        val ivPhoto: ImageView = view.findViewById(R.id.ivDetailPhoto)
        val tvTitle: TextView = view.findViewById(R.id.tvDetailTitle)
        val tvDate: TextView = view.findViewById(R.id.tvDetailDate)
        val tvMemo: TextView = view.findViewById(R.id.tvDetailMemo)
        val tvLocation: TextView = view.findViewById(R.id.tvDetailLocation)
        val btnEdit: Button = view.findViewById(R.id.btnEdit)

        val record = dbHelper.getRecordById(recordId) ?: return

        tvTitle.text = record.title
        tvDate.text = record.visitDate
        tvMemo.text = record.memo

        if (record.latitude != 0.0 && record.longitude != 0.0) {
            tvLocation.text = "위치: ${String.format("%.4f", record.latitude)}, ${String.format("%.4f", record.longitude)}"
            tvLocation.visibility = View.VISIBLE
        } else {
            tvLocation.visibility = View.GONE
        }

        if (record.photoPath.isNotEmpty() && File(record.photoPath).exists()) {
            val bitmap = BitmapFactory.decodeFile(record.photoPath)
            ivPhoto.setImageBitmap(bitmap)
        } else {
            ivPhoto.setImageResource(R.drawable.ic_placeholder)
        }

        btnEdit.setOnClickListener {
            val intent = Intent(requireContext(), AddEditActivity::class.java)
            intent.putExtra("record_id", record.id)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        view?.let { onViewCreated(it, null) }
    }
}
