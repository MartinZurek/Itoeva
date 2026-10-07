package com.notime.glyphsim.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.notime.glyphcore.data.AnimationType
import com.notime.glyphsim.matrix.*
import kotlin.math.roundToInt

internal fun objectName(id: GameAdventure.ObjectId, de: Boolean): String = when (id) {
    GameAdventure.ObjectId.SEEDS -> if (de) "Samen sammeln" else "Collect seeds"
    GameAdventure.ObjectId.WOOD -> if (de) "Holz sammeln" else "Collect wood"
    GameAdventure.ObjectId.MARKER -> if (de) "Wegstein untersuchen" else "Inspect waystone"
    GameAdventure.ObjectId.GARDEN -> if (de) "Samen pflanzen" else "Plant seeds"
    GameAdventure.ObjectId.BENCH -> if (de) "Bank reparieren · 1 Holz" else "Repair bench · 1 wood"
    GameAdventure.ObjectId.CAMP -> if (de) "Lagerbank reparieren · 1 Holz" else "Repair camp seat · 1 wood"
}

internal fun eventText(event: GameAdventure.Event, de: Boolean): String = when (event) {
    GameAdventure.Event.FOREST_SEEDS -> if (de) "Im Wald Samen für den Park gefunden." else "Found seeds for the park in the forest."
    GameAdventure.Event.FOREST_WOOD -> if (de) "Holz gefunden. Es reicht für eine Reparatur im Park oder im Lager." else "Found wood for one repair in the park or camp."
    GameAdventure.Event.OLD_MARKER -> if (de) "Der alte Wegstein trägt ein Blattzeichen. Es verbindet Wald und Park." else "An old waystone bears a leaf symbol connecting forest and park."
    GameAdventure.Event.PLANTED -> if (de) "Im Park wächst nun eine junge Pflanze." else "A young plant now grows in the park."
    GameAdventure.Event.BENCH_REPAIRED -> if (de) "Die Parkbank hat eine neue Verstärkung erhalten." else "The park bench has a new reinforcement."
    GameAdventure.Event.CAMP_REPAIRED -> if (de) "Der Sitzplatz im Lager ist wieder hergerichtet." else "Restored the camp seat."
}

internal fun residentDialogue(resident: LivingResident, state: GameAdventure.State, known: Boolean, de: Boolean): String {
    val greeting = if (known) (if (de) "Schön, dich wiederzusehen. " else "Good to see you again. ")
        else (if (de) "Hallo! " else "Hello! ")
    val response = when {
        GameAdventure.Event.BENCH_REPAIRED in state.events && resident.anchorPlace == PlayScene.Place.PARK ->
            if (de) "Die Bank im Park hat jetzt eine feste Stütze. Hier kann man wieder in Ruhe lesen."
            else "The park bench has a firm support now. It is a lovely place to read."
        GameAdventure.Event.PLANTED in state.events ->
            if (de) "Die junge Pflanze im Park ist mir aufgefallen. Das Blattzeichen auf dem alten Wegstein im Wald passt dazu."
            else "I noticed the young plant in the park. It matches the leaf symbol on the old forest waystone."
        resident.role == ResidentRole.SHOPKEEPER ->
            if (de) "Du kannst Dinge aus dem Rucksack in die Ablage legen und später zurückholen. Im Wald liegen Samen und Holz."
            else "Store backpack items and retrieve them later. There are seeds and wood in the forest."
        resident.role == ResidentRole.ATHLETE ->
            if (de) "Der Waldweg führt zu einem alten Stein. Holz reicht zunächst für eine Reparatur: die Parkbank oder den Sitz im Lager."
            else "The forest path leads to an old stone. One wood repairs either the park bench or the camp seat."
        else -> if (de) "Im Park wartet eine Pflanzstelle auf Samen. Im Wald findest du welche und einen alten Wegstein mit Blattzeichen."
            else "There is a planting spot in the park. Find seeds and a leaf-marked waystone in the forest."
    }
    return greeting + response
}

