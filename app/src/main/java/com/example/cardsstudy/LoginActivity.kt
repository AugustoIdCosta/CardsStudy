package com.example.cardsstudy

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.example.cardsstudy.MainActivity

class LoginActivity : AppCompatActivity() {

    // Declara a instância de autenticação do Firebase
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Inicializa a instância do FirebaseAuth
        auth = FirebaseAuth.getInstance()

        // Referências para os componentes da UI
        val emailEditText = findViewById<TextInputEditText>(R.id.email_edit_text)
        val passwordEditText = findViewById<TextInputEditText>(R.id.password_edit_text)
        val loginButton = findViewById<Button>(R.id.login_button)
        val registerTextView = findViewById<TextView>(R.id.register_text_view)

        // Configura o clique do botão de login
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            // Validação simples para ver se os campos não estão vazios
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor, preencha todos os campos.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Tenta fazer o login com o Firebase
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Login bem-sucedido
                        Toast.makeText(this, "Login efetuado com sucesso!", Toast.LENGTH_SHORT).show()

                        // Navega para a tela principal do app
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish() // Fecha a LoginActivity para o usuário não voltar para ela
                    } else {
                        // Se o login falhar, mostra uma mensagem de erro
                        Toast.makeText(this, "Falha na autenticação: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        // Configura o clique do texto de cadastro
        registerTextView.setOnClickListener {
            // Inicia a com.example.cardsstudy.RegisterActivity (que vamos criar)
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }
}