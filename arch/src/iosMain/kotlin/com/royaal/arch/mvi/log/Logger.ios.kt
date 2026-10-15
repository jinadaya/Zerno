package com.royaal.arch.mvi.log

import platform.Foundation.NSLog

actual object Logger {
    actual fun warn(tag: String, message: String) {
        // %@ avoids treating message content as an NSLog format string.
        NSLog("%@", "$tag: $message")
    }
}
