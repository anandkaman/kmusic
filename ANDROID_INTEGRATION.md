# Android App Integration Guide

Complete guide for integrating the music streaming backend with your Android app, including role-based upload functionality.

## Super Admin Credentials

The backend automatically creates a super admin user on first startup:

```
Username: kaman
Password: Johnedoms2@
Role: admin
```

**Important:** Only users with the `admin` role can upload tracks. Regular users can only browse and play music.

---

## Authentication & Role-Based UI

### 1. API Response Structure

When a user logs in, the API returns their role:

```json
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "user": {
    "id": 1,
    "username": "kaman",
    "email": "kaman@musicstream.local",
    "display_name": "Super Admin",
    "role": "admin",
    "created_at": "2024-01-15T10:30:00Z",
    "updated_at": "2024-01-15T10:30:00Z"
  }
}
```

### 2. Data Classes (Kotlin)

Create these data classes in your Android app:

```kotlin
// File: data/model/User.kt
package com.yourapp.musicstream.data.model

import com.google.gson.annotations.SerializedName

data class User(
    val id: Long,
    val username: String,
    val email: String,
    @SerializedName("display_name")
    val displayName: String,
    val role: String, // "admin" or "user"
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String
) {
    fun isAdmin(): Boolean = role == "admin"
}

data class LoginRequest(
    val username: String,
    val password: String
)

data class AuthResponse(
    val token: String,
    val user: User
)
```

### 3. Retrofit API Interface

```kotlin
// File: data/api/MusicApiService.kt
package com.yourapp.musicstream.data.api

import com.yourapp.musicstream.data.model.*
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface MusicApiService {

    @POST("/api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("/api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @GET("/api/user/me")
    suspend fun getCurrentUser(): Response<User>

    // Upload - Only for admin users
    @Multipart
    @POST("/api/tracks/upload")
    suspend fun uploadTrack(
        @Part file: MultipartBody.Part
    ): Response<Track>

    @GET("/api/tracks")
    suspend fun getTracks(
        @Query("artist") artist: String? = null,
        @Query("album") album: String? = null,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null
    ): Response<List<Track>>

    @GET("/api/tracks/{id}")
    suspend fun getTrack(@Path("id") id: Long): Response<Track>

    @Streaming
    @GET("/api/tracks/{id}/stream")
    suspend fun streamTrack(@Path("id") id: Long): Response<ResponseBody>

    @GET("/api/playlists")
    suspend fun getPlaylists(): Response<List<Playlist>>

    @POST("/api/playlists")
    suspend fun createPlaylist(@Body request: CreatePlaylistRequest): Response<Playlist>

    @GET("/api/search")
    suspend fun search(@Query("q") query: String): Response<List<Track>>
}
```

### 4. Authentication Interceptor

Add JWT token to all requests:

```kotlin
// File: data/api/AuthInterceptor.kt
package com.yourapp.musicstream.data.api

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val tokenProvider: () -> String?) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val token = tokenProvider()

        return if (token != null) {
            val authenticatedRequest = request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
            chain.proceed(authenticatedRequest)
        } else {
            chain.proceed(request)
        }
    }
}
```

### 5. Retrofit Setup

```kotlin
// File: data/api/RetrofitClient.kt
package com.yourapp.musicstream.data.api

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "http://your-server-ip:8080/" // Change this!

    private var tokenProvider: (() -> String?)? = null

    fun setTokenProvider(provider: () -> String?) {
        tokenProvider = provider
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(AuthInterceptor { tokenProvider?.invoke() })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = GsonBuilder()
        .setLenient()
        .create()

    val api: MusicApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(MusicApiService::class.java)
    }
}
```

### 6. User Session Manager

Store user data and token securely:

```kotlin
// File: data/local/UserSessionManager.kt
package com.yourapp.musicstream.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.yourapp.musicstream.data.model.User

class UserSessionManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_user_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val gson = Gson()

    fun saveSession(token: String, user: User) {
        prefs.edit().apply {
            putString("auth_token", token)
            putString("user_data", gson.toJson(user))
            apply()
        }
    }

    fun getToken(): String? = prefs.getString("auth_token", null)

    fun getUser(): User? {
        val userJson = prefs.getString("user_data", null)
        return userJson?.let { gson.fromJson(it, User::class.java) }
    }

    fun isLoggedIn(): Boolean = getToken() != null

    fun isAdmin(): Boolean = getUser()?.isAdmin() == true

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
```

---

## Implementing Upload Feature

### 1. Settings Screen with Admin Check

