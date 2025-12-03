package com.kmusic.ui.settings

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.kmusic.MainActivity
import com.kmusic.R
import com.kmusic.data.local.ServerConfigManager
import com.kmusic.data.local.ThemePreferenceManager
import com.kmusic.data.local.UserSessionManager
import com.kmusic.databinding.FragmentSettingsBinding
import java.io.File

/**
 * SettingsFragment - Settings screen UI
 *
 * Features:
 * - User info display
 * - Theme selection (Light/Dark/System)
 * - Server configuration
 * - Upload music (admin only)
 * - Logout
 */
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: SettingsViewModel
    private lateinit var userSessionManager: UserSessionManager
    private lateinit var serverConfigManager: ServerConfigManager
    private lateinit var themePreferenceManager: ThemePreferenceManager

    // File picker for music upload
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                // TODO: Handle file upload
                Toast.makeText(requireContext(), "File selected: ${uri.lastPathSegment}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize managers
        userSessionManager = UserSessionManager(requireContext())
        serverConfigManager = ServerConfigManager(requireContext())
        themePreferenceManager = ThemePreferenceManager(requireContext())

        // Initialize ViewModel
        val factory = SettingsViewModelFactory(
            userSessionManager,
            serverConfigManager,
            themePreferenceManager
        )
        viewModel = ViewModelProvider(this, factory)[SettingsViewModel::class.java]

        // Setup UI
        setupClickListeners()
        observeViewModel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Setup click listeners
     */
    private fun setupClickListeners() {
        // Theme selection
        binding.themeSettingLayout.setOnClickListener {
            showThemeSelectionDialog()
        }

        // Server configuration
        binding.serverConfigLayout.setOnClickListener {
            showServerConfigDialog()
        }

        // Upload music (admin only)
        binding.uploadMusicLayout.setOnClickListener {
            showUploadMusicDialog()
        }

        // Logout
        binding.logoutLayout.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    /**
     * Observe ViewModel state
     */
    private fun observeViewModel() {
        // User info
        viewModel.currentUser.observe(viewLifecycleOwner) { user ->
            user?.let {
                binding.userDisplayName.text = it.displayName
                binding.userEmail.text = it.email
                binding.userRole.text = if (it.isAdmin()) "Admin" else "User"

                // Show/hide admin section
                if (it.isAdmin()) {
                    binding.adminSectionTitle.visibility = View.VISIBLE
                    binding.adminCard.visibility = View.VISIBLE
                } else {
                    binding.adminSectionTitle.visibility = View.GONE
                    binding.adminCard.visibility = View.GONE
                }
            }
        }

        // Theme
        viewModel.currentTheme.observe(viewLifecycleOwner) { theme ->
            binding.themeValueText.text = viewModel.getThemeDisplayName()
        }

        // Server address
        viewModel.serverAddress.observe(viewLifecycleOwner) { address ->
            binding.serverAddressText.text = address
        }

        // Logout event
        viewModel.logoutEvent.observe(viewLifecycleOwner) { shouldLogout ->
            if (shouldLogout) {
                Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
                (requireActivity() as MainActivity).navigateToLogin()
            }
        }

        // Error messages
        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    /**
     * Show theme selection dialog
     */
    private fun showThemeSelectionDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_theme_selection, null)
        val radioGroup = dialogView.findViewById<android.widget.RadioGroup>(R.id.themeRadioGroup)

        // Pre-select current theme
        val currentTheme = viewModel.currentTheme.value ?: ThemePreferenceManager.THEME_SYSTEM
        when (currentTheme) {
            ThemePreferenceManager.THEME_LIGHT -> radioGroup.check(R.id.themeLight)
            ThemePreferenceManager.THEME_DARK -> radioGroup.check(R.id.themeDark)
            else -> radioGroup.check(R.id.themeSystem)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Apply") { _, _ ->
                val selectedTheme = when (radioGroup.checkedRadioButtonId) {
                    R.id.themeLight -> ThemePreferenceManager.THEME_LIGHT
                    R.id.themeDark -> ThemePreferenceManager.THEME_DARK
                    else -> ThemePreferenceManager.THEME_SYSTEM
                }
                viewModel.changeTheme(selectedTheme)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Show server configuration dialog
     */
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
                    Toast.makeText(requireContext(), "Server settings saved", Toast.LENGTH_SHORT).show()

                    // Update display
                    val protocol = if (useHttps) "https" else "http"
                    binding.serverAddressText.text = "$protocol://$address:$port"
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Show upload music dialog
     */
    private fun showUploadMusicDialog() {
        if (!viewModel.isAdmin()) {
            Toast.makeText(requireContext(), "Only admins can upload music", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_upload_music, null)
        val selectFileButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.selectFileButton)

        selectFileButton.setOnClickListener {
            openFilePicker()
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Upload Music")
            .setView(dialogView)
            .setPositiveButton("Upload") { _, _ ->
                // TODO: Implement upload logic
                Toast.makeText(requireContext(), "Upload functionality coming soon", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Open file picker for music files
     */
    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "audio/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        filePickerLauncher.launch(Intent.createChooser(intent, "Select Music File"))
    }

    /**
     * Show logout confirmation dialog
     */
    private fun showLogoutConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                viewModel.logout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
