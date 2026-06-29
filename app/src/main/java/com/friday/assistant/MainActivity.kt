package com.friday.assistant

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.friday.assistant.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

/**
 * Friday'in ana ekrani. Windows surumundeki jarvis.py'nin Android karsiligi:
 * mikrofon/metin girisi alir, GeminiClient'a gonderir, sonucu hem ekranda
 * gosterir hem sesli okur.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var settingsManager: SettingsManager
    private lateinit var memoryStore: MemoryStore
    private lateinit var voiceController: VoiceController
    private lateinit var conversationAdapter: ConversationAdapter
    private var geminiClient: GeminiClient? = null
    private var isListening = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            showError(getString(R.string.permission_required))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        settingsManager = SettingsManager(this)
        memoryStore = MemoryStore(this)

        setupConversationList()
        setupVoiceController()
        setupButtons()
        ensureMicrophonePermission()
        initializeGeminiClient()
    }

    private fun setupConversationList() {
        conversationAdapter = ConversationAdapter()
        binding.conversationList.layoutManager = LinearLayoutManager(this)
        binding.conversationList.adapter = conversationAdapter
    }

    private fun setupVoiceController() {
        voiceController = VoiceController(
            context = this,
            onResult = { text -> handleUserInput(text) },
            onStatusChange = { status -> updateStatusUi(status) },
            onError = { message -> showError(message) }
        )
        voiceController.initialize()
    }

    private fun setupButtons() {
        binding.micButton.setOnClickListener {
            if (!isListening) {
                voiceController.startListening()
            } else {
                voiceController.stopListening()
            }
        }

        binding.sendButton.setOnClickListener { sendTypedMessage() }
        binding.textInput.setOnEditorActionListener { _, _, _ ->
            sendTypedMessage()
            true
        }

        binding.settingsButton.setOnClickListener {
            startActivity(android.content.Intent(this, SettingsActivity::class.java))
        }
    }

    private fun sendTypedMessage() {
        val text = binding.textInput.text?.toString()?.trim()
        if (text.isNullOrEmpty()) return
        binding.textInput.text?.clear()
        handleUserInput(text)
    }

    private fun ensureMicrophonePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun initializeGeminiClient() {
        lifecycleScope.launch {
            val keys = settingsManager.getGeminiApiKeys()
            val weatherCity = settingsManager.getWeatherCity()

            val executor = CommandExecutor(
                context = this@MainActivity,
                weatherCity = weatherCity,
                weatherApiKey = settingsManager.getWeatherApiKey(),
                onMemorySave = { key, value ->
                    lifecycleScope.launch { memoryStore.save(key, value) }
                }
            )

            geminiClient = if (keys.isNotEmpty()) GeminiClient(keys, executor) else null

            if (keys.isEmpty()) {
                conversationAdapter.addMessage(
                    "Friday",
                    "Henüz bir Gemini API anahtarı tanımlı değil. Ayarlar (⚙) ekranından ekleyebilirsin.",
                    isError = true
                )
                binding.conversationList.scrollToPosition(conversationAdapter.itemCount - 1)
            }
        }
    }

    private fun handleUserInput(text: String) {
        conversationAdapter.addMessage("Siz", text)
        binding.conversationList.scrollToPosition(conversationAdapter.itemCount - 1)

        val client = geminiClient
        if (client == null) {
            showError("Gemini API anahtarı tanımlı değil. Ayarlar ekranından ekleyebilirsin.")
            return
        }

        binding.statusText.text = getString(R.string.status_thinking)
        binding.logoView.setStatus(FridayLogoView.Status.THINKING)

        lifecycleScope.launch {
            val result = client.sendMessage(text)

            conversationAdapter.addMessage("Friday", result.displayText, isError = !result.success)
            binding.conversationList.scrollToPosition(conversationAdapter.itemCount - 1)

            binding.statusText.text = getString(R.string.status_idle)
            binding.logoView.setStatus(
                if (result.success) FridayLogoView.Status.IDLE else FridayLogoView.Status.ERROR
            )

            voiceController.speak(result.spokenText)
        }
    }

    private fun updateStatusUi(status: VoiceController.Status) {
        when (status) {
            VoiceController.Status.LISTENING -> {
                isListening = true
                binding.statusText.text = getString(R.string.status_listening)
                binding.micButton.text = getString(R.string.mic_button_on)
                binding.logoView.setStatus(FridayLogoView.Status.LISTENING)
            }
            VoiceController.Status.SPEAKING -> {
                binding.statusText.text = getString(R.string.status_speaking)
                binding.logoView.setStatus(FridayLogoView.Status.SPEAKING)
            }
            VoiceController.Status.IDLE -> {
                isListening = false
                binding.statusText.text = getString(R.string.status_idle)
                binding.micButton.text = getString(R.string.mic_button_off)
                binding.logoView.setStatus(FridayLogoView.Status.IDLE)
            }
        }
    }

    private fun showError(message: String) {
        conversationAdapter.addMessage("Friday", message, isError = true)
        binding.conversationList.scrollToPosition(conversationAdapter.itemCount - 1)
        binding.logoView.setStatus(FridayLogoView.Status.ERROR)
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceController.destroy()
    }
}
