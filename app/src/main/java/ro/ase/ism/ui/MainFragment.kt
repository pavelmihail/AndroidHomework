package ro.ase.ism.ui

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.Observer
import ro.ase.ism.R
import ro.ase.ism.databinding.FragmentMainBinding
import ro.ase.ism.network.KeyManager

class MainFragment : Fragment() {

    private lateinit var viewModel: MainViewModel
    private lateinit var binding: FragmentMainBinding
    private val keyManager = KeyManager
    private var publicKey: String? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        viewModel.publicKeyLiveData.observe(viewLifecycleOwner) { publicKey ->
            binding.textViewKey.text = publicKey ?: "Failed to load key."
        }

        viewModel.resultLiveData.observe(viewLifecycleOwner) { isSuccess ->
            if (isSuccess) {
                Toast.makeText(context, "Operation Successful", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Operation Failed", Toast.LENGTH_SHORT).show()
            }
        }

        binding.buttonGetKey.setOnClickListener {
            publicKey = viewModel.getPublicKey().toString()
            binding.textViewKey.text = (KeyManager.instance?.generateDerivedKey(publicKey!!) ?: "").toString()
        }

        binding.buttonPostKey.setOnClickListener {
            val key = publicKey ?: ""
            viewModel.postPublicKey(key)
        }

        binding.buttonSendMessage.setOnClickListener {
            val message = binding.editMessage.text.toString()
            if (message.isNotEmpty()) {
                viewModel.postMessage(message)
            } else {
                Toast.makeText(context, "Message cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }
    }
}