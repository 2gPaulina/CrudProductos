package com.example.crudproductos

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private var tokenJwtGuardado: String? = null // Aquí guardaremos el JWT para usar el CRUD

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login) // <-- Verifica que coincida con tu XML

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
                        // Firebase dice que el usuario y contraseña son correctos
                        Toast.makeText(this, "Firebase Auth: Éxito", Toast.LENGTH_SHORT).show()

                        // 2. PASO JWT: Solicitar el token JWT firmado a nuestro FastAPI
                        intercambiarTokenConFastAPI(correo)
                    } else {
                        // Error de credenciales en Firebase
                        Toast.makeText(this, "Error en Firebase: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }

    private fun intercambiarTokenConFastAPI(correo: String) {
        // Creamos la petición para nuestro servidor FastAPI (Mandamos un token de prueba y el correo)
        val requestData = LoginRequest(firebase_token = "token_desde_android", correo = correo)

        RetrofitClient.instance.loginUsuario(requestData).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val loginResponse = response.body()!!

                    // ¡GUARDAMOS EL TOKEN JWT SOBERANO!
                    tokenJwtGuardado = loginResponse.token_jwt
                    val rolUsuario = loginResponse.usuario.rol

                    Toast.makeText(this@LoginActivity, "JWT Recibido. Rol: $rolUsuario", Toast.LENGTH_LONG).show()

                    val intent = android.content.Intent(this@LoginActivity, ProductosActivity::class.java)

                    // Le pasamos el Token JWT de alta seguridad a la siguiente pantalla para que pueda hacer el CRUD
                    intent.putExtra("TOKEN_JWT", tokenJwtGuardado)

                    // Arrancamos la pantalla de productos
                    startActivity(intent)

                    // Cerramos el Login para que si el usuario le pica al botón de "Atrás" en el cel, no se regrese aquí
                    finish()
                } else {
                    Toast.makeText(this@LoginActivity, "FastAPI rechazó la solicitud", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                Toast.makeText(this@LoginActivity, "Fallo de red: ${t.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        })
    }
}