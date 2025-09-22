package com.example.cardsstudy

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.gms.tasks.Tasks
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot

class AnalyticsActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var overallProgressBar: ProgressBar
    private lateinit var overallProgressText: TextView
    private lateinit var locationsBarChart: BarChart
    private lateinit var locationSpinner: Spinner

    private var allUserSessions = listOf<StudySession>()
    private var savedLocations = listOf<StudyLocation>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analytics)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar_analytics)
        toolbar.setNavigationOnClickListener { finish() }

        overallProgressBar = findViewById(R.id.overall_progress_bar)
        overallProgressText = findViewById(R.id.overall_progress_text)
        locationsBarChart = findViewById(R.id.locations_bar_chart)
        locationSpinner = findViewById(R.id.location_filter_spinner)

        setupProfileSection()
        loadInitialData()
    }

    private fun setupProfileSection() {
        val userEmailTextView = findViewById<TextView>(R.id.user_email_text)
        val logoutButton = findViewById<Button>(R.id.logout_button)
        auth.currentUser?.let { user ->
            userEmailTextView.text = user.email
        }
        logoutButton.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    private fun loadInitialData() {
        val user = auth.currentUser ?: return

        val locationsTask = db.collection("studyLocations").whereEqualTo("userId", user.uid).get()
        val sessionsTask = db.collection("studySessions").whereEqualTo("userId", user.uid).get()

        Tasks.whenAllSuccess<QuerySnapshot>(locationsTask, sessionsTask).addOnSuccessListener { results ->
            savedLocations = results[0].toObjects(StudyLocation::class.java)
            allUserSessions = results[1].toObjects(StudySession::class.java)

            Log.d("AnalyticsActivity", "Carregadas ${savedLocations.size} localizações e ${allUserSessions.size} sessões.")

            setupLocationSpinner()
            processAndDisplayAnalytics(allUserSessions)
        }.addOnFailureListener { e ->
            Log.e("AnalyticsActivity", "Falha ao carregar dados iniciais", e)
        }
    }

    private fun setupLocationSpinner() {
        val locationNames = mutableListOf("Todos os Locais")
        savedLocations.forEach { locationNames.add(it.name) }

        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, locationNames)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        locationSpinner.adapter = spinnerAdapter

        locationSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedLocationName = if (position == 0) null else locationNames[position]
                val filteredSessions = if (selectedLocationName == null) {
                    allUserSessions
                } else {
                    allUserSessions.filter { it.locationName == selectedLocationName }
                }
                processAndDisplayAnalytics(filteredSessions)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun processAndDisplayAnalytics(sessionsToDisplay: List<StudySession>) {
        setupOverallProgress(sessionsToDisplay)
        // O gráfico de barras principal sempre reflete todos os dados carregados, não apenas os filtrados.
        // Se quisesse que o gráfico também filtrasse, passaria 'sessionsToDisplay' aqui.
        setupLocationsChart(allUserSessions)
    }

    private fun setupOverallProgress(sessions: List<StudySession>) {
        val totalCorrect = sessions.sumOf { it.correctCount }
        val totalIncorrect = sessions.sumOf { it.incorrectCount }
        val totalCards = totalCorrect + totalIncorrect

        val percentage = if (totalCards > 0) (totalCorrect.toDouble() / totalCards) * 100 else 0.0

        overallProgressBar.progress = percentage.toInt()
        overallProgressText.text = "%.1f%% de acerto".format(percentage)
    }

    private fun setupLocationsChart(allSessions: List<StudySession>) {
        val sessionsByLocation = allSessions.groupBy { it.locationName }
        val locationPerformance = sessionsByLocation.mapValues { (_, locationSessions) ->
            val totalCorrect = locationSessions.sumOf { it.correctCount }
            val totalCards = locationSessions.sumOf { it.correctCount + it.incorrectCount }
            if (totalCards > 0) (totalCorrect.toDouble() / totalCards) * 100 else 0.0
        }

        val entries = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()
        locationPerformance.entries.forEachIndexed { index, entry ->
            entries.add(BarEntry(index.toFloat(), entry.value.toFloat()))
            labels.add(entry.key.takeIf { it.isNotEmpty() } ?: "Geral")
        }

        if (entries.isEmpty()) {
            locationsBarChart.data = null // Limpa dados antigos
            locationsBarChart.invalidate() // Redesenha o gráfico vazio
            locationsBarChart.visibility = View.VISIBLE // Garante que a mensagem "No chart data" aparece
            return
        }

        locationsBarChart.visibility = View.VISIBLE
        val dataSet = BarDataSet(entries, "Desempenho por Local")
        dataSet.color = Color.MAGENTA
        dataSet.valueTextSize = 12f

        locationsBarChart.data = BarData(dataSet)
        locationsBarChart.description.isEnabled = false
        locationsBarChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        locationsBarChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        locationsBarChart.xAxis.setDrawGridLines(false)
        locationsBarChart.axisLeft.axisMinimum = 0f
        locationsBarChart.axisLeft.axisMaximum = 100f
        locationsBarChart.axisRight.isEnabled = false
        locationsBarChart.animateY(1000)
        locationsBarChart.invalidate()
    }
}