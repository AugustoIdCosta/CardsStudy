package com.example.cardsstudy

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LocationsListActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var locationsRecyclerView: RecyclerView
    private lateinit var adapter: LocationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_locations_list)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar_locations_list)
        toolbar.setNavigationOnClickListener { finish() }

        locationsRecyclerView = findViewById(R.id.locations_recycler_view)
        locationsRecyclerView.layoutManager = LinearLayoutManager(this)

        loadLocations()
    }

    private fun loadLocations() {
        val user = auth.currentUser ?: return
        db.collection("studyLocations")
            .whereEqualTo("userId", user.uid)
            .get()
            .addOnSuccessListener { querySnapshot ->
                // CORREÇÃO AQUI: Iteramos para obter os IDs
                val locations = mutableListOf<StudyLocation>()
                for (document in querySnapshot.documents) {
                    val location = document.toObject(StudyLocation::class.java)
                    location?.let {
                        it.id = document.id // Atribuímos o ID manualmente
                        locations.add(it)
                    }
                }

                adapter = LocationAdapter(locations) { locationToDelete ->
                    showDeleteConfirmationDialog(locationToDelete)
                }
                locationsRecyclerView.adapter = adapter
            }
    }

    private fun showDeleteConfirmationDialog(location: StudyLocation) {
        AlertDialog.Builder(this)
            .setTitle("Apagar Local")
            .setMessage("Tem a certeza que quer apagar '${location.name}'?")
            .setPositiveButton("Apagar") { _, _ -> deleteLocation(location) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteLocation(location: StudyLocation) {
        // Agora, location.id terá o valor correto
        db.collection("studyLocations").document(location.id).delete()
            .addOnSuccessListener {
                Toast.makeText(this, "'${location.name}' apagado.", Toast.LENGTH_SHORT).show()
                loadLocations()
            }
            .addOnFailureListener { e ->
                Log.e("LocationsList", "Erro ao apagar", e)
                Toast.makeText(this, "Falha ao apagar.", Toast.LENGTH_SHORT).show()
            }
    }
}