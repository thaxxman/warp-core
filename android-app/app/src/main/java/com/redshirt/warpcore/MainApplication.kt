package com.redshirt.warpcore

import android.app.Application
import com.redshirt.warpcore.data.AppDatabase

class MainApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
}