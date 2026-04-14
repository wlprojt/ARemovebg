package com.codepillars.removebg


import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ImageRepository()

    private val _selectedImage = MutableStateFlow<Uri?>(null)
    val selectedImage: StateFlow<Uri?> = _selectedImage.asStateFlow()

    private val _resultBytes = MutableStateFlow<ByteArray?>(null)
    val resultBytes: StateFlow<ByteArray?> = _resultBytes.asStateFlow()

    private val _originalBitmap = MutableStateFlow<Bitmap?>(null)
    val originalBitmap: StateFlow<Bitmap?> = _originalBitmap.asStateFlow()

    private val _editedBitmap = MutableStateFlow<Bitmap?>(null)
    val editedBitmap: StateFlow<Bitmap?> = _editedBitmap.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun setOriginalBitmap(bitmap: Bitmap?) {
        _originalBitmap.value = bitmap
    }

    fun setEditedBitmap(bitmap: Bitmap?) {
        _editedBitmap.value = bitmap
    }

    fun setSelectedImage(uri: Uri?) {
        _selectedImage.value = uri
        _resultBytes.value = null
        _editedBitmap.value = null
        _error.value = null
    }

    fun clearAll() {
        _selectedImage.value = null
        _resultBytes.value = null
        _originalBitmap.value = null
        _editedBitmap.value = null
        _error.value = null
        _loading.value = false
    }

    fun removeBackground() {
        val uri = _selectedImage.value ?: return

        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            val result = repository.removeBackground(getApplication(), uri)
            result
                .onSuccess { bytes ->
                    _resultBytes.value = bytes
                }
                .onFailure { throwable ->
                    _error.value = throwable.message ?: "Something went wrong"
                }

            _loading.value = false
        }
    }
}