/** Kleine ortsfeste Requisiten und bleibende Folgen in denselben Koordinaten wie die Figur. */
@Composable
internal fun GameAdventureObjects(scene: GameScenes.Scene, pos: PlayControl.Pos, state: GameAdventure.State,
    phase: Int, modifier: Modifier, camera: GameCamera.State? = null) {
    Canvas(modifier) {
        val fit = camera?.let { GameCamera.fit(it, scene, size.width, size.height) } ?: GameScenes.fit(scene, size.width, size.height, pos.x, pos.depth)
        val brightness = (1f - GameSceneLighting.darkness(scene, state.minuteOfDay) * .6f)
        withTransform({ translate(fit.left, fit.top); scale(fit.scale, fit.scale, Offset.Zero) }) {
            for (id in GameAdventure.ObjectId.entries.filter { it.place == scene.place && GameAdventure.visible(state, it) }) {
                val anchor = GameAdventure.position(scene, id)
                val (x, y) = GameScenes.feet(scene, anchor)
                val green = Color(0xFF78A765).copy(alpha = brightness)
                val wood = Color(0xFFAD8354).copy(alpha = brightness)
                when (id) {
                    GameAdventure.ObjectId.SEEDS -> if (GameAdventure.Event.FOREST_SEEDS !in state.events) {
                        drawOval(Color(0xFF9D7146), Offset(x - 6, y - 7), Size(12f, 7f))
                        repeat(4) { drawCircle(Color(0xFFE1C688), 1.2f, Offset(x - 3 + it * 2, y - 4)) }
                    }
                    GameAdventure.ObjectId.WOOD -> if (GameAdventure.Event.FOREST_WOOD !in state.events) {
                        repeat(3) { drawLine(wood, Offset(x - 8, y - 2 - it * 3), Offset(x + 7, y - 4 - it * 3), 3f) }
                    }
                    GameAdventure.ObjectId.MARKER -> {
                        drawRect(Color(0xFF797F72), Offset(x - 6, y - 19), Size(12f, 19f))
                        drawLine(green, Offset(x, y - 15), Offset(x, y - 5), 1f)
                        drawOval(green, Offset(x - 4, y - 14), Size(8f, 5f))
                    }
                    GameAdventure.ObjectId.GARDEN -> {
                        drawOval(Color(0xFF554335), Offset(x - 11, y - 3), Size(22f, 6f))
                        if (GameAdventure.Event.PLANTED in state.events) {
                            drawLine(green, Offset(x, y), Offset(x, y - 14), 2f)
                            drawOval(green, Offset(x - 9, y - 13), Size(9f, 5f))
                            drawOval(green, Offset(x, y - 16), Size(9f, 5f))
                            drawCircle(Color(0xFFE4C875), 2f, Offset(x, y - 16))
                        } else repeat(3) { drawCircle(wood, 1f, Offset(x - 6 + it * 6, y)) }
                    }
                    GameAdventure.ObjectId.BENCH, GameAdventure.ObjectId.CAMP -> {
                        val fixed = (if (id == GameAdventure.ObjectId.BENCH) GameAdventure.Event.BENCH_REPAIRED else GameAdventure.Event.CAMP_REPAIRED) in state.events
                        val seat = scene.spots.firstOrNull { it.station == PlayScene.Station.BENCH }?.hit
                        if (fixed && seat != null) {
                            // Neues Stützholz sitzt am gemalten Möbel, nicht als loses Symbol am Boden.
                            val sx = (seat.x0 + seat.x1) / 2f
                            val sy = seat.y1 - 4f
                            drawLine(wood, Offset(sx - 20, sy - 10), Offset(sx + 20, sy), 3f)
                            drawCircle(Color(0xFFD7D1BE), 1f, Offset(sx - 18, sy - 9))
                            drawCircle(Color(0xFFD7D1BE), 1f, Offset(sx + 18, sy - 1))
                        } else {
                            drawLine(wood, Offset(x - 9, y - 2), Offset(x + 9, y - 2), 3f)
                            drawRect(Color(0xFF473729), Offset(x - 1, y - 4), Size(3f, 6f))
                        }
                    }
                }
                if (phase % 8 < 4) drawCircle(Color(0xFFF5D995).copy(alpha = .5f), 2f, Offset(x, y - 24), style = Stroke(.7f))
            }
        }
    }
}

/** NPC-Fuesse, Groesse und Kamera verwenden die Bildgeometrie des Spielers. */
@Composable
internal fun GameResidentSprites(scene: GameScenes.Scene, host: PlayControl.Pos, actors: Collection<GameResidents.Actor>,
    phase: Int, minute: Int, modifier: Modifier, camera: GameCamera.State? = null) {
    val density = LocalDensity.current
    BoxWithConstraints(modifier) {
        val w = with(density) { maxWidth.toPx() }
        val h = with(density) { maxHeight.toPx() }
        val fit = camera?.let { GameCamera.fit(it, scene, w, h) } ?: GameScenes.fit(scene, w, h, host.x, host.depth)
        for (actor in actors.sortedBy { it.pos.depth }) {
            val species = actor.snapshot.species
            val task = LivingPopulationLayout.poseFor(actor.snapshot)
            val sequence = remember(species, task, actor.moving) {
                when {
                    actor.moving -> AvatarAnimations.walkSequence(species)
                    task is LivingPopulationLayout.ResidentPose.Doing -> AvatarAnimations.reactionFor(species, task.topic)
                    else -> AvatarAnimations.idleSequence(species, AvatarMood.NEUTRAL)
                }
            }
            val frame = sequence.frames[Math.floorMod(phase + species.ordinal * 3, sequence.frames.size)]
            val (x, y) = GameScenes.feet(scene, actor.pos)
            val (sx, sy) = fit.toScreen(x, y)
            val px = GameScenes.avatarHeight(scene, actor.pos) * fit.scale / .8f
            val dp = with(density) { px.toDp() }
            AvatarSpriteView(frame = frame, species = species, showBackground = false,
                brightnessScale = 1f - GameSceneLighting.darkness(scene, minute) * .35f,
                gameDirection = actor.facing, gameMoving = actor.moving,
                contentDescription = stringResource(species.labelRes),
                modifier = Modifier.width(dp).height(dp * AvatarGeometry.HEIGHT / AvatarGeometry.SIZE)
                    .offset { IntOffset((sx - px / 2f).roundToInt(), AvatarFooting.topFor(sy, px, AvatarBodies.forSpecies(species).groundRow()).roundToInt()) })
            val item = if (!actor.moving && task is LivingPopulationLayout.ResidentPose.Doing) when (task.topic) {
                AnimationType.BOOK -> PlayEffects.Carried.BOOK
                AnimationType.DRINK -> PlayEffects.Carried.CUP
                else -> null
            } else null
            if (item != null) ItemIcon(item, Modifier
                .offset { IntOffset((sx + px * .08f).roundToInt(), (sy - px * .36f).roundToInt()) }
                .size(with(density) { (px * .25f).toDp() }))

        }
    }
}

