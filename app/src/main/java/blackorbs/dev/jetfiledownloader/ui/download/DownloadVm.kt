package blackorbs.dev.jetfiledownloader.ui.download

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.paging.PagingData
import androidx.paging.cachedIn
import blackorbs.dev.jetfiledownloader.entities.Download
import blackorbs.dev.jetfiledownloader.entities.Status
import blackorbs.dev.jetfiledownloader.repository.BaseDownloadRepo
import blackorbs.dev.jetfiledownloader.repository.DownloadRepo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class DownloadVm(private val repo: BaseDownloadRepo): ViewModel() {

    val downloads: Flow<PagingData<Download>> = repo.getAll()
        .distinctUntilChanged()
        .cachedIn(viewModelScope)

    private val _newDownloads = mutableListOf<Download>()
    val newDownloads: State<List<Download>> = derivedStateOf{_newDownloads}

    var showNotifBox = mutableStateOf(true) //TODO: change to false

    suspend fun add(download: Download): Download {
        repo.add(download).also { download.id = it }
        _newDownloads.add(download)
        return download
    }

    fun update(size: Long, id: Long){
        viewModelScope.launch { repo.update(size, id) }
    }

    fun update(status: MutableState<Status>, id: Long){
        viewModelScope.launch { repo.update(status, id) }
    }

    fun delete(download: Download){
        viewModelScope.launch { repo.delete(download) }
    }

    companion object{
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                DownloadVm( DownloadRepo(this[APPLICATION_KEY]) )
            }
        }
    }

}
