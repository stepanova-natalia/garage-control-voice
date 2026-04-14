package pro.web4u.app.garage.voice

import org.junit.Assert.assertEquals
import org.junit.Test
import pro.web4u.app.garage.voice.feature.voice.Command
import pro.web4u.app.garage.voice.feature.voice.CommandParser

class CommandParserTest {
    private val name = "джарвис"

    //GATE OPEN
    @Test
    fun `открой ворота возвращает GATE_OPEN`() {
        assertEquals(Command.GATE_OPEN, CommandParser.parse("джарвис открой ворота", name))
    }

    @Test
    fun `открыть ворота возвращает GATE_OPEN`() {
        assertEquals(Command.GATE_OPEN, CommandParser.parse("джарвис открыть ворота", name))
    }

    //GATE CLOSE
    @Test
    fun `закрой ворота возвращает GATE_CLOSE`() {
        assertEquals(Command.GATE_CLOSE, CommandParser.parse("джарвис закрой ворота", name))
    }

    @Test
    fun `закрыть ворота возвращает GATE_CLOSE`() {
        assertEquals(Command.GATE_CLOSE, CommandParser.parse("джарвис закрыть ворота", name))
    }

    // GATE_STOP
    @Test
    fun `останови ворота возвращает GATE_STOP`() {
        assertEquals(Command.GATE_STOP, CommandParser.parse("джарвис останови движение", name))
    }

    @Test
    fun `остановить движение возвращает GATE_STOP`() {
        assertEquals(Command.GATE_STOP, CommandParser.parse("джарвис остановить движение", name))
    }

    // GATE_CONTINUE
    @Test
    fun `продолжи движение возвращает GATE_CONTINUE`() {
        assertEquals(Command.GATE_CONTINUE, CommandParser.parse("джарвис продолжи движение", name))
    }

    @Test
    fun `продолжить движение возвращает GATE_CONTINUE`() {
        assertEquals(Command.GATE_CONTINUE, CommandParser.parse("джарвис продолжить движение", name))
    }

    //GATE STATUS
    @Test
    fun `статус ворот возвращает GATE_STATUS`() {
        assertEquals(Command.GATE_STATUS, CommandParser.parse("джарвис статус ворот", name))
    }

    //LIGHT ON
    @Test
    fun `включи свет возвращает LIGHT_ON`() {
        assertEquals(Command.LIGHT_ON, CommandParser.parse("джарвис включи свет", name))
    }

    @Test
    fun `включить свет возвращает LIGHT_ON`() {
        assertEquals(Command.LIGHT_ON, CommandParser.parse("джарвис включить свет", name))
    }

    //LIGHT OFF
    @Test
    fun `выключи свет возвращает LIGHT_OFF`() {
        assertEquals(Command.LIGHT_OFF, CommandParser.parse("джарвис выключи свет", name))
    }

    @Test
    fun `выключить свет возвращает LIGHT_OFF`() {
        assertEquals(Command.LIGHT_OFF, CommandParser.parse("джарвис выключить свет", name))
    }

    //LIGHT STATUS
    @Test
    fun `статус света возвращает LIGHT_STATUS`() {
        assertEquals(Command.LIGHT_STATUS, CommandParser.parse("джарвис статус света", name))
    }

    // UNKNOWN
    @Test
    fun `без имени возвращает UNKNOWN`() {
        assertEquals(Command.UNKNOWN, CommandParser.parse("открой ворота", name))
    }

    @Test
    fun `неизвестная команда возвращает UNKNOWN`() {
        assertEquals(Command.UNKNOWN, CommandParser.parse("джарвис привет", name))
    }

    @Test
    fun `пустая строка возвращает UNKNOWN`() {
        assertEquals(Command.UNKNOWN, CommandParser.parse("", name))
    }

    @Test
    fun `только имя возвращает UNKNOWN`() {
        assertEquals(Command.UNKNOWN, CommandParser.parse("джарвис", name))
    }
}