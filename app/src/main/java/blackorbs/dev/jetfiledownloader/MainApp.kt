package blackorbs.dev.jetfiledownloader

import android.app.Application
import androidx.room.Room
import blackorbs.dev.jetfiledownloader.data.Database
import blackorbs.dev.jetfiledownloader.entities.Download

class MainApp: Application() {
    lateinit var database: Database
    var addNum = 0
    val ongoingDownloads = mutableListOf<Download>()
    val errorDownloads = mutableListOf<Download>()

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            this, klass = Database::class.java, "app_data"
        ).build()
        TimberLogger.init()
    }
}