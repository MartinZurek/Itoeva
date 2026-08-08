package com.notime.glyphsim.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.notime.glyphsim.R
import com.notime.glyphsim.matrix.AvatarAnimations
import com.notime.glyphsim.matrix.AvatarSpecies
import com.notime.glyphsim.matrix.AvatarSpriteView

private val SHEET_BG = Color(0xFF101012)
private val BUBBLE_BG = Color(0xFF1E1E22)
private val TEXT_PRIMARY = Color(0xFFF1EEE6)

/**
 * Die einmalige Erklaerung beim allerersten Einschalten des Spielmodus.
 *
 * **Warum ueberhaupt eine Erklaerung:** Der Schalter aendert nicht die Optik, sondern lautlos die
 * Grundlage der App - ploetzlich kommen nicht mehr die eigenen, sorgfaeltig eingerichteten
 * Erinnerungen, sondern gewuerfelte. Ohne ein Wort dazu waere der erste Eindruck, die eigenen
 * Erinnerungen seien verschwunden. Deshalb steht hier ausdruecklich, dass sie nur ruhen und beim
 * Ausschalten zurueckkommen.
 *
 * Bewusst in derselben Sprechblasen-Form wie [AvatarAssistantDialog]: Es ist der Avatar, der
 * spricht, und das Spiel dreht sich um ihn - ein sachlicher Systemdialog waere hier ein Bruch.
 * Der Avatar steht deshalb wie dort sichtbar daneben.
 */
@Composable
fun PlayModeIntroDialog(
    species: AvatarSpecies,
    onStart: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SHEET_BG,
        titleContentColor = TEXT_PRIMARY,
        textContentColor = TEXT_PRIMARY,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AvatarSpriteView(
                    frame = remember(species) { AvatarAnimations.idlePose(species) },
                    showBackground = false,
                    modifier = Modifier.size(40.dp)
                )
                Text(
                    stringResource(R.string.playmode_intro_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Bubble(stringResource(R.string.playmode_intro_1))
                Bubble(stringResource(R.string.playmode_intro_2))
                Bubble(stringResource(R.string.playmode_intro_3))
                Bubble(stringResource(R.string.playmode_intro_4))
                Choice(stringResource(R.string.playmode_start), onStart)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_back)) }
        }
    )
}

/** Wie im Avatar-Assistenten - eine Sprechblase des Avatars. */
@Composable
private fun Bubble(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        color = TEXT_PRIMARY,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp))
            .background(BUBBLE_BG)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    )
}

@Composable
private fun Choice(text: String, onClick: () -> Unit) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = TEXT_PRIMARY,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF2A2A30))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    )
}
