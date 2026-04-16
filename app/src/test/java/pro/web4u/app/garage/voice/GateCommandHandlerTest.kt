package pro.web4u.app.garage.voice

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import pro.web4u.app.garage.voice.feature.garage.GarageApi
import pro.web4u.app.garage.voice.feature.garage.GarageWebSocketClient
import pro.web4u.app.garage.voice.feature.garage.GateStatusResponse
import pro.web4u.app.garage.voice.feature.voice.handler.GateCommandHandler
import pro.web4u.app.garage.voice.feature.voice.tts.TextSpeaker

class GateCommandHandlerTest {

    private lateinit var garageApi: GarageApi
    private lateinit var tts: TextSpeaker
    private lateinit var webSocketClient: GarageWebSocketClient
    private lateinit var handler: GateCommandHandler

    @Before
    fun setup() {
        garageApi = mock()
        tts = mock()
        webSocketClient = mock()
        handler = GateCommandHandler(garageApi, tts, webSocketClient)
    }

    //canHandle
    @Test
    fun `canHandle возвращает true для ворота`() {
        assert(handler.canHandle("открой ворота"))
    }

    @Test
    fun `canHandle возвращает false для свет`() {
        assert(!handler.canHandle("включи свет"))
    }

    //GATE_OPEN
    @Test
    fun  `открой ворота когда закрыты - отправляем команду`() = runTest {
        whenever(garageApi.gateStatus()).thenReturn(GateStatusResponse("close"))
        whenever(webSocketClient.gateStatus).thenReturn(flowOf("moving", "open").let {
            MutableSharedFlow<String>().also { flow ->
                launch { flow.emit("moving"); flow.emit("open") }
            }
        })
        handler.handle("открой ворота")
        verify(garageApi).gate(
            org.mockito.kotlin.argThat { action == "open" }
        )
    }

    @Test
    fun  `открой ворота когда уже открыты - не отправляет команду`() = runTest {
        whenever(garageApi.gateStatus()).thenReturn(GateStatusResponse("open"))
        handler.handle("открой ворота")
        verify(tts).speak("Ворота уже открыты")
    }

    //GATE_CLOSE
    @Test
    fun  `закрой ворота когда открыты - отправляет команду`() = runTest {
        whenever(garageApi.gateStatus()).thenReturn(GateStatusResponse("open"))
        whenever(webSocketClient.gateStatus).thenReturn(
            MutableSharedFlow<String>().also { flow ->
                launch { flow.emit("moving"); flow.emit("close") }
            }
        )
        handler.handle("закрой ворота")
        verify(garageApi).gate(
            org.mockito.kotlin.argThat { action == "close" }
        )
    }

    @Test
    fun  `закрой ворота когда уже закрыты - не отправляет команду`() = runTest {
        whenever(garageApi.gateStatus()).thenReturn(GateStatusResponse("close"))
        handler.handle("закрой ворота")
        verify(tts).speak("Ворота уже закрыты")
    }

    //GATE_STATUS
    @Test
    fun `статус ворот — озвучивает открыты`() = runTest {
        whenever(garageApi.gateStatus()).thenReturn(GateStatusResponse("open"))
        handler.handle("статус ворот")
        verify(tts).speak("Ворота открыты")
    }

    @Test
    fun `статус ворот — озвучивает закрыты`() = runTest {
        whenever(garageApi.gateStatus()).thenReturn(GateStatusResponse("close"))
        handler.handle("статус ворот")
        verify(tts).speak("Ворота закрыты")
    }

    // GATE_STOP
    @Test
    fun `останови ворота когда в движении — отправляет команду`() = runTest {
        whenever(garageApi.gateStatus()).thenReturn(GateStatusResponse("moving"))
        handler.handle("останови ворота")
        verify(garageApi).gate(
            org.mockito.kotlin.argThat { action == "open" }
        )
        verify(tts).speak("Команда остановки отправлена")
    }

    @Test
    fun `останови ворота когда не в движении — сообщает статус`() = runTest {
        whenever(garageApi.gateStatus()).thenReturn(GateStatusResponse("close"))
        handler.handle("останови ворота")
        verify(tts).speak("Ворота закрыты")
    }
}