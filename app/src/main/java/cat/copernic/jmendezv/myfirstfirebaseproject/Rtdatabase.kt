package cat.copernic.jmendezv.myfirstfirebaseproject

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.facebook.login.LoginManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.database.*
import com.google.firebase.database.ktx.getValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig

// https://firebase.google.com/docs/database/android/read-and-write?hl=es#kotlin+ktx

data class Record(val id: Long = System.currentTimeMillis(), var data: String) {
    override fun toString(): String {
        return "['$id': '$data']"
    }
}

/*
* Backend as a service
*
* Rules are JSON style entries with key-value pairs
* Keys being permissions such as read and write
* values being conditions.
* Only true results allow permission.
*
* There are predifined properties:
*
* data: the data that is in the db
*
* newData: the data that is about to be entered
*
* auth: the auth info of the user trying to access the db
*
* now: current datetime
*
* */

/*
{
  "rules": {
    ".read": "false",  // 2021-12-18
    ".write": "false",  // 2021-12-18
    'node_name' : {
        ".read": "auth.uid != null",  // 2021-12-18
        ".write": "now < 1639782000000",  // 2021-12-18
    }
  }
}
 */
class Rtdatabase : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    private lateinit var tvMostra: TextView
    private lateinit var etDada: EditText
    private lateinit var btModifica: Button
    private lateinit var btError: Button
    private lateinit var provider: Provider
    private lateinit var addressEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var getButton: Button
    private lateinit var deleteButton: Button
    private lateinit var user: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rtdatabase)

        //val db = Firebase.database
        // Write a message to the database
        // https://demofirebase1-49ea1-default-rtdb.europe-west1.firebasedatabase.app/
        val database =
            FirebaseDatabase.getInstance("https://demofirebase1-49ea1-default-rtdb.europe-west1.firebasedatabase.app/")
        //val database = Firebase.database
        val myRef = database.getReference("message")

        provider =
            Provider.valueOf(intent.extras?.getString("provider")!!)
        tvMostra = findViewById(R.id.tvMostra)
        etDada = findViewById(R.id.etDada)
        btModifica = findViewById(R.id.btModifica)
        btError = findViewById(R.id.btError)
        addressEditText = findViewById(R.id.addressEditText)
        phoneEditText = findViewById(R.id.phoneEditText)
        saveButton = findViewById(R.id.saveButton)
        getButton = findViewById(R.id.getButton)
        deleteButton = findViewById(R.id.deleteButton)
        user = intent.getStringExtra("user") ?: "unknown user"

        setup(myRef)

        // Remote config
        Firebase.remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val showErrorButton = Firebase.remoteConfig.getBoolean("show_error_button")
                val errorButtonText = Firebase.remoteConfig.getString("error_button_text")
                if (showErrorButton) {
                    btError.visibility = View.VISIBLE
                }
                btError.text = errorButtonText
            }
        }

    }

    private fun setup(myRef: DatabaseReference) {

        btModifica.setOnClickListener {
//            val myRef = database.getReference("message")
            val value = etDada.text.toString()
            myRef.setValue(Record(data = value).toString())
            Log.d(TAG, "value modified $value ")
//            btModifica.isEnabled = false

        }

        btError.visibility = View.INVISIBLE

        btError.setOnClickListener {

            FirebaseCrashlytics.getInstance().setUserId("0xCAFEBABE")
            FirebaseCrashlytics.getInstance().setCustomKey("key1", true)
            FirebaseCrashlytics.getInstance().setCustomKey("Provider", provider.name)

            FirebaseCrashlytics.getInstance().log("solved crash")

//            throw IllegalAccessError("Core is not responding...")
//            Thread.sleep(Long.MAX_VALUE)
        }

        // Read from the database
        myRef.addValueEventListener(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                val value = snapshot.getValue<String>()
                Log.d(TAG, "Value is: $value")
                tvMostra.text = value
                btModifica.isEnabled = true
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Failed to read value.", error.toException())
                tvMostra.text = "error de lectura"
                btModifica.isEnabled = true
            }
        })

        saveButton.setOnClickListener {
            db.collection("users").document(user).set(
                hashMapOf(
                    "provider" to provider.name,
                    "address" to addressEditText.text.toString(),
                    "phone" to phoneEditText.text.toString()
                )
            )
            Toast.makeText(this, "saved", Toast.LENGTH_SHORT).show()
        }

        getButton.setOnClickListener {
            db.collection("users").document(user).get().addOnSuccessListener {
                addressEditText.setText(it.get("address") as String?)
                phoneEditText.setText(it.get("phone") as String?)
            }
        }

        deleteButton.setOnClickListener {
            db.collection("users").document(user).delete()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        logOut()
    }

    private fun logOut() {
        val pref =
            getSharedPreferences(getString(R.string.prefs_filename), Context.MODE_PRIVATE).edit {
                clear()
                apply()
            }

        if (provider == Provider.FACEBOOK) {
            LoginManager.getInstance().logOut()
        }

        FirebaseAuth.getInstance().signOut()
        onBackPressed()
    }
}

