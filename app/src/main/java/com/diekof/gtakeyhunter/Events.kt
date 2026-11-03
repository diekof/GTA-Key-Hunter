package com.diekof.gtakeyhunter
import kotlinx.coroutines.flow.MutableSharedFlow
object Events {
    val changed = MutableSharedFlow<Unit>(extraBufferCapacity = 32)
    fun ping() { changed.tryEmit(Unit) }
}