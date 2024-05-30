package ro.ase.ism.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androiddevs.mvvmnewsapp.api.RetrofitInstance
import kotlinx.coroutines.launch
import ro.ase.ism.network.KeyManager

class MainViewModel : ViewModel() {

    val publicKeyLiveData = MutableLiveData<String>()
    val resultLiveData = MutableLiveData<Boolean>()
    private val keyManager = KeyManager

    fun getPublicKey() {
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.getPublicKey()
                if (response.isSuccessful && response.body() != null) {
                publicKeyLiveData.postValue(response.body())
            } else {
                // Post error message or handle error state
                publicKeyLiveData.postValue("Error: ${response.errorBody()?.string()}")
            }
            } catch (e: Exception) {
                publicKeyLiveData.postValue("Exception: ${e.message}")
            }
        }
    }

    fun postPublicKey(publicKey: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.postPublicKey(publicKey)
                resultLiveData.postValue(response.isSuccessful)
            } catch (e: Exception) {
                resultLiveData.postValue(false)
            }
        }
    }

    fun postMessage(message: String) {
        viewModelScope.launch {
            try {
                val response = keyManager.instance?.encryptMessage(message)
                    ?.let { RetrofitInstance.api.postMessage(it) }
                resultLiveData.postValue(response?.isSuccessful)
            } catch (e: Exception) {
                resultLiveData.postValue(false)
            }
        }
    }
}