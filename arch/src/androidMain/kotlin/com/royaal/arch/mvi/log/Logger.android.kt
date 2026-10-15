package com.royaal.arch.mvi.log

actual object Logger {
    actual fun warn(tag: String, message: String) {
        android.util.Log.w(tag, message)
    }
}
