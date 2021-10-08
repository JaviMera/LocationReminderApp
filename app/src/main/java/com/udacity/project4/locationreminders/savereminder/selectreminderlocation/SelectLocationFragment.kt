package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.*

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var map: GoogleMap
    private val _locationClient: FusedLocationProviderClient by lazy { LocationServices.getFusedLocationProviderClient(requireContext()) }
    private lateinit var _marker: Marker

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        binding.saveReminderActivityButtonSave.setOnClickListener {

            onLocationSelected()
        }
        return binding.root
    }

    private fun enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                REQUEST_PERMISSION_CODE
            )
        }else{
            map.isMyLocationEnabled = true

            _locationClient.lastLocation.addOnCompleteListener{
                if(it.isSuccessful && it.result != null){
                    val currentPosition = LatLng(it.result.latitude, it.result.longitude)
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                        currentPosition,
                        ZOOM_LEVEL
                    ))
                    
                    _marker = map.addMarker(
                        MarkerOptions()
                            .position(currentPosition)
                            .title("Current Location"))

                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode == REQUEST_PERMISSION_CODE){
            if(grantResults.isNotEmpty() && (grantResults.all { it == PackageManager.PERMISSION_GRANTED})) {
                enableMyLocation()
            }
        }
    }

    private fun onLocationSelected() {
        _viewModel.reminderSelectedLocationStr.value = _marker.title
        _viewModel.longitude.value = _marker.position.longitude
        _viewModel.latitude.value = _marker.position.latitude
        _viewModel.navigationCommand.value =
            NavigationCommand.To(SelectLocationFragmentDirections.actionSelectLocationFragmentToSaveReminderFragment())
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        googleMap?.let {
            map = it
            map.setOnMapLongClickListener { clickPosition ->
                _marker.remove()
                _marker = map.addMarker(MarkerOptions()
                    .position(clickPosition)
                    .title("Current Location"))
            }
            enableMyLocation()
        }
    }

    companion object {
        private const val REQUEST_PERMISSION_CODE = 1002
        private const val ZOOM_LEVEL = 15.0f
    }
}
