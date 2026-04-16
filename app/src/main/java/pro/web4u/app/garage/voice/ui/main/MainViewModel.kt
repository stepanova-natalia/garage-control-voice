package pro.web4u.app.garage.voice.ui.main

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import pro.web4u.app.garage.voice.feature.voice.VoiceSession
import javax.inject.Inject


@HiltViewModel
class MainViewModel @Inject constructor(
    private val voiceSession: VoiceSession
) : ViewModel() {
    val voiceState = voiceSession.state
}