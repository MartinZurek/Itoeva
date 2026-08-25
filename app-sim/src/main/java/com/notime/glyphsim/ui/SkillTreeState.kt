package com.notime.glyphsim.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notime.glyphcore.data.AnimationMotif
import com.notime.glyphcore.data.AnimationNode
import com.notime.glyphcore.data.AnimationTree
import com.notime.glyphsim.data.AppDatabase
import com.notime.glyphsim.skilltree.AvatarUnlockRepository

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

/**
 * Die freigeschalteten Knoten des gerade anwesenden Begleiters, in Baumreihenfolge.
 *
 * Beobachtet statt einmalig gelesen: Die Zieh-Leiste soll in dem Moment mitwachsen, in dem etwas
 * freigeschaltet wird, ohne dass jemand den Bildschirm neu aufbauen muss.
 *
 * [AvatarUnlockRepository.ensureSeeded] laeuft dabei mit - beim allerersten Aufruf je Profil legt
 * es die neun Hauptgruppen an. Der Aufruf ist beliebig wiederholbar und tut danach nichts mehr.
 */
@Composable
fun rememberUnlockedNodes(context: Context, profileId: String): List<AnimationNode> {
    val repository = remember(context) { AvatarUnlockRepository(AppDatabase.getInstance(context)) }
    LaunchedEffect(profileId) {
        repository.ensureSeeded(profileId, System.currentTimeMillis())
    }
    val ids by repository.observeUnlockedNodes(profileId)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    return remember(ids) {
        val offen = ids.toSet()
        // Ueber AnimationTree.nodes statt ueber die Id-Liste: Damit stehen sie in Baumreihenfolge
        // (Hauptgruppe, dann was darunter haengt) statt in der Reihenfolge, in der SQLite sie
        // gerade liefert - die Leiste soll nicht bei jedem Start anders aussehen.
        AnimationTree.nodes.filter { it.id in offen && it.motif != null }
    }
}
