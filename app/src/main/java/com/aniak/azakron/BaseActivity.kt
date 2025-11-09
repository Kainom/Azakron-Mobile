package com.aniak.azakron

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.ContextThemeWrapper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder

open class BaseActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient

    protected open val showMenu: Boolean = true

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (showMenu) {
            menuInflater.inflate(R.menu.menu_main, menu)
            if (menu is MenuBuilder) {
                menu.setOptionalIconsVisible(true)
            }
        }
        return showMenu
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_profile -> {
                showProfile()
                true
            }
            R.id.action_settings -> {
                showSettings()
                true
            }
            R.id.action_logout -> {
                showLogoutDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    protected fun setupMenuButton(menuButton: View) {
        menuButton.setOnClickListener { view ->
            showPopupMenu(view)
        }
    }

    private fun showPopupMenu(view: View) {
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.menu_main, popup.menu)

        // Força ícones visíveis
        try {
            val fieldMPopup = PopupMenu::class.java.getDeclaredField("mPopup")
            fieldMPopup.isAccessible = true
            val mPopup = fieldMPopup.get(popup)
            mPopup.javaClass
                .getDeclaredMethod("setForceShowIcon", Boolean::class.java)
                .invoke(mPopup, true)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val menu = popup.menu
            if (menu is MenuBuilder) {
                menu.setOptionalIconsVisible(true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        popup.setOnMenuItemClickListener { item ->
            onOptionsItemSelected(item)
        }

        popup.show()

        // Força o fundo PRETO no PopupWindow
        try {
            val fieldMPopup = PopupMenu::class.java.getDeclaredField("mPopup")
            fieldMPopup.isAccessible = true
            val mPopup = fieldMPopup.get(popup)

            // Tenta pegar o PopupWindow
            val popupField = mPopup.javaClass.getDeclaredField("mPopup")
            popupField.isAccessible = true
            val popupWindow = popupField.get(mPopup) as? android.widget.PopupWindow

            // Cria drawable preto com borda
            val shape = android.graphics.drawable.GradientDrawable()
            shape.setColor(Color.parseColor("#1a1a1a"))
            shape.cornerRadius = 32f
            shape.setStroke(2, Color.parseColor("#333333"))

            popupWindow?.setBackgroundDrawable(shape)

        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Força texto PRETO e ícones PRETOS (já que o fundo vai ser preto, os textos tem que ser brancos)
        // Vamos usar uma abordagem diferente: Handler para aplicar após renderizar
        view.postDelayed({
            try {
                val fieldMPopup = PopupMenu::class.java.getDeclaredField("mPopup")
                fieldMPopup.isAccessible = true
                val mPopup = fieldMPopup.get(popup)
                val listView = mPopup.javaClass.getDeclaredMethod("getListView").invoke(mPopup) as? android.widget.ListView

                listView?.apply {
                    setBackgroundColor(Color.parseColor("#1a1a1a"))

                    // Força cada item a ter texto branco
                    for (i in 0 until childCount) {
                        val item = getChildAt(i)
                        item?.let { view ->
                            // Procura por TextViews dentro do item
                            if (view is android.view.ViewGroup) {
                                for (j in 0 until view.childCount) {
                                    val child = view.getChildAt(j)
                                    if (child is android.widget.TextView) {
                                        child.setTextColor(Color.WHITE)
                                    }
                                    if (child is android.widget.ImageView) {
                                        child.setColorFilter(Color.WHITE)
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, 50)
    }

    private fun showProfile() {
        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        val name = prefs.getString("user_name", "Usuário")
        val email = prefs.getString("user_email", "email@example.com")

        showCustomDialog(
            title = "📋 Perfil",
            message = "👤 Nome: $name\n📧 Email: $email",
            positiveButtonText = "OK"
        )
    }

    private fun showSettings() {
        Toast.makeText(this, "⚙️ Configurações em breve", Toast.LENGTH_SHORT).show()
    }

    private fun showLogoutDialog() {
        showCustomDialog(
            title = "🚪 Sair",
            message = "Deseja realmente sair da sua conta?",
            positiveButtonText = "Sim",
            negativeButtonText = "Cancelar",
            onPositiveClick = {
                performLogout()
            }
        )
    }

    private fun showCustomDialog(
        title: String,
        message: String,
        positiveButtonText: String,
        negativeButtonText: String? = null,
        onPositiveClick: (() -> Unit)? = null
    ) {
        val builder = MaterialAlertDialogBuilder(this, R.style.CustomAlertDialog)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveButtonText) { dialog, _ ->
                onPositiveClick?.invoke()
                dialog.dismiss()
            }

        if (negativeButtonText != null) {
            builder.setNegativeButton(negativeButtonText) { dialog, _ ->
                dialog.dismiss()
            }
        }

        val dialog = builder.create()

        // SOLUÇÃO DEFINITIVA: Força o fundo preto
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.parseColor("#1a1a1a")))
        }

        dialog.show()

        // Força a cor dos botões depois de mostrar
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
            setTextColor(ContextCompat.getColor(context, R.color.primary))
        }

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.apply {
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
        }
    }

    private fun performLogout() {
        setupGoogleSignIn()

        googleSignInClient.signOut().addOnCompleteListener(this) {
            googleSignInClient.revokeAccess().addOnCompleteListener(this) {
                clearUserSession()
                navigateToLogin()
            }
        }
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun clearUserSession() {
        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        prefs.edit().clear().apply()

        Toast.makeText(this, "✅ Logout realizado com sucesso", Toast.LENGTH_SHORT).show()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}