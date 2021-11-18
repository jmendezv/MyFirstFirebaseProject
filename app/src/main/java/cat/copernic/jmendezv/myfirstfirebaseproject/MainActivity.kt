package cat.copernic.jmendezv.myfirstfirebaseproject

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import cat.copernic.jmendezv.myfirstfirebaseproject.R
import cat.copernic.jmendezv.myfirstfirebaseproject.Rtdatabase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase


//https://github.com/firebase/snippets-android/blob/8184cba2c40842a180f91dcfb4a216e721cc6ae6/auth/app/src/main/java/com/google/firebase/quickstart/auth/kotlin/EmailPasswordActivity.kt#L41-L55

const val TAG = "App"

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var tvTexto: TextView
    private lateinit var btAutentifica: Button
    private lateinit var btAlta: Button
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btRtDatabase: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btAutentifica = findViewById(R.id.btAutentifica)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        tvTexto = findViewById(R.id.tvTexto)
        btAlta = findViewById(R.id.btAlta)

        btRtDatabase = findViewById(R.id.btRtDatabase)

        // Initialize Firebase Auth
        auth = Firebase.auth

        btAutentifica.setOnClickListener{
            btAutentifica.isEnabled = false
            var password = etPassword.text.toString()
            var email = etEmail.text.toString()
            signIn( email.trim(), password.trim())
            //signIn( "pep.mendez@copernic.cat", "copernic")
        }

        btAlta.setOnClickListener{
            btAlta.isEnabled = false
            var password = etPassword.text.toString()
            var email = etEmail.text.toString()
            Log.d( TAG, "email: $email Pass: $password")
            createAccount( email.trim(), password.trim())
            //signIn( "ivan.canton@copernic.cat", "1234321")
        }

        btRtDatabase.setOnClickListener {
            val intent = Intent( this, Rtdatabase::class.java)
            startActivity(intent)
        }
    }

    // [START on_start_check_user]
    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        if(currentUser != null){
            reload()
        }
    }
    // [END on_start_check_user]

    private fun signOut(){
        Firebase.auth.signOut()
    }

    private fun createAccount(email: String, password: String) {
        // [START create_user_with_email]
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                btAlta.isEnabled = true
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "createUserWithEmail:success")
                    val user = auth.currentUser
                    updateUI(user)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    Toast.makeText(baseContext, "Authentication failed.",
                        Toast.LENGTH_SHORT).show()
                    updateUI(null)
                }
            }
        // [END create_user_with_email]
    }

    private fun signIn(email: String, password: String) {
        // [START sign_in_with_email]
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                btAutentifica.isEnabled = true
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithEmail:success")
                    val user = auth.currentUser
                    updateUI(user)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    Toast.makeText(baseContext, "Authentication failed.",
                        Toast.LENGTH_SHORT).show()
                    updateUI(null)
                }
            }
        // [END sign_in_with_email]
    }

    private fun updateUI(user: FirebaseUser?) {
        if( user != null ) tvTexto.text = user.email
        else tvTexto.text = "sense registrar"
    }

    private fun reload() {

    }

}
