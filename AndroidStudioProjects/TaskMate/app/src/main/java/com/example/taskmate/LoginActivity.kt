package com.example.taskmate

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.taskmate.storage.TaskStorage
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val storage = TaskStorage(this)
        val auth = FirebaseAuth.getInstance()

        setContent {
            var email by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }
            var isLoading by remember { mutableStateOf(false) }
            var errorMessage by remember { mutableStateOf<String?>(null) }

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "TaskMate Login", style = MaterialTheme.typography.headlineMedium)
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        errorMessage?.let {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = it, color = Color.Red, style = MaterialTheme.typography.bodySmall)
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        if (isLoading) {
                            CircularProgressIndicator()
                        } else {
                            Button(
                                onClick = {
                                    errorMessage = null
                                    if (email.isBlank() || password.isBlank()) {
                                        errorMessage = "Please fill all fields"
                                        return@Button
                                    }
                                    
                                    // Fallback for demo/admin login if needed, or just pure Firebase
                                    if (email == "admin" && password == "1234") {
                                        storage.saveLoginState(true)
                                        startActivity(Intent(this@LoginActivity, TaskActivity::class.java))
                                        finish()
                                        return@Button
                                    }

                                    isLoading = true
                                    auth.signInWithEmailAndPassword(email, password)
                                        .addOnCompleteListener { task ->
                                            isLoading = false
                                            if (task.isSuccessful) {
                                                storage.saveLoginState(true)
                                                startActivity(Intent(this@LoginActivity, TaskActivity::class.java))
                                                finish()
                                            } else {
                                                errorMessage = task.exception?.localizedMessage ?: "Login failed"
                                            }
                                        }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Login")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        TextButton(onClick = {
                            startActivity(Intent(this@LoginActivity, RegisterActivity::class.java))
                        }) {
                            Text("Don't have an account? Sign Up")
                        }
                    }
                }
            }
        }
    }
}