```kotlin
// File: ui/settings/SettingsFragment.kt
package com.yourapp.musicstream.ui.settings

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.yourapp.musicstream.R
import com.yourapp.musicstream.data.local.UserSessionManager
import com.yourapp.musicstream.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionManager: UserSessionManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSettingsBinding.bind(view)

        sessionManager = UserSessionManager(requireContext())

        setupUI()
    }

    private fun setupUI() {
        val user = sessionManager.getUser()

        // Show/hide upload option based on admin status
        if (user?.isAdmin() == true) {
            binding.uploadMusicCard.visibility = View.VISIBLE
            binding.uploadMusicCard.setOnClickListener {
                findNavController().navigate(
                    R.id.action_settings_to_upload
                )
            }
        } else {
            binding.uploadMusicCard.visibility = View.GONE
        }

        // Other settings
        binding.accountInfo.text = "Logged in as: ${user?.displayName}"
        binding.userRole.text = "Role: ${user?.role?.uppercase()}"

        binding.logoutButton.setOnClickListener {
            logout()
        }
    }

    private fun logout() {
        sessionManager.clearSession()
        findNavController().navigate(R.id.action_settings_to_login)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
```

### 2. Settings Layout XML

```xml
<!-- File: res/layout/fragment_settings.xml -->
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:fillViewport="true">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="16dp">

        <!-- User Info -->
        <com.google.android.material.card.MaterialCardView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginBottom="16dp"
            app:cardElevation="4dp">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:padding="16dp">

                <TextView
                    android:id="@+id/accountInfo"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Logged in as: User"
                    android:textSize="16sp" />

                <TextView
                    android:id="@+id/userRole"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="8dp"
                    android:text="Role: USER"
                    android:textColor="@color/colorAccent"
                    android:textStyle="bold" />
            </LinearLayout>
        </com.google.android.material.card.MaterialCardView>

        <!-- Upload Music (Admin Only) -->
        <com.google.android.material.card.MaterialCardView
            android:id="@+id/uploadMusicCard"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginBottom="16dp"
            android:visibility="gone"
            app:cardElevation="4dp">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:padding="16dp"
                android:gravity="center_vertical">

                <ImageView
                    android:layout_width="24dp"
                    android:layout_height="24dp"
                    android:src="@drawable/ic_upload"
                    android:contentDescription="Upload" />

                <TextView
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:layout_marginStart="16dp"
                    android:text="Upload Music"
                    android:textSize="16sp" />

                <ImageView
                    android:layout_width="24dp"
                    android:layout_height="24dp"
                    android:src="@drawable/ic_chevron_right"
                    android:contentDescription="Go" />
            </LinearLayout>
        </com.google.android.material.card.MaterialCardView>

        <!-- Other Settings -->
        <com.google.android.material.card.MaterialCardView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginBottom="16dp"
            app:cardElevation="4dp">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:padding="16dp">

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Preferences"
                    android:textSize="18sp"
                    android:textStyle="bold" />

                <!-- Add other settings here -->
            </LinearLayout>
        </com.google.android.material.card.MaterialCardView>

        <!-- Logout Button -->
        <com.google.android.material.button.MaterialButton
            android:id="@+id/logoutButton"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="24dp"
            android:text="Logout"
            app:icon="@drawable/ic_logout" />
    </LinearLayout>
</ScrollView>
```

### 3. Upload Fragment

```kotlin
// File: ui/upload/UploadFragment.kt
package com.yourapp.musicstream.ui.upload

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.yourapp.musicstream.R
import com.yourapp.musicstream.databinding.FragmentUploadBinding
import kotlinx.coroutines.launch

class UploadFragment : Fragment(R.layout.fragment_upload) {

    private var _binding: FragmentUploadBinding? = null
    private val binding get() = _binding!!

    private val viewModel: UploadViewModel by viewModels()

    private var selectedFileUri: Uri? = null

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedFileUri = result.data?.data
            updateUI()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentUploadBinding.bind(view)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.selectFileButton.setOnClickListener {
            openFilePicker()
        }

        binding.uploadButton.setOnClickListener {
            uploadFile()
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "audio/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        filePickerLauncher.launch(intent)
    }

    private fun uploadFile() {
        val uri = selectedFileUri
        if (uri == null) {
            Toast.makeText(requireContext(), "Please select a file first", Toast.LENGTH_SHORT).show()
            return
        }

        binding.uploadButton.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                viewModel.uploadTrack(uri, requireContext())
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updateUI() {
        selectedFileUri?.let { uri ->
            val fileName = uri.lastPathSegment ?: "Unknown"
            binding.selectedFileText.text = "Selected: $fileName"
            binding.uploadButton.isEnabled = true
        }
    }

    private fun observeViewModel() {
        viewModel.uploadState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UploadState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.uploadButton.isEnabled = false
                }
                is UploadState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Upload successful!", Toast.LENGTH_SHORT).show()
                    // Reset
                    selectedFileUri = null
                    binding.selectedFileText.text = "No file selected"
                    binding.uploadButton.isEnabled = false
                }
                is UploadState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.uploadButton.isEnabled = true
                    Toast.makeText(requireContext(), "Error: ${state.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
```

