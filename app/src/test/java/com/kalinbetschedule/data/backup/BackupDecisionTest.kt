package com.kalinbetschedule.data.backup
import org.junit.Assert.assertEquals
import org.junit.Test
class BackupDecisionTest {
    private val filled = """{"clients":[{"id":1}],"services":[],"slots":[]}"""
    private val empty = """{"clients":[],"services":[],"slots":[]}"""
    @Test
    fun `первая копия уходит на пустой Диск`() {
        assertEquals(Decision.UPLOAD, decide(remoteJson = null, localJson = filled, localEmpty = false))
    }
    @Test
    fun `совпадающие данные никого не трогают`() {
        assertEquals(Decision.IN_SYNC, decide(remoteJson = filled, localJson = filled, localEmpty = false))
    }
    @Test
    fun `после переустановки данные приезжают с Диска`() {
        assertEquals(Decision.TAKE_REMOTE, decide(remoteJson = filled, localJson = empty, localEmpty = true))
    }
    @Test
    fun `расхождение решает человек`() {
        assertEquals(Decision.ASK, decide(remoteJson = filled, localJson = empty, localEmpty = false))
    }
    @Test
    fun `пустой телефон и пустой Диск не спорят`() {
        assertEquals(Decision.UPLOAD, decide(remoteJson = null, localJson = empty, localEmpty = true))
    }
}
