package blackorbs.dev.jetfiledownloader.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import blackorbs.dev.jetfiledownloader.entities.Converter
import blackorbs.dev.jetfiledownloader.entities.Download
import blackorbs.dev.jetfiledownloader.entities.Favorite

@Database(
    entities =
    [Download::class, Favorite::class],
    version = 2,
)
@TypeConverters(Converter::class)
abstract class Database: RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
    abstract fun favoriteDao(): FavoriteDao
}