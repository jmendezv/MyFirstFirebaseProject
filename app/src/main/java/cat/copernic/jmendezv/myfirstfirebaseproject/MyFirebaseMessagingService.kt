package cat.copernic.jmendezv.myfirstfirebaseproject

import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

// Las notificacionn llegan en segundo plano
// Si la app esta abierta habra que manejarlas desde codigo
class MyFirebaseMessagingService : FirebaseMessagingService() {


    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

//        Looper.prepare()
        // Handler deprecated
//        Handler().post() {
//            Toast.makeText(baseContext, "Notificacion en primer planao ${message.notification?.title}", Toast.LENGTH_SHORT).show()
//        }
//        Looper.loop()

        Handler(Looper.getMainLooper()).post {
            Toast.makeText(baseContext, "Notificacion en primer planao ${message.notification?.title}", Toast.LENGTH_SHORT).show()
        }
    }

}