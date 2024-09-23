package blackorbs.dev.jetfiledownloader.fakes

import blackorbs.dev.jetfiledownloader.data.FavoriteDao
import blackorbs.dev.jetfiledownloader.entities.Favorite

class FakeFavoriteDao: FavoriteDao {
    private val favorites = mutableListOf<Favorite>()

    override suspend fun add(favorite: Favorite): Long {
        favorites.indexOf(favorite).let {
            when(it){
                -1 -> favorites.add(favorite.apply {
                    id = favorites.size.toLong()
                })
                else -> favorites[it] = favorite
            }
        }
        return favorite.id
    }

    override suspend fun update(favorite: Favorite) {
        favorites[favorite.id.toInt()] = favorite
    }

    override suspend fun delete(favorite: Favorite) {
        favorites.remove(favorite)
    }

    override suspend fun getAll(): List<Favorite> = favorites
}