package com.kmusic.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kmusic.MainActivity
import com.google.android.material.textfield.TextInputEditText
import com.kmusic.R
import com.kmusic.data.local.ServerConfigManager
import com.kmusic.data.local.UserSessionManager
import com.kmusic.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: LoginViewModel
    private lateinit var sessionManager: UserSessionManager
    private lateinit var serverConfigManager: ServerConfigManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize managers
        sessionManager = UserSessionManager(requireContext())
        serverConfigManager = ServerConfigManager(requireContext())

        // Initialize ViewModel
        viewModel = ViewModelProvider(
            this,
            LoginViewModelFactory(sessionManager, serverConfigManager)
        )[LoginViewModel::class.java]

        setupUI()
        observeViewModel()

        // Check if already logged in
        if (sessionManager.isLoggedIn()) {
            navigateToHome()
        }
    }

    private fun setupUI() {
        // Login button click
        binding.loginButton.setOnClickListener {
            val username = binding.usernameInput.text.toString()
            val password = binding.passwordInput.text.toString()
            viewModel.login(username, password)
        }

        // Register button click
        binding.registerButton.setOnClickListener {
            showRegisterDialog()
        }

        // Server settings button
        binding.serverSettingsButton.setOnClickListener {
            showServerConfigDialog()
        }

        // Handle IME action (keyboard "Done" button)
        binding.passwordInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.loginButton.performClick()
                true
            } else {
                false
            }
        }

        // Show server address if configured
        if (serverConfigManager.isConfigured()) {
            val serverInfo = "${serverConfigManager.getServerAddress()}:${serverConfigManager.getPort()}"
            binding.serverSettingsButton.text = "Server: $serverInfo"
        }
    }

    private fun observeViewModel() {
        viewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthState.Idle -> {
                    setLoading(false)
                }
                is AuthState.Loading -> {
                    setLoading(true)
                }
                is AuthState.Success -> {
                    setLoading(false)
                    val user = state.response.user
                    val welcomeMessage = if (user.isAdmin()) {
                        "Welcome back, Admin ${user.displayName}!"
                    } else {
                        "Welcome back, ${user.displayName}!"
                    }
                    Toast.makeText(requireContext(), welcomeMessage, Toast.LENGTH_SHORT).show()
                    navigateToHome()
                }
                is AuthState.Error -> {
                    setLoading(false)
                    showError(state.message)
                }
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.loginButton.isEnabled = !loading
        binding.registerButton.isEnabled = !loading
        binding.serverSettingsButton.isEnabled = !loading
        binding.usernameInput.isEnabled = !loading
        binding.passwordInput.isEnabled = !loading
    }

    private fun showError(message: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showServerConfigDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_server_config, null)
        val addressInput = dialogView.findViewById<TextInputEditText>(R.id.serverAddressInput)
        val portInput = dialogView.findViewById<TextInputEditText>(R.id.portInput)
        val httpsSwitch = dialogView.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.httpsSwitch)

        // Pre-fill with current values
        addressInput.setText(serverConfigManager.getServerAddress())
        portInput.setText(serverConfigManager.getPort().toString())
        httpsSwitch.isChecked = serverConfigManager.useHttps()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Server Configuration")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val address = addressInput.text.toString()
                val port = portInput.text.toString().toIntOrNull() ?: 8080
                val useHttps = httpsSwitch.isChecked

                if (address.isNotBlank()) {
                    serverConfigManager.saveServerConfig(address, port, useHttps)
                    binding.serverSettingsButton.text = "Server: $address:$port"
                    Toast.makeText(requireContext(), "Server configured", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Please enter server address", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRegisterDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_register, null)
        val usernameInput = dialogView.findViewById<TextInputEditText>(R.id.registerUsernameInput)
        val emailInput = dialogView.findViewById<TextInputEditText>(R.id.registerEmailInput)
        val displayNameInput = dialogView.findViewById<TextInputEditText>(R.id.registerDisplayNameInput)
        val passwordInput = dialogView.findViewById<TextInputEditText>(R.id.registerPasswordInput)
        val confirmPasswordInput = dialogView.findViewById<TextInputEditText>(R.id.registerConfirmPasswordInput)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Create Account")
            .setView(dialogView)
            .setPositiveButton("Register") { _, _ ->
                val username = usernameInput.text.toString()
                val email = emailInput.text.toString()
                val displayName = displayNameInput.text.toString()
                val password = passwordInput.text.toString()
                val confirmPassword = confirmPasswordInput.text.toString()

                if (password != confirmPassword) {
                    Toast.makeText(requireContext(), "Passwords don't match", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                viewModel.register(username, email, password, displayName)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun navigateToHome() {
        (requireActivity() as MainActivity).navigateToHome()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// ViewModel Factory
class LoginViewModelFactory(
    private val sessionManager: UserSessionManager,
    private val serverConfigManager: ServerConfigManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            return LoginViewModel(sessionManager, serverConfigManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
