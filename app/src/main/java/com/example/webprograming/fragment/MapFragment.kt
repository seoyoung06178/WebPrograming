package com.example.webprograming.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.webprograming.R
import com.example.webprograming.db.TravelDBHelper
import com.example.webprograming.model.TravelRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class MapFragment : Fragment() {

    private lateinit var mapView: MapView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvMapEmpty: TextView
    private lateinit var dbHelper: TravelDBHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Configuration.getInstance().userAgentValue = requireContext().packageName
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbHelper = TravelDBHelper(requireContext())
        mapView = view.findViewById(R.id.mapView)
        progressBar = view.findViewById(R.id.progressBarMap)
        tvMapEmpty = view.findViewById(R.id.tvMapEmpty)

        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)

        val mapController = mapView.controller
        mapController.setZoom(7.0)
        mapController.setCenter(GeoPoint(36.5, 127.0))

        loadMarkersAsync()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        loadMarkersAsync()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    private fun loadMarkersAsync() {
        viewLifecycleOwner.lifecycleScope.launch {
            progressBar.visibility = View.VISIBLE

            val records = withContext(Dispatchers.IO) {
                dbHelper.getAllRecords()
            }

            mapView.overlays.clear()

            val geoRecords = records.filter { it.latitude != 0.0 || it.longitude != 0.0 }

            if (geoRecords.isEmpty()) {
                tvMapEmpty.visibility = View.VISIBLE
            } else {
                tvMapEmpty.visibility = View.GONE
                for (record in geoRecords) {
                    addMarker(record)
                }
                if (geoRecords.size == 1) {
                    val r = geoRecords.first()
                    mapView.controller.setCenter(GeoPoint(r.latitude, r.longitude))
                    mapView.controller.setZoom(12.0)
                }
            }

            mapView.invalidate()
            progressBar.visibility = View.GONE
        }
    }

    private fun addMarker(record: TravelRecord) {
        try {
            val marker = Marker(mapView)
            marker.position = GeoPoint(record.latitude, record.longitude)
            marker.title = record.title
            marker.snippet = record.visitDate
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            mapView.overlays.add(marker)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
