package com.kmusic.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.kmusic.MainActivity
import com.kmusic.R
import com.kmusic.data.local.ServerConfigManager
import com.kmusic.data.model.Track
import com.kmusic.data.repository.TrackRepository
import com.kmusic.databinding.FragmentHomeBinding
import com.google.android.material.snackbar.Snackbar

/**
 * HomeFragment - Home screen with track listing
 *
 * Features:
 * - Display list of tracks
 * - Search functionality
 * - Play tracks
 * - Navigate to player
 *
 * TODO: Implement RecyclerView adapter for tracks
 */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: HomeViewModel
    private lateinit var serverConfigManager: ServerConfigManager
    private lateinit var trackRepository: TrackRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize managers and repository
        serverConfigManager = ServerConfigManager(requireContext())
        trackRepository = TrackRepository(serverConfigManager)

        // Initialize ViewModel
        val factory = HomeViewModelFactory(trackRepository)
        viewModel = ViewModelProvider(this, factory)[HomeViewModel::class.java]

        // Setup UI
        setupClickListeners()
        setupRecyclerView()
        observeViewModel()

        // Load tracks
        viewModel.loadTracks()
    }

    /**
     * Setup click listeners
     */
    private fun setupClickListeners() {
        binding.settingsButton.setOnClickListener {
            (requireActivity() as MainActivity).navigateToSettings()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Setup RecyclerView for track list
     */
    private fun setupRecyclerView() {
        // TODO: Implement TrackAdapter
        // binding.tracksRecyclerView.apply {
        //     layoutManager = LinearLayoutManager(requireContext())
        //     adapter = trackAdapter
        // }
    }

    /**
     * Observe ViewModel state
     */
    private fun observeViewModel() {
        // Tracks
        viewModel.tracks.observe(viewLifecycleOwner) { tracks ->
            // TODO: Update adapter
            // trackAdapter.submitList(tracks)
        }

        // Loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // TODO: Show/hide progress indicator
            // binding.progressIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Error messages
        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    /**
     * Play a track
     */
    private fun playTrack(track: Track) {
        (requireActivity() as MainActivity).navigateToPlayer(track)
    }

    /**
     * Play tracks from a list
     */
    private fun playTracks(tracks: List<Track>, startIndex: Int = 0) {
        (requireActivity() as MainActivity).navigateToPlayer(tracks, startIndex)
    }
}
