package com.royaal.arch.mvi.log

expect object Logger {
    fun warn(tag: String, message: String)
}
