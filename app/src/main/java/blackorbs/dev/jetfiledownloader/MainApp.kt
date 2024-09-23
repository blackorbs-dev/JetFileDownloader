package blackorbs.dev.jetfiledownloader

import android.app.Application

open class MainApp: Application() {

    open val appModule: BaseAppModule by lazy {
        AppModule(this)
    }

    override fun onCreate() {
        super.onCreate()
        TimberLogger.init()
    }
}