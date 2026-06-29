package com.friday.assistant

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.friday.assistant.databinding.ActivitySettingsBinding
import kotlinx.coroutines.launch

/**
 * Ayarlar ekrani: Gemini API anahtari ve hava durumu sehrini kaydeder.
 * Windows surumundeki .env dosyasinin/kur.bat'in mobil karsiligi -- burada
 * kullanici dogrudan ekrandan girip kaydediyor, dosya duzenlemiyor.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var settingsManager: SettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        settingsManager = SettingsManager(this)
        loadCurrentSettings()

        binding.saveButton.setOnClickListener { saveSettings() }
    }

    private fun loadCurrentSettings() {
        lifecycleScope.launch {
            val keys = settingsManager.getGeminiApiKeys()
            if (keys.isNotEmpty()) {
                binding.apiKeyInput.setText(keys.first())
            }
            binding.cityInput.setText(settingsManager.getWeatherCity())
            binding.weatherKeyInput.setText(settingsManager.getWeatherApiKey())
        }
    }

    private fun saveSettings() {
        val apiKey = binding.apiKeyInput.text?.toString()?.trim().orEmpty()
        val city = binding.cityInput.text?.toString()?.trim().orEmpty()
        val weatherKey = binding.weatherKeyInput.text?.toString()?.trim().orEmpty()

        lifecycleScope.launch {
            if (apiKey.isNotEmpty()) settingsManager.saveGeminiApiKey(apiKey, slot = 1)
            if (city.isNotEmpty()) settingsManager.saveWeatherCity(city)
            if (weatherKey.isNotEmpty()) settingsManager.saveWeatherApiKey(weatherKey)
            finish()
        }
    }
}
