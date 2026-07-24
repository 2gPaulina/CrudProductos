package com.example.crudproductos

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private var tokenJwtGuardado: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Inicializar Firebase Auth
        auth = FirebaseAuth.getInstance()

        val etCorreo = findViewById<EditText>(R.id.etCorreo)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        btnLogin.setOnClickListener {
            val correo = etCorreo.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (correo.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor llena todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 1. PASO MANDATORIO: Autenticar primero con Firebase Auth
            auth.signInWithEmailAndPassword(correo, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Firebase Auth: Éxito", Toast.LENGTH_SHORT).show()

                        // 2. PASO JWT: Solicitar el token JWT firmado a nuestro FastAPI
                        intercambiarTokenConFastAPI(correo)
                    } else {
                        Toast.makeText(this, "Error en Firebase: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }

    private fun intercambiarTokenConFastAPI(correo: String) {
        val requestData = LoginRequest(firebase_token = "token_desde_android", correo = correo)

        RetrofitClient.instance.loginUsuario(requestData).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val loginResponse = response.body()!!

                    tokenJwtGuardado = loginResponse.token_jwt

                    // Convertimos el rol a minúsculas para evitar problemas de mayúsculas ("Empleado" vs "empleado")
                    val rolUsuario = loginResponse.usuario.rol.lowercase().trim()

                    // 1. Guardamos el Token en SharedPreferences para la pantalla de Usuario
                    val prefs = getSharedPreferences("MisPreferencias", Context.MODE_PRIVATE)
                    prefs.edit().putString("TOKEN", tokenJwtGuardado).apply()

                    Toast.makeText(this@LoginActivity, "JWT Recibido. Rol: $rolUsuario", Toast.LENGTH_LONG).show()

                    // 2. DECISIÓN DE PANTALLA SEGÚN EL ROL
                    val intent: Intent = when (rolUsuario) {
                        "empleado", "usuario" -> {
                            // Si es un empleado o usuario estándar, va al Punto de Venta
                            Intent(this@LoginActivity, Usuario::class.java)
                        }
                        "admin", "encargado" -> {
                            // Si es administrador o encargado, va al CRUD completo
                            Intent(this@LoginActivity, ProductosActivity::class.java)
                        }
                        else -> {
                            // Si el rol es desconocido, por defecto abrimos la pantalla básica de usuario
                            Intent(this@LoginActivity, Usuario::class.java)
                        }
                    }

                    // Le pasamos también el Token JWT por Intent por si lo utilizas directamente en la Activity
                    intent.putExtra("TOKEN_JWT", tokenJwtGuardado)

                    // Arrancamos la pantalla correspondiente
                    startActivity(intent)

                    // Cerramos el Login
                    finish()
                } else {
                    Toast.makeText(this@LoginActivity, "FastAPI rechazó la solicitud", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                Toast.makeText(this@LoginActivity, "Fallo de red: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }
}