package app.zerno

import android.app.Application
import app.zerno.di.initKoin
import org.koin.android.ext.koin.androidContext

class ZernoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@ZernoApplication)
        }
    }
}