### 4. Upload ViewModel

```kotlin
// File: ui/upload/UploadViewModel.kt
package com.yourapp.musicstream.ui.upload

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourapp.musicstream.data.api.RetrofitClient
import com.yourapp.musicstream.data.model.Track
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

sealed class UploadState {
    object Loading : UploadState()
    data class Success(val track: Track) : UploadState()
    data class Error(val message: String) : UploadState()
}

class UploadViewModel : ViewModel() {

    private val _uploadState = MutableLiveData<UploadState>()
    val uploadState: LiveData<UploadState> = _uploadState

    fun uploadTrack(uri: Uri, context: Context) {
        viewModelScope.launch {
            try {
                _uploadState.value = UploadState.Loading

                // Convert URI to File
                val file = uriToFile(uri, context)

                // Create multipart request
                val requestFile = file.asRequestBody("audio/*".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

                // Upload
                val response = RetrofitClient.api.uploadTrack(body)

                if (response.isSuccessful && response.body() != null) {
                    _uploadState.value = UploadState.Success(response.body()!!)
                    // Clean up temp file
                    file.delete()
                } else {
                    _uploadState.value = UploadState.Error(response.message())
                }
            } catch (e: Exception) {
                _uploadState.value = UploadState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun uriToFile(uri: Uri, context: Context): File {
        val contentResolver = context.contentResolver
        val tempFile = File.createTempFile("upload", ".aac", context.cacheDir)

        contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }

        return tempFile
    }
}
```

---

## Testing the Integration

### 1. Test Login with Admin

```kotlin
// In your LoginFragment or activity
lifecycleScope.launch {
    val response = RetrofitClient.api.login(
        LoginRequest(
            username = "kaman",
            password = "Johnedoms2@"
        )
    )

    if (response.isSuccessful && response.body() != null) {
        val authResponse = response.body()!!
        sessionManager.saveSession(authResponse.token, authResponse.user)

        // Check if admin
        if (authResponse.user.isAdmin()) {
            Log.d("Login", "Admin user logged in!")
            // Navigate to home with upload option
        }
    }
}
```

### 2. Test Upload (Admin Only)

```kotlin
// This will work only when logged in as "kaman"
val file = File("/path/to/song.aac")
val requestFile = file.asRequestBody("audio/aac".toMediaTypeOrNull())
val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

val response = RetrofitClient.api.uploadTrack(body)
// Success: Returns track metadata
// Failure (403): "only admins can upload tracks"
```

---

## Dependencies

Add these to your `app/build.gradle`:

```gradle
dependencies {
    // Networking
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
    implementation 'com.squareup.okhttp3:okhttp:4.11.0'
    implementation 'com.squareup.okhttp3:logging-interceptor:4.11.0'

    // Encrypted SharedPreferences
    implementation 'androidx.security:security-crypto:1.1.0-alpha06'

    // Coroutines
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3'

    // ViewModel & LiveData
    implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2'
    implementation 'androidx.lifecycle:lifecycle-livedata-ktx:2.6.2'

    // Navigation
    implementation 'androidx.navigation:navigation-fragment-ktx:2.7.5'
    implementation 'androidx.navigation:navigation-ui-ktx:2.7.5'

    // Material Design
    implementation 'com.google.android.material:material:1.10.0'
}
```

---

## Summary

1. **Super admin created automatically** on backend startup (username: `kaman`, password: `Johnedoms2@`)
2. **All users can browse/play** music
3. **Only admin users can upload** tracks
4. **Role returned in login/register** response
5. **Check `user.role == "admin"`** to show/hide upload UI
6. **403 error** if non-admin tries to upload

Your Android app should:
- Store the user's role after login
- Show "Upload Music" option in settings only if `isAdmin()` returns true
- Handle 403 Forbidden errors gracefully

---

**Ready to build!** Start with the login flow, then add the conditional upload UI based on the user's role.
