package com.learneverywhere.app

import android.app.Application
import com.google.firebase.FirebaseApp

class LearnEverywhereApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // No generated Firebase resources means local-only mode; no AI initialization is needed.
        val firebaseApp = FirebaseApp.initializeApp(this) ?: return
        // Application startup precedes Activity creation and the first translation request.
        installFirebaseAppCheck(firebaseApp)
    }
}
