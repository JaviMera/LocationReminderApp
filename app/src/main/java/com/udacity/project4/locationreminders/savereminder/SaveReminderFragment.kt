package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.TargetApi
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.concurrent.TimeUnit

class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding
    private val _geofenceClient: GeofencingClient by lazy {LocationServices.getGeofencingClient(requireContext())}
    private val _geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireActivity(), GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(requireContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private val _runningQOrLater = android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.Q

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            //            Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value

            val geofence = Geofence.Builder()
                .setRequestId(location)
                .setCircularRegion(latitude!!, longitude!!, GEOFENCE_RADIUS_IN_METERS)
                .setExpirationDuration(TimeUnit.DAYS.toMillis(1))
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build()

            val geofencingRequest = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build()

            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Toast.makeText(requireContext(), "User permissions are not granted.", Toast.LENGTH_SHORT)
                    .show()

                return@setOnClickListener
            }else{

                _geofenceClient.addGeofences(geofencingRequest, _geofencePendingIntent)?.run {
                    addOnSuccessListener {
                        Toast.makeText(requireContext(), "Geofence created", Toast.LENGTH_SHORT)
                            .show()
                    }

                    addOnFailureListener {
                        Toast.makeText(requireContext(), "Unable to to create Geofence", Toast.LENGTH_SHORT)
                            .show()

                        Log.w("SaveReminderFragment", it.message.toString())
                    }
                }
            }
//            TODO: use the user entered reminder details to:
//             2) save the reminder to the local db
        }

        _viewModel.reminderSelectedLocationStr.observe(viewLifecycleOwner, Observer {
            if(it != null){
                binding.selectedLocation.text = it
            }
        })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(grantResults.isEmpty()
            || grantResults[LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED
            || (requestCode == REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
                    && grantResults[BACKGROUND_LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED)){

            Toast.makeText(requireContext(), "You need to grant location permission in order to add a new reminder", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStart() {
        super.onStart()

        if(geofencingPermissionsApproved()){
            // TODO: Add check for device's location
        }else{
            requestGeofencingPermissions()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }

    @TargetApi(29)
    private fun geofencingPermissionsApproved() : Boolean {
        val foregroundLocationApproved = (
                PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION))

        val backgroundPermissionApproved = if(_runningQOrLater) {
            PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        }else{
            return true
        }

        return foregroundLocationApproved && backgroundPermissionApproved
    }

    @TargetApi(29)
    private fun requestGeofencingPermissions(){
        if(geofencingPermissionsApproved())
            return

        var permissionArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

        val resultCode = when{
            _runningQOrLater -> {
                permissionArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
            }
            else -> REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        }

        ActivityCompat.requestPermissions(
            requireActivity(),
            permissionArray,
            resultCode
        )
    }

    companion object{
        private const val GEOFENCE_RADIUS_IN_METERS = 100.0f
        private const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 33
        private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
        private const val LOCATION_PERMISSION_INDEX = 0
        private const val BACKGROUND_LOCATION_PERMISSION_INDEX = 1
    }
}
