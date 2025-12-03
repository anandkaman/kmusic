package com.kmusic

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.kmusic.data.local.ThemePreferenceManager
import com.kmusic.data.local.UserSessionManager
import com.kmusic.data.model.Track
import com.kmusic.ui.auth.LoginFragment
import com.kmusic.ui.home.HomeFragment
import com.kmusic.ui.player.PlayerFragment
import com.kmusic.ui.settings.SettingsFragment

/**
 * Main Activity - Entry point of the KMusic app
 *
 * Handles:
 * - Theme initialization
 * - Initial fragment setup (Login or Home based on session)
 * - Fragment navigation container
 */
class MainActivity : AppCompatActivity() {

    private lateinit var sessionManager: UserSessionManager
    private lateinit var themePreferenceManager: ThemePreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize managers
        sessionManager = UserSessionManager(this)
        themePreferenceManager = ThemePreferenceManager(this)

        // Set theme based on user preference
        initializeTheme()

        setContentView(R.layout.activity_main)

        // Load initial fragment if this is first creation
        if (savedInstanceState == null) {
            loadInitialFragment()
        }
    }

    /**
     * Initialize app theme based on user preferences
     */
    private fun initializeTheme() {
        themePreferenceManager.applyTheme()
    }

    /**
     * Load the appropriate initial fragment based on login state
     */
    private fun loadInitialFragment() {
        val fragment = if (sessionManager.isLoggedIn()) {
            HomeFragment()
        } else {
            LoginFragment()
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    /**
     * Navigate to home screen (called after successful login)
     */
    fun navigateToHome() {
        val fragment = HomeFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    /**
     * Navigate to player screen with a single track
     */
    fun navigateToPlayer(track: Track) {
        val fragment = PlayerFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()

        // Wait for fragment to be attached, then play track
        supportFragmentManager.executePendingTransactions()
        (supportFragmentManager.findFragmentById(R.id.fragment_container) as? PlayerFragment)
            ?.playTrack(track)
    }

    /**
     * Navigate to player screen with a queue of tracks
     */
    fun navigateToPlayer(tracks: List<Track>, startIndex: Int = 0) {
        val fragment = PlayerFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()

        // Wait for fragment to be attached, then play tracks
        supportFragmentManager.executePendingTransactions()
        (supportFragmentManager.findFragmentById(R.id.fragment_container) as? PlayerFragment)
            ?.playTracks(tracks, startIndex)
    }

    /**
     * Navigate to settings screen
     */
    fun navigateToSettings() {
        val fragment = SettingsFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    /**
     * Navigate to login screen (called after logout)
     */
    fun navigateToLogin() {
        val fragment = LoginFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
