package blackorbs.dev.jetfiledownloader.ui.download

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.paging.cachedIn
import blackorbs.dev.jetfiledownloader.MainApp
import blackorbs.dev.jetfiledownloader.entities.Download
import blackorbs.dev.jetfiledownloader.entities.Status
import blackorbs.dev.jetfiledownloader.repository.BaseDownloadRepository
import blackorbs.dev.jetfiledownloader.repository.DownloadRepository
import blackorbs.dev.jetfiledownloader.ui.shareFiles
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class DownloadVM(
    private val repo: BaseDownloadRepository
): ViewModel() {

    val downloads = repo.getAll().cachedIn(viewModelScope)
    private val _download = mutableStateOf<Download?>(null)
    val download: State<Download?> = derivedStateOf { _download.value }

    private val _newDownloads = mutableStateListOf<Download>()
    val newDownloads = derivedStateOf {
        _newDownloads.sortedByDescending { it.dateTime }
    }
    private val _selectedDownloads = mutableStateListOf<Download>()
    val selectedItemCount = derivedStateOf { _selectedDownloads.size }

    var isPendingDelete by mutableStateOf(false)
        private set

    var showNotifBox = mutableStateOf(false)

    suspend fun add(download: Download): Download {
        repo.add(download).also { download.id = it }
        _newDownloads.add(download)
        return download
    }

    fun getDownload(id: Long){
        viewModelScope.launch {
            _download.value = repo.get(id)
        }
    }

    private var deleteJob: Job? = null

    fun deleteSelectedItems(){
        isPendingDelete = true
        deleteJob = viewModelScope.launch {
            _selectedDownloads.forEach {
                it.isPendingDelete.value = true
            }
            delay(4000)
            _selectedDownloads.forEach {
                with(it){
                    if(isPendingDelete.value){
                        status.value = Status.Deleted
                        repo.delete(this)
                        with(File(filePath)) {
                           if(exists()) delete()
                        }
                    }
                }
            }
            downloadCountUpdater?.invoke(_selectedDownloads.size)
            _selectedDownloads.clear()
            isPendingDelete = false
        }
    }

    fun shareSelection(context: Context){
        viewModelScope.launch {
            context.shareFiles(
                _selectedDownloads.map { it.filePath }
            )
        }
    }

    fun setSelection(download: Download){
        if(download.isPending || download.isPendingDelete.value)
            return
        if(download.isSelected.value){
            download.isSelected.value = false
            _selectedDownloads.remove(download)
            if(_selectedDownloads.isEmpty()){
                deleteJob?.cancel()
                isPendingDelete = false
            }
        }
        else if(!isPendingDelete){
            download.isSelected.value = true
            _selectedDownloads.add(download)
        }
    }

    private var downloadCountUpdater: ((Int) -> Unit)? = null

    companion object{
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MainApp
                DownloadVM( DownloadRepository(app)).apply {
                    downloadCountUpdater = {
                        app.newDownloadsCount.intValue -= it
                    }
                }
            }
        }
    }

}
