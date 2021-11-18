package cat.copernic.jmendezv.myfirstfirebaseproject

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase

// https://firebase.google.com/docs/database/android/read-and-write?hl=es#kotlin+ktx

data class Record(val id: Long = System.currentTimeMillis(), var data: String) {
    override fun toString(): String {
        return "['$id': '$data']"
    }
}

class Rtdatabase : AppCompatActivity() {

    private lateinit var tvMostra: TextView
    private lateinit var etDada: EditText
    private lateinit var btModifica: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rtdatabase)

        //val db = Firebase.database
        // Write a message to the database
        // https://demofirebase1-49ea1-default-rtdb.europe-west1.firebasedatabase.app/
        val database = FirebaseDatabase.getInstance("https://demofirebase1-49ea1-default-rtdb.europe-west1.firebasedatabase.app/")
        //val database = Firebase.database
        val myRef = database.getReference("message")

        tvMostra = findViewById(R.id.tvMostra)
        etDada = findViewById(R.id.etDada)
        btModifica = findViewById(R.id.btModifica)

        btModifica.setOnClickListener {
//            val myRef = database.getReference("message")
            val value = etDada.text.toString()
            myRef.setValue(Record(data = value).toString())
            Log.d(TAG, "value modified $value ")
//            btModifica.isEnabled = false
        }

        // Read from the database
        myRef.addValueEventListener(object: ValueEventListener {

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

    }
}

