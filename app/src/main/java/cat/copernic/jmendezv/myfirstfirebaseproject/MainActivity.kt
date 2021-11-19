package cat.copernic.jmendezv.myfirstfirebaseproject

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase


//https://github.com/firebase/snippets-android/blob/8184cba2c40842a180f91dcfb4a216e721cc6ae6/auth/app/src/main/java/com/google/firebase/quickstart/auth/kotlin/EmailPasswordActivity.kt#L41-L55

const val TAG = "App"

class MainActivity : AppCompatActivity() {

    private val GOOGLE_SIGN_IN = 100

    private lateinit var auth: FirebaseAuth

    private lateinit var tvTexto: TextView
    private lateinit var btAutentifica: Button
    private lateinit var btAlta: Button
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btRtDatabase: Button
    private lateinit var btGoogleLogin: Button

    val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            onActivityResult(GOOGLE_SIGN_IN, result)
        }//.launch(googleClient.signInIntent)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sessionActiva()

        btAutentifica = findViewById(R.id.btAutentifica)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        tvTexto = findViewById(R.id.tvTexto)
        btAlta = findViewById(R.id.btAlta)
        btGoogleLogin = findViewById(R.id.googleId)

        btRtDatabase = findViewById(R.id.btRtDatabase)

        // Initialize Firebase Auth
        auth = Firebase.auth


        btAutentifica.setOnClickListener {
            btAutentifica.isEnabled = false
            var password = etPassword.text.toString()
            var email = etEmail.text.toString()
            signIn(email.trim(), password.trim())
            //signIn( "pep.mendez@copernic.cat", "copernic")
        }

        btAlta.setOnClickListener {
            btAlta.isEnabled = false
            var password = etPassword.text.toString()
            var email = etEmail.text.toString()
            Log.d(TAG, "email: $email Pass: $password")
            createAccount(email.trim(), password.trim())
            //signIn( "ivan.canton@copernic.cat", "1234321")
        }

        btRtDatabase.setOnClickListener {
            navigateToLogged()
        }

        btGoogleLogin.setOnClickListener {
            googleLogin()
        }
    }

    private fun googleLogin() {
        val googleConf = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.my_default_web_client_id))
            .requestEmail()
            .build()
        val googleClient = GoogleSignIn.getClient(this, googleConf)
        // para salir de la cuenta que podamos estar logeado en ese momento,
        // por si tenemos varias cuentas de google asociadas a nuestro dispositivo de android
        googleClient.signOut()
        // deprecated
        //startActivityForResult(googleClient.signInIntent, GOOGLE_SIGN_IN)
        launcher.launch(googleClient.signInIntent)

    }

    private fun onActivityResult(requestCode: Int, result: ActivityResult) {
        // Login en dos pasos: primero Google, después Firebase
        if (result.resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                GOOGLE_SIGN_IN -> {
                    // utenticacion en google
                    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    try {
                        val account: GoogleSignInAccount = task.getResult(ApiException::class.java)
                        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                        // autenticacion en firebase
                        FirebaseAuth.getInstance().signInWithCredential(credential)
                            .addOnCompleteListener {
                                if (it.isSuccessful) {
                                    showAlert("Google Auth", "Authenticated")
                                    navigateToLogged()
                                } else {
                                    showAlert("Google Auth", "Authentication produjo errores")
                                }
                            }
                    } catch (error: ApiException) {
                        showAlert("Google Auth", error.message!!)
                    }
                }
            }
        }
    }


    private fun navigateToLogged() {
        val intent = Intent(this, Rtdatabase::class.java)
        startActivity(intent)
    }

    // [START on_start_check_user]
    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        if (currentUser != null) {
            reload()
        }
    }
    // [END on_start_check_user]

    private fun signOut() {
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
                    val user: FirebaseUser? = auth.currentUser
                    updateUI(user)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    Toast.makeText(
                        baseContext, "Authentication failed.",
                        Toast.LENGTH_SHORT
                    ).show()
                    updateUI(null)
                }
            }
        // [END create_user_with_email]
    }

    private fun showAlert(title: String, message: String) {
        val dialog = AlertDialog.Builder(this).apply {
            setTitle(title)
            setMessage(message)
            setPositiveButton("Aceptar", null)
        }.create()
        dialog.show()
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
                    val pref = getSharedPreferences(
                        getString(R.string.prefs_filename),
                        Context.MODE_PRIVATE
                    ).edit {
                        putString("email", email)
                        putString("password", password)
                        apply()
                    }
                    updateUI(user)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    Toast.makeText(
                        baseContext, "Authentication failed.",
                        Toast.LENGTH_SHORT
                    ).show()
                    updateUI(null)
                }
            }
        // [END sign_in_with_email]
    }

    private fun sessionActiva() {
        val pref = getSharedPreferences(getString(R.string.prefs_filename), Context.MODE_PRIVATE)
        val email = pref.getString("email", null)
        if (email != null) {
            navigateToLogged()
        }
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) tvTexto.text = user.email
        else tvTexto.text = "sense registrar"
    }

    private fun reload() {

    }

}
