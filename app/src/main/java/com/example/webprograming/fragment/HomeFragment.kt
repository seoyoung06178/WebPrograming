package com.example.webprograming.fragment

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.webprograming.AddEditActivity
import com.example.webprograming.R
import com.example.webprograming.adapter.TravelAdapter
import com.example.webprograming.db.TravelDBHelper
import com.example.webprograming.model.TravelRecord
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmpty: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: TravelAdapter
    private lateinit var dbHelper: TravelDBHelper
    private var selectedRecord: TravelRecord? = null
    private var currentSortOrder = "visit_date DESC"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbHelper = TravelDBHelper(requireContext())
        recyclerView = view.findViewById(R.id.recyclerView)
        tvEmpty = view.findViewById(R.id.tvEmpty)
        progressBar = view.findViewById(R.id.progressBar)
        val fabAdd: FloatingActionButton = view.findViewById(R.id.fabAdd)

        setupRecyclerView()
        registerForContextMenu(recyclerView)

        fabAdd.setOnClickListener {
            val intent = Intent(requireContext(), AddEditActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        loadRecordsAsync()
    }

    private fun setupRecyclerView() {
        adapter = TravelAdapter(
            mutableListOf(),
            onItemClick = { record -> showDetail(record) },
            onItemLongClick = { record, view ->
                selectedRecord = record
                view.showContextMenu()
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    private fun loadRecordsAsync() {
        viewLifecycleOwner.lifecycleScope.launch {
            progressBar.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            tvEmpty.visibility = View.GONE

            val records = withContext(Dispatchers.IO) {
                dbHelper.getAllRecords(currentSortOrder)
            }

            progressBar.visibility = View.GONE
            adapter.updateRecords(records)

            if (records.isEmpty()) {
                tvEmpty.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                tvEmpty.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            }
        }
    }

    private fun showDetail(record: TravelRecord) {
        val detailFragment = DetailFragment.newInstance(record.id)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, detailFragment)
            .addToBackStack(null)
            .commit()
    }

    // 옵션 메뉴
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.options_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_sort_date -> {
                currentSortOrder = "visit_date DESC"
                loadRecordsAsync()
                Toast.makeText(requireContext(), "날짜순 정렬", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.menu_sort_name -> {
                currentSortOrder = "title ASC"
                loadRecordsAsync()
                Toast.makeText(requireContext(), "이름순 정렬", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.menu_delete_all -> {
                showDeleteAllDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // 컨텍스트 메뉴
    override fun onCreateContextMenu(
        menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)
        requireActivity().menuInflater.inflate(R.menu.context_menu, menu)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val record = selectedRecord ?: return super.onContextItemSelected(item)
        return when (item.itemId) {
            R.id.context_edit -> {
                val intent = Intent(requireContext(), AddEditActivity::class.java)
                intent.putExtra("record_id", record.id)
                startActivity(intent)
                true
            }
            R.id.context_delete -> {
                showDeleteDialog(record)
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }

    private fun showDeleteDialog(record: TravelRecord) {
        AlertDialog.Builder(requireContext())
            .setTitle("삭제 확인")
            .setMessage("'${record.title}' 기록을 삭제하시겠습니까?")
            .setPositiveButton("삭제") { _, _ ->
                deleteRecordAsync(record.id)
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showDeleteAllDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("전체 삭제")
            .setMessage("모든 여행 기록을 삭제하시겠습니까?")
            .setPositiveButton("삭제") { _, _ ->
                deleteAllRecordsAsync()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun deleteRecordAsync(id: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                dbHelper.deleteRecord(id)
            }
            loadRecordsAsync()
            Toast.makeText(requireContext(), "삭제되었습니다", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteAllRecordsAsync() {
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                dbHelper.deleteAllRecords()
            }
            loadRecordsAsync()
            Toast.makeText(requireContext(), "모두 삭제되었습니다", Toast.LENGTH_SHORT).show()
        }
    }
}
