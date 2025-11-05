package com.aniak.azakron

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.aniak.azakron.databinding.ActivityLoginBinding
import com.example.azakron.Notes
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var googleSignInClient: GoogleSignInClient

    // Launcher para o resultado do Google Sign-In
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        handleSignInResult(task)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verificar se usuário já está logado
        if (isUserLoggedIn()) {
            navigateToNotes()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupGoogleSignIn()
        setupClickListeners()
    }

    private fun setupGoogleSignIn() {
        // Configure o Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            // Adicione seu Web Client ID aqui do Firebase Console
            // .requestIdToken("YOUR_WEB_CLIENT_ID")
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun setupClickListeners() {
        binding.btnGoogleLogin.setOnClickListener {
            signInWithGoogle()
        }
    }

    private fun signInWithGoogle() {
        // Mostrar loading
        binding.progressBar.visibility = View.VISIBLE
        binding.btnGoogleLogin.isEnabled = false

        // Limpar conta anterior para forçar seleção de conta
        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)

            // Login bem-sucedido
            saveUserSession(account)

            Toast.makeText(
                this,
                "Bem-vindo, ${account.displayName}!",
                Toast.LENGTH_SHORT
            ).show()

            navigateToNotes()

        } catch (e: ApiException) {
            // Falha no login
            binding.progressBar.visibility = View.GONE
            binding.btnGoogleLogin.isEnabled = true

            when (e.statusCode) {
                12501 -> {
                    // Usuário cancelou o login
                    Toast.makeText(this, "Login cancelado", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    Toast.makeText(
                        this,
                        "Erro ao fazer login: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun saveUserSession(account: GoogleSignInAccount) {
        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean("is_logged_in", true)
            putString("user_name", account.displayName)
            putString("user_email", account.email)
            putString("user_photo_url", account.photoUrl?.toString())
            putString("user_id", account.id)
            apply()
        }
    }

    private fun isUserLoggedIn(): Boolean {
        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        return prefs.getBoolean("is_logged_in", false)
    }

    private fun navigateToNotes() {
        val intent = Intent(this, Notes::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}

// Adicione este companion object na sua Notes Activity para fazer logout
/*
companion object {
    fun logout(context: Context) {
        val prefs = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()

        val intent = Intent(context, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(intent)
    }
}
*/