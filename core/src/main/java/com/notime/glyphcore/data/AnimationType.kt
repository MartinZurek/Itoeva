package com.notime.glyphcore.data

import androidx.annotation.StringRes
import com.notime.glyphcore.R

/**
 * Die fest eingebauten Animationstypen.
 *
 * [labelRes] statt eines festen Textes, damit sich die Bezeichnungen uebersetzen lassen (siehe
 * res/values/strings.xml im Kern). Wichtig ist die Trennung von Anzeigename und Enum-Name: in
 * der Datenbank steht ausschliesslich der Enum-Name (`GENERAL`, `DRINK`, ...), gespeicherte
 * Erinnerungen und Fuetterungen bleiben von einer Uebersetzung also voellig unberuehrt.
 */
enum class AnimationType(@StringRes val labelRes: Int) {
    FOCUS(R.string.animation_focus),
    DRINK(R.string.animation_drink),
    MOVE(R.string.animation_move),
    GENERAL(R.string.animation_general),
    REST(R.string.animation_rest),
    WORK(R.string.animation_work),
    MINDFULNESS(R.string.animation_mindfulness),
    LOVE(R.string.animation_love),
    SLEEP(R.string.animation_sleep),
    MEDICINE(R.string.animation_medicine),
    BOOK(R.string.animation_book),
    CREATIVITY(R.string.animation_creativity)
}
