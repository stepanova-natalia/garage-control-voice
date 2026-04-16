package pro.web4u.app.garage.voice

import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import pro.web4u.app.garage.voice.feature.garage.GarageApi
import pro.web4u.app.garage.voice.feature.garage.LightStatusResponse
import pro.web4u.app.garage.voice.feature.voice.handler.LightCommandHandler
import pro.web4u.app.garage.voice.feature.voice.tts.TextSpeaker

class LightCommandHandlerTest {

    private lateinit var garageApi: GarageApi
    private lateinit var tts: TextSpeaker
    private lateinit var handler: LightCommandHandler

    @Before
    fun setup() {
        garageApi = mock()
        tts = mock()
        handler = LightCommandHandler(garageApi, tts)
    }

    // canHandle
    @Test
    fun `canHandle возвращает true для свет`() {
        assert(handler.canHandle("включи свет"))
    }

    @Test
    fun `canHandle возвращает false для ворота`() {
        assert(!handler.canHandle("открой ворота"))
    }

    // LIGHT_ON
    @Test
    fun `включи свет когда выключен — отправляет команду`() = runTest {
        whenever(garageApi.lightStatus()).thenReturn(LightStatusResponse("off"))
        handler.handle("включи свет")
        verify(garageApi).light(
            org.mockito.kotlin.argThat { action == "on" }
        )
        verify(tts).speak("Включаю свет")
    }

    @Test
    fun `включи свет когда уже включен — не отправляет команду`() = runTest {
        whenever(garageApi.lightStatus()).thenReturn(LightStatusResponse("on"))
        handler.handle("включи свет")
        verify(tts).speak("Свет уже включен")
    }

    // LIGHT_OFF
    @Test
    fun `выключи свет когда включен — отправляет команду`() = runTest {
        whenever(garageApi.lightStatus()).thenReturn(LightStatusResponse("on"))
        handler.handle("выключи свет")
        verify(garageApi).light(
            org.mockito.kotlin.argThat { action == "off" }
        )
        verify(tts).speak("Выключаю свет")
    }

    @Test
    fun `выключи свет когда уже выключен — не отправляет команду`() = runTest {
        whenever(garageApi.lightStatus()).thenReturn(LightStatusResponse("off"))
        handler.handle("выключи свет")
        verify(tts).speak("Свет уже выключен")
    }

    // LIGHT_STATUS
    @Test
    fun `статус света — озвучивает включён`() = runTest {
        whenever(garageApi.lightStatus()).thenReturn(LightStatusResponse("on"))
        handler.handle("статус света")
        verify(tts).speak("Свет включен")
    }

    @Test
    fun `статус света — озвучивает выключен`() = runTest {
        whenever(garageApi.lightStatus()).thenReturn(LightStatusResponse("off"))
        handler.handle("статус света")
        verify(tts).speak("Свет выключен")
    }
}