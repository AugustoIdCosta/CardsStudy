package com.example.cardsstudy

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore

class CardListActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_DECK_ID = "extra_deck_id"
        const val EXTRA_DECK_NAME = "extra_deck_name"
    }

    private val db = FirebaseFirestore.getInstance()
    private lateinit var cardsRecyclerView: RecyclerView
    private lateinit var adapter: CardAdapter
    private var deckId: String? = null

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
                deckId?.let {
                    val intent = Intent(this, StudyActivity::class.java).apply {
                        putExtra(StudyActivity.EXTRA_DECK_ID, it)
                        putExtra(StudyActivity.EXTRA_DECK_NAME, intent.getStringExtra(EXTRA_DECK_NAME))
                    }
                    startActivity(intent)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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

        AlertDialog.Builder(this)
            .setTitle("Novo Cartão (Frente e Verso)")
            .setView(dialogView)
            .setPositiveButton("Salvar") { _, _ ->
                val front = frontEditText.text.toString().trim()
                val back = backEditText.text.toString().trim()
                if (front.isNotEmpty() && back.isNotEmpty()) {
                    saveNewCard(FrontBackCard(front, back))
                } else {
                    Toast.makeText(this, "Preencha a frente e o verso.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
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
                    loadCards(deckId) // Recarrega a lista para refletir a exclusão
                }
                .addOnFailureListener { e ->
                    Log.e("CardListActivity", "Erro ao apagar cartão", e)
                    Toast.makeText(this, "Falha ao apagar o cartão.", Toast.LENGTH_SHORT).show()
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
                // A criação do adapter agora inclui a lógica de exclusão
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