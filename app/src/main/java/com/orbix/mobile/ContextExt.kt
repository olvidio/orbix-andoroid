package com.orbix.mobile

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity

/** LocalContext en Compose suele ser ContextThemeWrapper; hay que descender al Activity. */
internal tailrec fun Context.findComponentActivity(): ComponentActivity? {
    return when (this) {
        is ComponentActivity -> this
        is ContextWrapper -> baseContext.findComponentActivity()
        else -> null
    }
}
