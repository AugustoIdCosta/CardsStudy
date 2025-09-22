package com.example.cardsstudy

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint

class LocationsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var lastKnownLocation: LatLng? = null
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            getCurrentLocation()
        } else {
            Toast.makeText(this, "Permissão de localização negada.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_locations)

        // Adicione uma Toolbar ao seu activity_locations.xml se ainda não tiver
        val toolbar = findViewById<Toolbar>(R.id.toolbar_locations) // Presume-se que o ID da Toolbar é este
        setSupportActionBar(toolbar)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        findViewById<FloatingActionButton>(R.id.fab_save_location).setOnClickListener {
            showSaveLocationDialog()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        checkLocationPermission()
        loadAndDisplaySavedLocations()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_locations, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.action_manage_locations) {
            startActivity(Intent(this, LocationsListActivity::class.java))
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    private fun loadAndDisplaySavedLocations() {
        val user = auth.currentUser ?: return
        db.collection("studyLocations")
            .whereEqualTo("userId", user.uid)
            .get()
            .addOnSuccessListener { querySnapshot ->
                map.clear()
                for (document in querySnapshot.documents) {
                    val studyLocation = document.toObject(StudyLocation::class.java)
                    studyLocation?.location?.let { geoPoint ->
                        val latLng = LatLng(geoPoint.latitude, geoPoint.longitude)
                        map.addMarker(
                            MarkerOptions()
                                .position(latLng)
                                .title(studyLocation.name)
                        )
                    }
                }
            }
    }

    private fun showSaveLocationDialog() {
        if (lastKnownLocation == null) {
            Toast.makeText(this, "Localização atual ainda não obtida. Tente novamente.", Toast.LENGTH_SHORT).show()
            return
        }
        val editText = EditText(this)
        editText.hint = "Ex: Casa, Biblioteca"
        AlertDialog.Builder(this)
            .setTitle("Nomear Local de Estudo")
            .setView(editText)
            .setPositiveButton("Guardar") { _, _ ->
                val name = editText.text.toString().trim()
                if (name.isNotEmpty()) {
                    saveLocationToFirebase(name)
                } else {
                    Toast.makeText(this, "O nome não pode ser vazio.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun saveLocationToFirebase(name: String) {
        val user = auth.currentUser
        val location = lastKnownLocation
        if (user == null || location == null) {
            Toast.makeText(this, "Erro: Utilizador ou localização não encontrados.", Toast.LENGTH_SHORT).show()
            return
        }
        val geoPoint = GeoPoint(location.latitude, location.longitude)
        val studyLocation = StudyLocation(
            userId = user.uid,
            name = name,
            location = geoPoint
        )
        db.collection("studyLocations")
            .add(studyLocation)
            .addOnSuccessListener {
                Toast.makeText(this, "Local '$name' guardado com sucesso!", Toast.LENGTH_SHORT).show()
                loadAndDisplaySavedLocations()
            }
            .addOnFailureListener { e ->
                Log.e("LocationsActivity", "Erro ao guardar localização", e)
                Toast.makeText(this, "Falha ao guardar o local.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
                getCurrentLocation()
            }
            else -> {
                locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            map.isMyLocationEnabled = true
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        val currentLatLng = LatLng(location.latitude, location.longitude)
                        lastKnownLocation = currentLatLng
                        if (map.cameraPosition.zoom < 10f) { // Evita mover o mapa se o utilizador já deu zoom
                            map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                        }
                    }
                }
        }
    }
}