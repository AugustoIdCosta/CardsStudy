package com.example.cardsstudy

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class CardListActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_DECK_ID = "extra_deck_id"
        const val EXTRA_DECK_NAME = "extra_deck_name"
    }

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private lateinit var cardsRecyclerView: RecyclerView
    private lateinit var adapter: CardAdapter
    private var deckId: String? = null

    private var selectedImageUri: Uri? = null
    private lateinit var cardImagePreview: ImageView
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            cardImagePreview.setImageURI(it)
            cardImagePreview.visibility = View.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card_list)

        deckId = intent.getStringExtra(EXTRA_DECK_ID)
        val deckName = intent.getStringExtra(EXTRA_DECK_NAME)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar_card_list)
        setSupportActionBar(toolbar)
        toolbar.title = deckName ?: "Cartões"
        toolbar.setNavigationOnClickListener { finish() }

        cardsRecyclerView = findViewById(R.id.cards_recycler_view)
        cardsRecyclerView.layoutManager = LinearLayoutManager(this)

        if (deckId == null) {
            Toast.makeText(this, "Erro: ID do baralho não encontrado.", Toast.LENGTH_LONG).show()
            finish(); return
        }

        loadCards(deckId!!)

        findViewById<FloatingActionButton>(R.id.fab_add_card).setOnClickListener {
            showCardTypeSelectorDialog()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_card_list, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_study -> {
                showLocationSelectorDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showLocationSelectorDialog() {
        val user = auth.currentUser ?: return
        db.collection("studyLocations").whereEqualTo("userId", user.uid).get()
            .addOnSuccessListener { querySnapshot ->
                val locations = mutableListOf<StudyLocation>()
                for (document in querySnapshot.documents) {
                    val location = document.toObject(StudyLocation::class.java)
                    location?.let {
                        it.id = document.id
                        locations.add(it)
                    }
                }
                val locationNames = mutableListOf("Estudo Geral (sem local)")
                locations.forEach { locationNames.add(it.name) }

                AlertDialog.Builder(this)
                    .setTitle("Onde está a estudar?")
                    .setItems(locationNames.toTypedArray()) { _, which ->
                        val selectedLocation = if (which == 0) null else locations[which - 1]
                        startStudySession(selectedLocation)
                    }
                    .show()
            }
            .addOnFailureListener { startStudySession(null) }
    }

    private fun startStudySession(location: StudyLocation?) {
        val deckName = intent.getStringExtra(EXTRA_DECK_NAME)
        val intent = Intent(this, StudyActivity::class.java).apply {
            putExtra(StudyActivity.EXTRA_DECK_ID, deckId)
            putExtra(StudyActivity.EXTRA_DECK_NAME, deckName)
            location?.let {
                putExtra(StudyActivity.EXTRA_LOCATION_ID, it.id)
                putExtra(StudyActivity.EXTRA_LOCATION_NAME, it.name)
            }
        }
        startActivity(intent)
    }

    private fun showCardTypeSelectorDialog() {
        val cardTypes = arrayOf("Frente e Verso", "Múltipla Escolha", "Digite a Resposta", "Omissão (Cloze)")
        AlertDialog.Builder(this)
            .setTitle("Selecione o Tipo de Cartão")
            .setItems(cardTypes) { _, which ->
                when (which) {
                    0 -> showAddFrontBackDialog()
                    1 -> showAddMultipleChoiceDialog()
                    2 -> showAddTypeAnswerDialog()
                    3 -> showAddClozeDialog()
                }
            }
            .show()
    }

    private fun showAddFrontBackDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_card, null)
        val frontEditText = dialogView.findViewById<EditText>(R.id.card_front_edit_text)
        val backEditText = dialogView.findViewById<EditText>(R.id.card_back_edit_text)
        val selectImageButton = dialogView.findViewById<Button>(R.id.select_image_button)
        cardImagePreview = dialogView.findViewById(R.id.card_image_preview)
        selectedImageUri = null
        cardImagePreview.visibility = View.GONE
        selectImageButton.setOnClickListener { pickImageLauncher.launch("image/*") }
        AlertDialog.Builder(this)
            .setTitle("Novo Cartão (Frente e Verso)")
            .setView(dialogView)
            .setPositiveButton("Salvar") { _, _ ->
                val frontText = frontEditText.text.toString().trim()
                val backText = backEditText.text.toString().trim()
                if (backText.isEmpty() || (frontText.isEmpty() && selectedImageUri == null)) {
                    Toast.makeText(this, "Preencha o verso e um campo da frente (texto ou imagem).", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }
                uploadImageAndSaveCard(frontText, backText)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun uploadImageAndSaveCard(frontText: String, backText: String) {
        if (selectedImageUri != null) {
            val imageRef = storage.reference.child("card_images/${UUID.randomUUID()}")
            imageRef.putFile(selectedImageUri!!)
                .continueWithTask { task ->
                    if (!task.isSuccessful) { task.exception?.let { throw it } }
                    imageRef.downloadUrl
                }.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val downloadUrl = task.result.toString()
                        saveNewCard(FrontBackCard(frontText, backText, downloadUrl))
                    } else {
                        Toast.makeText(this, "Falha no upload da imagem.", Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            saveNewCard(FrontBackCard(frontText, backText, null))
        }
    }

    private fun showAddMultipleChoiceDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_multiple_choice, null)
        val questionEt = dialogView.findViewById<EditText>(R.id.mc_question_edit_text)
        val correctEt = dialogView.findViewById<EditText>(R.id.mc_correct_answer_edit_text)
        val distractor1Et = dialogView.findViewById<EditText>(R.id.mc_distractor1_edit_text)
        val distractor2Et = dialogView.findViewById<EditText>(R.id.mc_distractor2_edit_text)
        val distractor3Et = dialogView.findViewById<EditText>(R.id.mc_distractor3_edit_text)
        AlertDialog.Builder(this)
            .setTitle("Novo Cartão (Múltipla Escolha)")
            .setView(dialogView)
            .setPositiveButton("Salvar") { _, _ ->
                val question = questionEt.text.toString().trim()
                val correctAnswer = correctEt.text.toString().trim()
                val distractors = listOfNotNull(
                    distractor1Et.text.toString().trim().takeIf { it.isNotEmpty() },
                    distractor2Et.text.toString().trim().takeIf { it.isNotEmpty() },
                    distractor3Et.text.toString().trim().takeIf { it.isNotEmpty() }
                )
                if (question.isNotEmpty() && correctAnswer.isNotEmpty() && distractors.isNotEmpty()) {
                    saveNewCard(MultipleChoiceCard(question, correctAnswer, distractors))
                } else {
                    Toast.makeText(this, "Preencha a pergunta, a resposta correta e pelo menos um distrator.", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showAddTypeAnswerDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_type_answer, null)
        val promptEt = dialogView.findViewById<EditText>(R.id.ta_prompt_edit_text)
        val answersEt = dialogView.findViewById<EditText>(R.id.ta_answers_edit_text)
        AlertDialog.Builder(this)
            .setTitle("Novo Cartão (Digite a Resposta)")
            .setView(dialogView)
            .setPositiveButton("Salvar") { _, _ ->
                val prompt = promptEt.text.toString().trim()
                val answersString = answersEt.text.toString().trim()
                if (prompt.isNotEmpty() && answersString.isNotEmpty()) {
                    val acceptableAnswers = answersString.split(',').map { it.trim() }
                    saveNewCard(TypeAnswerCard(prompt, acceptableAnswers))
                } else {
                    Toast.makeText(this, "Preencha a pergunta e pelo menos uma resposta.", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showAddClozeDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_cloze, null)
        val clozeEt = dialogView.findViewById<EditText>(R.id.cloze_edit_text)
        AlertDialog.Builder(this)
            .setTitle("Novo Cartão (Omissão)")
            .setView(dialogView)
            .setPositiveButton("Salvar") { _, _ ->
                val text = clozeEt.text.toString().trim()
                if (text.isNotEmpty() && text.contains("[[") && text.contains("]]")) {
                    saveNewCard(ClozeCard(text))
                } else {
                    Toast.makeText(this, "O texto deve conter pelo menos uma omissão, como [[exemplo]].", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun saveNewCard(card: Card) {
        deckId?.let { deckId ->
            db.collection("decks").document(deckId).collection("cards")
                .add(card)
                .addOnSuccessListener {
                    Toast.makeText(this, "Cartão salvo com sucesso!", Toast.LENGTH_SHORT).show()
                    loadCards(deckId)
                }
                .addOnFailureListener { e ->
                    Log.e("CardListActivity", "Erro ao salvar cartão", e)
                    Toast.makeText(this, "Falha ao salvar o cartão.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun showDeleteCardConfirmationDialog(card: Card) {
        AlertDialog.Builder(this)
            .setTitle("Apagar Cartão")
            .setMessage("Tem a certeza que quer apagar este cartão? A ação não pode ser desfeita.")
            .setPositiveButton("Apagar") { _, _ ->
                deleteCard(card)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteCard(card: Card) {
        deckId?.let { deckId ->
            db.collection("decks").document(deckId).collection("cards").document(card.id)
                .delete()
                .addOnSuccessListener {
                    Toast.makeText(this, "Cartão apagado.", Toast.LENGTH_SHORT).show()
                    loadCards(deckId)
                }
                .addOnFailureListener { e ->
                    Log.e("CardListActivity", "Erro ao apagar cartão", e)
                }
        }
    }

    private fun loadCards(deckId: String) {
        db.collection("decks").document(deckId).collection("cards")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val cardList = mutableListOf<Card>()
                for (document in querySnapshot.documents) {
                    val cardTypeString = document.getString("type") ?: "FRONT_BACK"
                    val card = try {
                        when (CardType.valueOf(cardTypeString)) {
                            CardType.FRONT_BACK -> document.toObject(FrontBackCard::class.java)
                            CardType.MULTIPLE_CHOICE -> document.toObject(MultipleChoiceCard::class.java)
                            CardType.TYPE_ANSWER -> document.toObject(TypeAnswerCard::class.java)
                            CardType.CLOZE -> document.toObject(ClozeCard::class.java)
                        }
                    } catch (e: IllegalArgumentException) {
                        Log.w("CardListActivity", "Tipo de cartão desconhecido: $cardTypeString")
                        null
                    }
                    card?.let {
                        it.id = document.id
                        cardList.add(it)
                    }
                }
                adapter = CardAdapter(cardList) { cardToDelete ->
                    showDeleteCardConfirmationDialog(cardToDelete)
                }
                cardsRecyclerView.adapter = adapter
            }
            .addOnFailureListener { e ->
                Log.e("CardListActivity", "Erro ao carregar cartões", e)
                Toast.makeText(this, "Falha ao carregar os cartões.", Toast.LENGTH_SHORT).show()
            }
    }
}