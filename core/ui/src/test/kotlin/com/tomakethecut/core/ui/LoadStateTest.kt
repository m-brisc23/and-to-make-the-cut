package com.tomakethecut.core.ui

import com.tomakethecut.core.domain.DataException
import com.tomakethecut.core.ui.state.ErrorKind
import com.tomakethecut.core.ui.state.LoadState
import com.tomakethecut.core.ui.state.toErrorKind
import com.tomakethecut.core.ui.state.toLoadState
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class LoadStateTest {

    @Test
    fun `data exceptions map to error kinds`() {
        assertEquals(ErrorKind.NETWORK, DataException.Network(IOException()).toErrorKind())
        assertEquals(ErrorKind.NOT_FOUND, DataException.NotFound("x").toErrorKind())
        assertEquals(ErrorKind.SERVER, DataException.Server(500).toErrorKind())
        assertEquals(ErrorKind.UNKNOWN, IllegalStateException().toErrorKind())
    }

    @Test
    fun `result maps to load state`() {
        assertEquals(LoadState.Success(1), Result.success(1).toLoadState())
        assertEquals(LoadState.Error(ErrorKind.SERVER), Result.failure<Int>(DataException.Server(502)).toLoadState())
    }
}
