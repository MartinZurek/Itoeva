package com.notime.glyphsim.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.notime.glyphcore.data.AnimationMotif
import com.notime.glyphcore.data.AnimationNode

/**
 * Der Anzeigename eines Knotens.
 *
 * **Zwei Quellen, und das ist Absicht** (siehe [AnimationNode.titleRes]): Gruppen und
 * Untergruppen haben einen eigenen, uebersetzten Namen, weil es ihn nirgends sonst gibt. Blaetter
 * tragen den Namen ihres Motivs - eine zweite Bezeichnung daneben koennte nur auseinanderlaufen.
 *
 * Der letzte Zweig faengt die Knoten ab, die noch nicht gezeichnet sind: Sie haben weder das eine
 * noch das andere und bekommen ihren Pfad-Rest als Notbehelf. Das ist bewusst schlicht - sobald
 * sie ein Motiv haben (SKILLBAUM.md, P8), kommt der Name mit.
 */
@Composable
fun AnimationNode.rememberTitle(): String {
    val motif = motif
    val title = titleRes
    return when {
        title != null -> stringResource(title)
        motif is AnimationMotif.Builtin -> stringResource(motif.type.labelRes)
        motif is AnimationMotif.Library -> motif.label
        else -> id.substringAfterLast('/').replaceFirstChar { it.uppercase() }
    }
}
