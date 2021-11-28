package cat.copernic.jmendezv.myfirstfirebaseproject

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.facebook.login.LoginManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.database.*
import com.google.firebase.database.ktx.getValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

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
    private val firebaseStorage: FirebaseStorage = Firebase.storage

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
    private lateinit var selectImage: Button
    private lateinit var makeImage: Button
    private lateinit var uploadImage: Button
    private lateinit var user: String
    private var imageUri: Uri? = null
    private val imageView by lazy { findViewById<ImageView>(R.id.imageView) }
    private val progressBar by lazy { findViewById<ProgressBar>(R.id.progressBar) }
    private lateinit var storageReference: StorageReference

    private val takeImageResult: ActivityResultLauncher<Uri> = registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->
        if (isSuccess) {
            latestTmpUri?.let { uri ->
                imageUri = uri
                imageView.setImageURI(uri)
            }
        }
    }

    private val selectImageFromGalleryResult: ActivityResultLauncher<String> = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { imageUri = uri; imageView.setImageURI(uri) }
    }

    // Single Permission Contract
    private val askLocationPermission: ActivityResultLauncher<String> = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if(isGranted){
            Log.e("TAG", "Location permnission granted")
        }else{
            Log.e("TAG", "Location permnission denied")
        }
    }

    private val askMultiplePermissions: ActivityResultLauncher<Array<String>> = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { map: Map<String, Boolean> ->
        for (entry in map.entries)
        {
            Toast.makeText(this, "${entry.key} = ${entry.value}", Toast.LENGTH_SHORT).show()
        }
    }


//    val startForResult: ActivityResultLauncher<String> = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
//        imageUri = uri
//        imageView.setImageURI(uri)
//    }


    // Receiver
//    private lateinit var getResult: ActivityResultLauncher<*>


    private var latestTmpUri: Uri? = null


    lateinit var observer : MyLifecycleObserver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rtdatabase)

        observer = MyLifecycleObserver(activityResultRegistry)

        lifecycle.addObserver(observer)

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
        selectImage = findViewById(R.id.selectImage)
        makeImage = findViewById(R.id.makeImage)
        uploadImage = findViewById(R.id.uploadImage)
        //imageView = findViewById(R.id.imageView)
        progressBar.setProgressTintList(ColorStateList.valueOf(Color.RED))

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

    private fun askSinglePermission() {
        askLocationPermission.launch (android.Manifest.permission.ACCESS_FINE_LOCATION)
    }

    // ActivityOptionsCompat for transition animations
    private fun askMultiplePermissions() {
        askMultiplePermissions.launch(arrayOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.BLUETOOTH
        ), ActivityOptionsCompat.makeBasic())
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

        selectImage.setOnClickListener {
            //selectImg()
            //selectImageFromGallery()
            observer.selectImage()

        }

        makeImage.setOnClickListener {
            takeImage()
        }

        uploadImage.setOnClickListener {
            uploadImg()
        }

        selectImage.isEnabled = false

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 111)
        } else {
            selectImage.isEnabled = true
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 111 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            selectImage.isEnabled = true
        }
    }

    private fun uploadImg() {
        if (imageUri == null) {
            Toast.makeText(this, "Image is null", Toast.LENGTH_SHORT).show()
            return
        }
        // User progress bar instead
//        val progressDialog = ProgressDialog(this).apply {
//            setTitle("Uploading file ...")
//        }

//        progressDialog.show()
        progressBar.setVisibility(View.VISIBLE)
//        getWindow().setFlags(
//            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
//            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        val formatter = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss")
        val fileName = formatter.format(Date())
        storageReference = FirebaseStorage.getInstance().getReference("images/$fileName")
        storageReference.putFile(imageUri!!).addOnSuccessListener {
            Toast.makeText(this, "Image uploaded", Toast.LENGTH_SHORT).show()
//            if (progressDialog.isShowing) progressDialog.dismiss()
            progressBar.setVisibility(View.INVISIBLE)
//            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        }.addOnFailureListener {
//            if (progressDialog.isShowing) progressDialog.dismiss()
            progressBar.setVisibility(View.INVISIBLE);
//            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            Toast.makeText(this, "Image error", Toast.LENGTH_SHORT).show()
        }
    }

    private fun selectImg() {

        observer.selectImage()

//        val intent = Intent().apply {
//            setType("image/*")
//            setAction(Intent.ACTION_GET_CONTENT)
//        }
//        startActivityForResult(intent, 100)

//        val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
//        { result: ActivityResult ->
//            if (result.resultCode == Activity.RESULT_OK) {
//                //  you will get result here in result.data
//            }
//
//        }


//        startForResult.launch("image/*")

//        startForResult.launch(Intent(activity, CameraCaptureActivity::class.java))
    }

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        if (requestCode == 100 && data != null && data.data != null) {
//            imageUri =  data.data as Uri
//            imageView.setImageURI(imageUri)
//
//        }
//    }

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
//        onBackPressed()
    }

    private fun takeImage() {
        lifecycleScope.launchWhenStarted {
            getTmpFileUri().let { uri ->
                latestTmpUri = uri
                takeImageResult.launch(uri)
            }
        }
    }

    private fun selectImageFromGallery() = selectImageFromGalleryResult.launch("image/*")

    private fun getTmpFileUri(): Uri {
        val tmpFile = File.createTempFile("tmp_image_file", ".png", cacheDir).apply {
            createNewFile()
            deleteOnExit()
        }

        return FileProvider.getUriForFile(applicationContext, "${BuildConfig.APPLICATION_ID}.provider", tmpFile)
    }

    inner class MyLifecycleObserver(private val registry : ActivityResultRegistry)
        : DefaultLifecycleObserver {

        lateinit var getContent : ActivityResultLauncher<String>

        override fun onCreate(owner: LifecycleOwner) {
            getContent = registry.register("key", owner, ActivityResultContracts.GetContent()) { uri: Uri? ->
                // Handle the returned Uri
                imageUri = uri
                imageView.setImageURI(uri)
            }
        }

        fun selectImage() {
            getContent.launch("image/*")
        }
    }

}


//class SimpleContract : ActivityResultContract<Integer, String?>() {
//
//    override fun createIntent(context: Context, input: Integer): Intent {
//        var intent = Intent(Intent.ACTION_GET_CONTENT)
//        intent.putExtra("anyKey", input)
//        return intent
//    }
//
//    override fun parseResult(resultCode: Int, intent: Intent?): String? = when {
//        resultCode != Activity.RESULT_OK -> null      // Return null, if action is cancelled
//        else -> intent?.getStringExtra("data")        // Return the data
//    }
//
//}