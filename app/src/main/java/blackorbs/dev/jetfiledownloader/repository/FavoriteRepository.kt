package blackorbs.dev.jetfiledownloader.repository

import blackorbs.dev.jetfiledownloader.data.FavoriteDao
import blackorbs.dev.jetfiledownloader.entities.Favorite

class FavoriteRepository(private val dao: FavoriteDao) : BaseFavoriteRepository {

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