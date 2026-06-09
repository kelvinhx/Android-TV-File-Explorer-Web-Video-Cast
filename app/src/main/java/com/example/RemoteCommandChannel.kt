package com.example

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

object RemoteCommandChannel {
    private val _commands = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val commands: SharedFlow<String> = _commands

    fun postCommand(cmd: String) {
        _commands.tryEmit(cmd)
    }
}
