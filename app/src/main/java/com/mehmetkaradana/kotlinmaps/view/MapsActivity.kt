package com.mehmetkaradana.kotlinmaps.view

import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.view.View
import androidx.core.text.set
import androidx.room.Room


import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.mehmetkaradana.kotlinmaps.R
import com.mehmetkaradana.kotlinmaps.databinding.ActivityMapsBinding
import com.mehmetkaradana.kotlinmaps.model.Place
import com.mehmetkaradana.kotlinmaps.roomdb.PlaceDao
import com.mehmetkaradana.kotlinmaps.roomdb.PlaceDatabase
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers


class MapsActivity : AppCompatActivity(), OnMapReadyCallback ,GoogleMap.OnMapLongClickListener{

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var locationListener: LocationListener
    private lateinit var locationManager: LocationManager
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private lateinit var sharedPreferences: SharedPreferences
    private  var trackBoolean : Boolean? = null
    private var selectedLatitude  :Double? = null
    private var selectedLongitude  :Double? = null
    private lateinit var db : PlaceDatabase
    private lateinit var placeDao: PlaceDao
    val compositeDisposable = CompositeDisposable()
    var placeFromMain: Place? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        val view = binding.root

        setContentView(view)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        registerLauncher()

        sharedPreferences = this.getSharedPreferences("com.mehmetkaradana.kotlinmaps", MODE_PRIVATE)
        trackBoolean = false

        db = Room.databaseBuilder(applicationContext,PlaceDatabase::class.java,"Places")
          //  .allowMainThreadQueries()
            .build()
        placeDao = db.PlaceDao()

        binding.saveButton.isEnabled = false
    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setOnMapLongClickListener (this)

        val intent = intent
        val info = intent.getStringExtra("info")

        if(info == "new"){
            binding.saveButton.visibility = View.VISIBLE
            binding.deleteButton.visibility = View.INVISIBLE
            //casting getsys.. herhangi bir service döndürüyor bu nedenle casting yaptım
            locationManager = this.getSystemService(LOCATION_SERVICE) as LocationManager

            locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    //  println("location: " + location.toString())
                    trackBoolean = sharedPreferences.getBoolean("trackBoolean",false)
                    if(!trackBoolean!!){
                        val userLocation = LatLng(location.latitude,location.longitude)
                        mMap.addMarker(MarkerOptions().position(userLocation).title("My Location"))
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation,16f))
                        sharedPreferences.edit().putBoolean("trackBoolean",true).apply()
                    }
                }
            }

            if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                //shouldShow androidin bir yapısı permission sorulmalı mı buna karar verir //Bu satır, daha önce kullanıcı izin istemini reddetmişse veya "Bir daha sorma" seçeneğini işaretlemeden reddetmişse true döner.
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION)){
                    Snackbar.make(binding.root,"Permission needed for location",Snackbar.LENGTH_LONG).setAction("Give Permission",
                        View.OnClickListener {
                            //request permission
                            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }).show()
                }else{
                    //Request Permission
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }

            }else{
                //permission granted
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0f,locationListener)

                val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if(lastLocation != null){
                    val lastUserLocation = LatLng(lastLocation.latitude,lastLocation.longitude)
                    //   mMap.addMarker(MarkerOptions().position(lastUserLocation).title("My new Location"))
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLocation,16f))
                }
                mMap.isMyLocationEnabled = true
            }
        }else{
            mMap.clear()
            placeFromMain = intent.getSerializableExtra("selectedPlace") as? Place
            placeFromMain?.let {
                val latlong = LatLng(it.latitude,it.longitude)

                mMap.addMarker(MarkerOptions().position(latlong).title(it.name))
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlong,15f))

                binding.saveButton.visibility = View.GONE
                binding.deleteButton.visibility = View.VISIBLE

                binding.editText.setText(it.name)

            }
        }


    }

    private fun registerLauncher(){//rfa izin isteyen API
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){ result ->
            if(result){
                if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                    //permission granted
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0f,locationListener)
                    val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    if(lastLocation != null){
                        val lastUserLocation = LatLng(lastLocation.latitude,lastLocation.longitude)
                       // mMap.addMarker(MarkerOptions().position(lastUserLocation).title("My new Location"))
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLocation,16f))
                    }
                    mMap.isMyLocationEnabled = true
                }
            }else{//bu dont allow dedikten sonra girdiginde
                //permission denied
                Toast.makeText(this@MapsActivity,"Permission needed!",Toast.LENGTH_LONG).show()
            }

        }
    }

    override fun onMapLongClick(p0: LatLng) {
       mMap.clear()
       mMap.addMarker(MarkerOptions().position(p0).title("Selected Location"))

        selectedLatitude=p0.latitude
        selectedLongitude=p0.longitude

        binding.saveButton.isEnabled = true

    }

    fun save(view: View){
//main thread UI,Default thread, -> cpu,IO thread db/internet
      //  val place = Place("test",1.1,1.1)
        if(selectedLatitude != null && selectedLongitude !=null){
            val place = Place(binding.editText.text.toString(),selectedLatitude!!,selectedLongitude!!)
           // placeDao.insert(place)
            compositeDisposable.add(
                placeDao.insert(place)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::handleResponse)
            )
        }
    }

    private fun handleResponse(){

        val intent = Intent(this,MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)

    }

    fun delete(view: View){
        placeFromMain?.let {
            compositeDisposable.add(
                placeDao.delete(it)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::handleResponse)
            )
        }
       /* if(selectedLatitude != null && selectedLongitude !=null){
            val place = Place(binding.editText.text.toString(),selectedLatitude!!,selectedLongitude!!)
            // placeDao.insert(place)
            compositeDisposable.add(
                placeDao.delete(place)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::handleResponse)
            )
        }*/
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
    }
}

