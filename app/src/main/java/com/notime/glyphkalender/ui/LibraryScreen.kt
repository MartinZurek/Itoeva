package com.notime.glyphkalender.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.notime.glyphcore.data.AnimationType
import com.notime.glyphcore.data.FrameCodec
import com.notime.glyphcore.data.FrameCrossfade
import com.notime.glyphcore.data.LibraryAnimation
import com.notime.glyphkalender.glyph.ReminderAnimations
import com.notime.glyphkalender.matrix.MatrixGeometry
import com.notime.glyphkalender.matrix.MatrixPreviewView
import com.notime.glyphkalender.matrix.PreviewAnimator

/**
 * Bibliothek aller waehlbaren Animationen: die 12 fest eingebauten [AnimationType]s
 * (Kotlin-gezeichnet, siehe glyph/ReminderAnimations.kt) genauso wie die per DB
 * erweiterbaren [LibraryAnimation]s - beide Gruppen teilen sich denselben Haken-/Plus-
 * Umschalter und dieselbe Gesamt-Obergrenze im Animations-Picker (siehe
 * GlyphReminderViewModel.MAX_ANIMATIONS_IN_PICKER). Jede Zeile spielt ihre Animation
 * live als kleine Vorschau ab. Umschalt-Button statt Drag & Drop, weil zuverlaessiger
 * auf dem Touchscreen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(onBack: () -> Unit, viewModel: GlyphReminderViewModel = viewModel()) {
    val libraryAnimations by viewModel.libraryAnimations.collectAsState()
    val builtInSelections by viewModel.builtInSelections.collectAsState()
    val builtInSelectedByType = builtInSelections.associate { it.animationType to it.isSelected }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.libraryFullEvent.collect {
            snackbarHostState.showSnackbar("Picker is full - remove an animation before adding another one")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Animation library") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Text(
                "Tap the checkmark to add an animation to the picker when creating a reminder, or tap " +
                    "the plus to put it back. That includes the 12 built-in ones - pick as few or as many as you want.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp)
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(AnimationType.entries, key = { "builtin_${it.name}" }) { type ->
                    BuiltInAnimationCard(
                        type = type,
                        isSelected = builtInSelectedByType[type.name] ?: true,
                        onToggle = { selected -> viewModel.setBuiltInAnimationSelected(type, selected) }
                    )
                }
                items(libraryAnimations, key = { it.id }) { animation ->
                    LibraryAnimationCard(
                        animation = animation,
                        onToggle = { selected -> viewModel.setLibraryAnimationSelected(animation.id, selected) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BuiltInAnimationCard(type: AnimationType, isSelected: Boolean, onToggle: (Boolean) -> Unit) {
    val visual = animationVisuals.getValue(type)
    var frame by remember { mutableStateOf(IntArray(MatrixGeometry.SIZE * MatrixGeometry.SIZE)) }

    LaunchedEffect(type) {
        val frames = ReminderAnimations.framesFor(type)
        while (frames.isNotEmpty()) {
            PreviewAnimator.play(frames) { frame = it }
        }
    }

    AnimationLibraryCard(
        emoji = visual.emoji,
        label = stringResource(type.labelRes),
        frame = frame,
        isSelected = isSelected,
        onToggle = onToggle
    )
}

@Composable
private fun LibraryAnimationCard(animation: LibraryAnimation, onToggle: (Boolean) -> Unit) {
    var frame by remember { mutableStateOf(IntArray(MatrixGeometry.SIZE * MatrixGeometry.SIZE)) }

    LaunchedEffect(animation.framesData) {
        val frames = decodeFrames(animation.framesData)
        while (frames.isNotEmpty()) {
            PreviewAnimator.play(frames) { frame = it }
        }
    }

    AnimationLibraryCard(
        emoji = animation.emoji,
        label = animation.label,
        frame = frame,
        isSelected = animation.isSelected,
        onToggle = onToggle
    )
}

@Composable
private fun AnimationLibraryCard(
    emoji: String,
    label: String,
    frame: IntArray,
    isSelected: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MatrixPreviewView(frame = frame, modifier = Modifier.size(56.dp))
            Spacer(Modifier.width(14.dp))
            Text(
                "$emoji $label",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                FilledIconButton(onClick = { onToggle(false) }) {
                    Icon(Icons.Default.Check, contentDescription = "Remove from picker")
                }
            } else {
                FilledTonalIconButton(onClick = { onToggle(true) }) {
                    Icon(Icons.Default.Add, contentDescription = "Add to picker")
                }
            }
        }
    }
}

/** Dieselbe Ueberblendung wie beim tatsaechlichen Abspielen (siehe LibraryAnimationRepository.framesFor), damit die Vorschau hier nicht haerter wirkt als die echte Animation. */
private fun decodeFrames(framesData: String): List<IntArray> =
    FrameCrossfade.withCrossfades(MatrixGeometry.SIZE, FrameCodec.decode(framesData))
