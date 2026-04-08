package com.music.vivi.ui.screens.equalizer.axion

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.vivi.eq.EqualizerService
import com.music.vivi.eq.data.EQProfileRepository
import com.music.vivi.eq.data.FilterType
import com.music.vivi.eq.data.ParametricEQBand
import com.music.vivi.eq.data.SavedEQProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AxionEqViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val equalizerService: EqualizerService,
    private val eqProfileRepository: EQProfileRepository
) : ViewModel() {

    private val prefs = context.getSharedPreferences("vivi_eq_prefs", Context.MODE_PRIVATE)

    private val _enabled = MutableStateFlow(prefs.getBoolean("enabled", false))
    val enabled = _enabled.asStateFlow()

    private val bandFrequencies = doubleArrayOf(31.0, 62.0, 125.0, 250.0, 500.0, 1000.0, 2000.0, 4000.0, 8000.0, 16000.0)
    
    private val _bandGains = MutableStateFlow(
        FloatArray(10) { prefs.getFloat("band_$it", 0f) }
    )
    val bandGains = _bandGains.asStateFlow()

    private val _mode = MutableStateFlow(prefs.getInt("mode", 0)) // 0: Simple, 1: Advanced
    val mode = _mode.asStateFlow()

    init {
        if (_enabled.value) {
            applyToService()
        }
    }

    fun setEnabled(enabled: Boolean) {
        _enabled.value = enabled
        prefs.edit().putBoolean("enabled", enabled).apply()
        if (enabled) {
            applyToService()
        } else {
            viewModelScope.launch {
                eqProfileRepository.setActiveProfile(null)
            }
            equalizerService.disable()
        }
    }

    fun setMode(mode: Int) {
        _mode.value = mode
        prefs.edit().putInt("mode", mode).apply()
    }

    fun setBandGain(index: Int, gain: Float) {
        val newGains = _bandGains.value.copyOf()
        newGains[index] = gain
        _bandGains.value = newGains
        prefs.edit().putFloat("band_$index", gain).apply()
        if (_enabled.value) {
            applyToService()
        }
    }

    fun setBandsGains(gains: FloatArray) {
        _bandGains.value = gains
        val editor = prefs.edit()
        gains.forEachIndexed { index, f -> editor.putFloat("band_$index", f) }
        editor.apply()
        if (_enabled.value) {
            applyToService()
        }
    }

    fun reset() {
        val flat = FloatArray(10) { 0f }
        setBandsGains(flat)
    }

    private fun applyToService() {
        viewModelScope.launch {
            val bands = _bandGains.value.mapIndexed { index, f ->
                ParametricEQBand(
                    frequency = bandFrequencies[index],
                    gain = f.toDouble() / 50.0, // 600 / 50 = 12dB
                    q = 1.41,
                    filterType = FilterType.PK,
                    enabled = true
                )
            }
            
            val profile = SavedEQProfile(
                id = "vivi_tuning",
                name = "Vivi Tuning",
                deviceModel = "ViviEqualizer",
                bands = bands,
                preamp = 0.0,
                isCustom = true,
                isActive = true
            )
            
            // Save to global repository and set as active to ensure persistence across restarts
            eqProfileRepository.saveProfile(profile)
            eqProfileRepository.setActiveProfile(profile.id)
            
            equalizerService.applyProfile(profile)
        }
    }
}
