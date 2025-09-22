package com.example.cardsstudy

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.Tasks
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

class MainActivity : AppCompatActivity() {

    private lateinit var decksRecyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var deckAdapter: DeckAdapter

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var deckList = mutableListOf<Deck>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        decksRecyclerView = findViewById(R.id.decks_recycler_view)
        emptyView = findViewById(R.id.empty_view)

        setupRecyclerView()

        findViewById<FloatingActionButton>(R.id.fab_add_deck).setOnClickListener {
            showAddDeckDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        // A chamada é movida para aqui. Este método é executado sempre que a tela volta a ser o foco.
        loadDecks()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_global_analytics -> {
                startActivity(Intent(this, AnalyticsActivity::class.java))
                true
            }
            R.id.action_locations -> {
                startActivity(Intent(this, LocationsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupRecyclerView() {
        deckAdapter = DeckAdapter(deckList,
            onItemClick = { clickedDeck ->
                val intent = Intent(this, CardListActivity::class.java).apply {
                    putExtra(CardListActivity.EXTRA_DECK_ID, clickedDeck.id)
                    putExtra(CardListActivity.EXTRA_DECK_NAME, clickedDeck.name)
                }
                startActivity(intent)
            },
            onDeleteClick = { deckToDelete ->
                showDeleteConfirmationDialog(deckToDelete)
            }
        )
        decksRecyclerView.layoutManager = LinearLayoutManager(this)
        decksRecyclerView.adapter = deckAdapter
    }

    private fun showDeleteConfirmationDialog(deck: Deck) {
        AlertDialog.Builder(this)
            .setTitle("Apagar Baralho")
            .setMessage("Tem a certeza que quer apagar o baralho '${deck.name}'? Esta ação não pode ser desfeita.")
            .setPositiveButton("Apagar") { _, _ ->
                deleteDeck(deck)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteDeck(deck: Deck) {
        db.collection("decks").document(deck.id).delete()
            .addOnSuccessListener {
                loadDecks() // Recarrega a lista para refletir a exclusão
            }
            .addOnFailureListener { e ->
                Log.e("MainActivity", "Erro ao apagar baralho", e)
            }
    }
    private fun showAddDeckDialog() {
        // Usamos o mesmo layout que criámos para os cartões, mas podemos criar um específico se quiser
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_deck, null)
        val nameEditText = dialogView.findViewById<EditText>(R.id.deck_name_edit_text)
        val descriptionEditText = dialogView.findViewById<EditText>(R.id.deck_description_edit_text)

        AlertDialog.Builder(this)
            .setTitle("Novo Baralho")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val name = nameEditText.text.toString().trim()
                val description = descriptionEditText.text.toString().trim()
                if (name.isNotEmpty()) {
                    saveNewDeck(name, description)
                } else {
                    Toast.makeText(this, "O nome não pode ser vazio.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun saveNewDeck(name: String, description: String) {
        val user = auth.currentUser ?: return
        val newDeck = Deck(
            name = name,
            description = description,
            userId = user.uid
        )
        db.collection("decks").add(newDeck)
            .addOnSuccessListener {
                Toast.makeText(this, "Baralho guardado!", Toast.LENGTH_SHORT).show()
                loadDecks() // Atualiza a lista
            }
            .addOnFailureListener { e ->
                Log.e("MainActivity", "Erro ao guardar baralho", e)
            }
    }

    private fun loadDecks() {
        val currentUser = auth.currentUser ?: return
        db.collection("decks").whereEqualTo("userId", currentUser.uid).get()
            .addOnSuccessListener { deckSnapshot ->
                val decksWithId = mutableListOf<Deck>()
                for (document in deckSnapshot.documents) {
                    val deck = document.toObject(Deck::class.java)
                    deck?.let {
                        it.id = document.id
                        decksWithId.add(it)
                    }
                }

                if (decksWithId.isEmpty()) {
                    deckList.clear()
                    updateUI()
                    return@addOnSuccessListener
                }

                val countTasks = decksWithId.map { deck ->
                    db.collection("decks").document(deck.id).collection("cards")
                        .whereLessThanOrEqualTo("nextReview", Date()).get()
                        .addOnSuccessListener { cardSnapshot ->
                            deck.dueCardsCount = cardSnapshot.size()
                        }
                }

                Tasks.whenAll(countTasks).addOnSuccessListener {
                    deckList.clear()
                    deckList.addAll(decksWithId)
                    updateUI()
                }
            }
            .addOnFailureListener { e ->
                Log.e("MainActivity", "Erro ao carregar baralhos", e)
            }
    }

    private fun updateUI() {
        if (deckList.isEmpty()) {
            decksRecyclerView.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
        } else {
            decksRecyclerView.visibility = View.VISIBLE
            emptyView.visibility = View.GONE
        }
        deckAdapter.notifyDataSetChanged()
    }
}