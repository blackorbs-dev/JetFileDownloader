package blackorbs.dev.jetfiledownloader.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import blackorbs.dev.jetfiledownloader.entities.Favorite

@Dao
interface FavoriteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(favorite: Favorite): Long

    @Update
    suspend fun update(favorite: Favorite)

    @Delete
    suspend fun delete(favorite: Favorite)

    @Query("SELECT * FROM favorites ORDER BY id DESC")
    suspend fun getAll(): List<Favorite>

}