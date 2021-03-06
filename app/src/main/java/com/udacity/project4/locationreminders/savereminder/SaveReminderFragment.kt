package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.TargetApi
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.location.Geofence.NEVER_EXPIRE
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding
    private lateinit var _requestPermissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var _requestLocationSetting: ActivityResultLauncher<IntentSenderRequest>
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
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        binding.saveReminder.setOnClickListener {
            if(!geofencingPermissionsApproved()){
                requestGeofencingPermissions()
            }else {
                checkDeviceLocationSettings()
            }
        }

        _viewModel.reminderSelectedLocationStr.observe(viewLifecycleOwner, Observer {
            if(it != null){
                binding.selectedLocation.text = it
            }
        })

        _viewModel.showSnackBarInt.observe(viewLifecycleOwner, Observer {
            Snackbar.make(
                binding.fragmentSaveReminder,
                getString(it!!),
                Snackbar.LENGTH_LONG
            )
                .show()
        })

        _viewModel.showSnackBar.observe(viewLifecycleOwner, Observer{
            val snackbar = Snackbar.make(
                binding.fragmentSaveReminder,
                it,
                Snackbar.LENGTH_INDEFINITE
            )

            snackbar.setAction("enable", View.OnClickListener {
                requestLocationPermissions()
            })

            snackbar.show()
        })

        _requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){ permissions ->

            if(permissions.all { permission -> permission.value!! }){
                binding.saveReminder.callOnClick()
            }else{
                Toast.makeText(requireContext(), "You need to grant location permission in order to add a new reminder", Toast.LENGTH_SHORT).show()
            }
        }

        _requestLocationSetting = registerForActivityResult(StartIntentSenderForResult()){
            try {
                when (it.resultCode) {
                    -1 -> {
                        binding.saveReminder.callOnClick()
                    }
                    0 -> {
                        _viewModel.showSnackBar.value = getString(R.string.location_required_error)
                    }
                    else -> {

                        Log.e("SaveReminderFragment", "Unable to receive location setting.")
                    }
                }
            }catch(exception: Exception){
                Log.e("SaveReminderFragment", exception.localizedMessage!!)
            }
        }
    }

    private fun addGeofence() {

        val title = _viewModel.reminderTitle.value
        val description = _viewModel.reminderDescription.value
        val location = _viewModel.reminderSelectedLocationStr.value
        val latitude = _viewModel.latitude.value
        val longitude = _viewModel.longitude.value

        val reminder = ReminderDataItem(
            title,
            description,
            location,
            latitude,
            longitude
        )

        if(_viewModel.validateEnteredData(reminder)){
            val geofence = Geofence.Builder()
                .setRequestId(reminder.id)
                .setCircularRegion(reminder.latitude!!, reminder.longitude!!, GEOFENCE_RADIUS_IN_METERS)
                .setExpirationDuration(NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build()

            val geofencingRequest = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build()

            _geofenceClient.addGeofences(geofencingRequest, _geofencePendingIntent)?.run {
                addOnSuccessListener {
                    Snackbar.make(
                        binding.fragmentSaveReminder,
                        getString(R.string.geofence_entered),
                        Snackbar.LENGTH_SHORT
                    ).show()

                    _viewModel.validateAndSaveReminder(reminder)
                }

                addOnFailureListener {
                    Toast.makeText(
                        requireContext(),
                        "Unable to to create Geofence",
                        Toast.LENGTH_SHORT
                    )
                        .show()

                    Log.w("SaveReminderFragment", it.message.toString())
                }
            }
        }
    }

    private fun checkDeviceLocationSettings(resolve:Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }

        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask = settingsClient.checkLocationSettings(builder.build())
        locationSettingsResponseTask.addOnFailureListener{exception ->
            if(exception is ResolvableApiException && resolve){
                try{
                    _requestLocationSetting.launch(IntentSenderRequest.Builder(exception.resolution).build())
                }catch(exception: IntentSender.SendIntentException){
                    Log.d("SaveReminderFragment", "Error getting location settings resolution: " + exception.message)
                }
            }else{
                Snackbar.make(
                    binding.fragmentSaveReminder,
                    R.string.location_required_error,
                    Snackbar.LENGTH_INDEFINITE
                ).setAction("OK"){
                    checkDeviceLocationSettings()
                }.show()
            }
        }

        locationSettingsResponseTask.addOnCompleteListener {
            if(it.isSuccessful){
                addGeofence()
            }
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

        var permissionArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

        if(_runningQOrLater){
            permissionArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
        }

        _requestPermissionLauncher.launch(
            permissionArray
        )
    }

    private fun requestLocationPermissions() {

        _requestPermissionLauncher.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
    }

    companion object{
        private const val GEOFENCE_RADIUS_IN_METERS = 100.0f
        private const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 33
        private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
        private const val LOCATION_PERMISSION_INDEX = 0
        private const val BACKGROUND_LOCATION_PERMISSION_INDEX = 1
        private const val REQUEST_LOCATION_PERMISSION_CODE = 1002
        private const val REQUEST_TURN_DEVICE_LOCATION_ON = 1003
    }
}