/** Außenregen benutzt dieselbe Wetterlage wie die Atmo; Fenster-Masken folgen mit den Innenräumen. */
@Composable
internal fun GameWeatherView(scene: GameScenes.Scene, pos: PlayControl.Pos, state: GameAdventure.State, modifier: Modifier, camera: GameCamera.State? = null) {
    Canvas(modifier) {
        if (state.weather != PlayWeather.RAIN) return@Canvas
        val outdoors = PlayScene.isOutdoors(scene.place)
        if (!outdoors) return@Canvas // Ohne Fenster-Maske kein Regen quer durch Zimmer.
        val fit = camera?.let { GameCamera.fit(it, scene, size.width, size.height) } ?: GameScenes.fit(scene, size.width, size.height, pos.x, pos.depth)
        val world = GameWorld.isWorld(scene)
        val span = if (world) 1920L else 480L
        val tall = if (world) 640L else 270L
        val origin = if (world) GameWorld.origin(scene.place) else 0f
        repeat(if (world) 150 else 60) { i ->
            val x = ((i * 79L + state.elapsed / 45L) % span).toFloat() - origin
            val y = ((i * 43L + state.elapsed / 9L) % tall).toFloat()
            val (sx, sy) = fit.toScreen(x, y)
            drawLine(Color(0xFFBFD4D7).copy(alpha = .35f), Offset(sx, sy), Offset(sx - 2f * fit.scale, sy + 5f * fit.scale), fit.scale)
        }
    }
}

@Composable
internal fun GameActionButton(label: String, onClick: () -> Unit, modifier: Modifier) {
    Text(label, color = Color(0xFFF6DFB5), fontSize = 13.sp,
        modifier = modifier.zIndex(2f).widthIn(max = 440.dp).background(Color(0xE0192C30), RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(12.dp))
}

@Composable
internal fun GameChronicle(state: GameAdventure.State, de: Boolean, onClose: () -> Unit) {
    GameWorldPanel(if (de) "Chronik" else "Chronicle", onClose) {
        Text(if (GameAdventure.Event.PLANTED !in state.events) {
            if (de) "Eine Pflanze für den Park: Finde Samen im Wald und setze sie an der Pflanzstelle ein."
            else "A plant for the park: find seeds in the forest and plant them in the park."
        } else {
            if (de) "Der Park wächst. Mit Holz kannst du die Bank hier oder den Sitz im Lager verstärken."
            else "The park is growing. Use wood to reinforce the bench here or the seat at camp."
        }, color = Color(0xFFF6DFB5), modifier = Modifier.padding(bottom = 12.dp))
        if (state.events.isEmpty() && state.met.isEmpty()) Text(if (de) "Deine Entdeckungen beginnen auf der Straße, im Park und im Wald." else "Your discoveries begin in the street, park and forest.", color = Color.White)
        state.events.forEach { Text(eventText(it, de), color = Color(0xFFCCDACC), modifier = Modifier.padding(vertical = 6.dp)) }
        for (id in state.met) LivingResidents.all.firstOrNull { it.profileId == id }?.let {
            Text((if (de) "Begegnet: " else "Met: ") + stringResource(it.species.labelRes), color = Color(0xFFCCDACC))
        }
    }
}

@Composable
internal fun GameWorldPanel(title: String, onClose: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0x99000000)), contentAlignment = Alignment.Center) {
        Column(Modifier.widthIn(max = 550.dp).fillMaxWidth().heightIn(max = 290.dp)
            .background(Color(0xFF172C30), RoundedCornerShape(16.dp)).padding(16.dp).verticalScroll(rememberScrollState())) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = Color(0xFFE7D7B5), modifier = Modifier.weight(1f))
                Text("×", color = Color.White, fontSize = 22.sp, modifier = Modifier.clickable(onClick = onClose).padding(12.dp))
            }
            content()
        }
    }
}
