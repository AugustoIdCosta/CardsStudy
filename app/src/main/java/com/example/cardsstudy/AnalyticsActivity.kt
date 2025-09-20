package com.example.cardsstudy

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class AnalyticsActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var sessionsRecyclerView: RecyclerView
    private lateinit var adapter: SessionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analytics)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar_analytics)
        toolbar.title = "Histórico Geral de Estudo"
        toolbar.setNavigationOnClickListener { finish() }

        sessionsRecyclerView = findViewById(R.id.sessions_recycler_view)
        sessionsRecyclerView.layoutManager = LinearLayoutManager(this)

        loadAllUserSessions()
    }

    private fun loadAllUserSessions() {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "Utilizador não encontrado.", Toast.LENGTH_SHORT).show()
            return
        }

        db.collectionGroup("studySessions") // Query de Grupo de Coleções
            .whereEqualTo("userId", user.uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val sessions = querySnapshot.toObjects(StudySession::class.java)
                adapter = SessionAdapter(sessions)
                sessionsRecyclerView.adapter = adapter
            }
            .addOnFailureListener { e ->
                Log.e("AnalyticsActivity", "Erro na query de grupo de coleções.", e)
                Toast.makeText(this, "Falha ao carregar histórico. Verifique o Logcat.", Toast.LENGTH_LONG).show()
            }
    }
}