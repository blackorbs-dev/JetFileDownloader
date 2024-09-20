package blackorbs.dev.jetfiledownloader.ui.favorite

import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import blackorbs.dev.jetfiledownloader.entities.Favorite
import blackorbs.dev.jetfiledownloader.repository.BaseFavoriteRepository
import blackorbs.dev.jetfiledownloader.repository.FavoriteRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class FavViewModel(
    private val repository: BaseFavoriteRepository
): ViewModel() {

    private val _favoriteItems = mutableListOf<Favorite>()
    val favoriteItems: State<List<Favorite>> =
        derivedStateOf { _favoriteItems }
    var isFavorite by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            _favoriteItems.addAll(
                repository.getAll()
            )
        }
    }

    private var favJob: Job? = null

    fun setIsFavorite(url: String){
        isFavorite = false
        favJob?.cancel()
        favJob = viewModelScope.launch {
            isFavorite =
                favoriteItems.value.any { it.url == url }
        }
    }

    private var url: String? = null

    fun add(favorite: Favorite){
        if(url == favorite.url)
            return
        url = favorite.url
        isFavorite = !isFavorite
        viewModelScope.launch {
            val index = _favoriteItems.indexOfFirst {
                it.url == favorite.url
            }
            if(index == -1) {
                _favoriteItems.add(0,
                    favorite.apply {
                        id = repository.add(favorite)
                    }
                )
            }
            else delete(_favoriteItems[index])
            url = null
        }
    }

    fun update(favorite: Favorite){
        viewModelScope.launch {
            repository.update(favorite)
            _favoriteItems[_favoriteItems.indexOfFirst {
                it.id == favorite.id
            }] = favorite
        }
    }

    fun delete(favorite: Favorite){
        viewModelScope.launch {
            repository.delete(favorite)
            _favoriteItems.remove(favorite)
        }
    }

    companion object{
        val Factory = viewModelFactory {
            initializer {
                FavViewModel(
                    FavoriteRepository(this[APPLICATION_KEY])
                )
            }
        }
    }

}