package blackorbs.dev.jetfiledownloader.repository

import android.app.Application
import blackorbs.dev.jetfiledownloader.MainApp
import blackorbs.dev.jetfiledownloader.entities.Favorite

class FavoriteRepository(app: Application?) : BaseFavoriteRepository {

    private val dao = (app as MainApp).favoriteDao

    override suspend fun getAll() = dao.getAll()

    override suspend fun add(favorite: Favorite) = dao.add(favorite)

    override suspend fun update(favorite: Favorite) = dao.update(favorite)

    override suspend fun delete(favorite: Favorite) = dao.delete(favorite)
}

interface BaseFavoriteRepository {
    suspend fun getAll(): List<Favorite>

    suspend fun add(favorite: Favorite): Long

    suspend fun update(favorite: Favorite)

    suspend fun delete(favorite: Favorite)
}