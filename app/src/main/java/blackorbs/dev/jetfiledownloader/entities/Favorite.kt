package blackorbs.dev.jetfiledownloader.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class Favorite(
    @ColumnInfo(defaultValue = "") var title: String,
    @ColumnInfo(defaultValue = "") var url: String,
    @PrimaryKey(autoGenerate = true) var id: Long = 0L
)