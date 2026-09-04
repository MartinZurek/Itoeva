from pathlib import Path

dock = Path("app-sim/src/main/java/com/notime/glyphsim/ui/DockScreen.kt")
s = dock.read_text()

def once(old: str, new: str, label: str):
    global s
    count = s.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly one match, got {count}")
    s = s.replace(old, new, 1)

once(
    "import com.notime.glyphcore.data.AnimationType\nimport com.notime.glyphcore.data.ReminderOpenDuration",
    "import com.notime.glyphcore.data.AnimationTree\nimport com.notime.glyphcore.data.AnimationType\nimport com.notime.glyphcore.data.ReminderOpenDuration",
    "AnimationTree import"
)
once(
    "import com.notime.glyphsim.skilltree.AvatarUnlockRepository\nimport com.notime.glyphsim.skilltree.SkillRepertoire",
    "import com.notime.glyphsim.skilltree.ActivityContext\nimport com.notime.glyphsim.skilltree.AvatarActivityBus\nimport com.notime.glyphsim.skilltree.AvatarActivityPlans\nimport com.notime.glyphsim.skilltree.AvatarUnlockRepository\nimport com.notime.glyphsim.skilltree.SkillRepertoire",
    "context imports"
)
once(
    """        var requestedTopic by remember(presenceProfileId) {
            mutableStateOf<AnimationType?>(
                if (initialPresence.resumesPreviousSituation) null else initialPresence.topic
            )
        }
""",
    """        var requestedTopic by remember(presenceProfileId) {
            mutableStateOf<AnimationType?>(
                if (initialPresence.resumesPreviousSituation) null else initialPresence.topic
            )
        }
        /**
         * Die genaue Skill-/Reminder-Absicht, falls sie praeziser ist als [requestedTopic].
         *
         * Kein zweiter Ablaufzustand: Der grobe Typ bleibt fuer Tagesablauf und Gespraech bestehen;
         * dieser Wert verhindert nur, dass z. B. "Football" vor der bestehenden
         * [PlayRoutine]-Ausfuehrung wieder zu einem unbestimmten MOVE zusammenschrumpft.
         */
        var requestedNodeId by remember(presenceProfileId) { mutableStateOf<String?>(null) }
""",
    "requested node state"
)
once(
    """                            currentPlace = place
                            renderedPlace = place
                            currentTopic = topic
                            requestedTopic = topic
""",
    """                            currentPlace = place
                            renderedPlace = place
                            currentTopic = topic
                            requestedNodeId = null
                            requestedTopic = topic
""",
    "long return reset"
)
once(
    """                    if (requestedTopic == null) {
                        requestedTopic = currentTopic ?: PlayPresence.topicFor(java.time.LocalDateTime.now())
                    }
""",
    """                    if (requestedTopic == null && requestedNodeId == null) {
                        requestedNodeId = null
                        requestedTopic = currentTopic ?: PlayPresence.topicFor(java.time.LocalDateTime.now())
                    }
""",
    "play mode resume"
)
once(
    """                            // Tageszeit wirkt dabei bereits mit: [moveToPlace] und die Routine
                            // selbst richten sich nach der aktuellen Tagesphase (siehe
                            // PlayAmbientActivity.currentDayPhase), dieselbe Logik wie beim
                            // autonomen Tagesablauf. Nur bei einem festen [AnimationType] moeglich
                            // - eine Bibliotheks-Animation ohne Thema kennt keinen Ort.
                            current.animationType?.let { requestedTopic = it }
""",
    """                            // Der grobe Typ bleibt der Rueckfall. Haengt die Erinnerungsanimation
                            // aber an einem bereits kontextuell ausgefuehrten Skillbaum-Knoten,
                            // behalten wir zusaetzlich genau DIESEN Knoten. Sonst wuerde
                            // "Football" hier wieder zu einem blossen MOVE zusammenschrumpfen und
                            // die folgende Entscheidung koennte weder Ort noch Skillstand nutzen.
                            val trigger = ReactionTrigger.of(
                                current.animationType, current.libraryAnimationLabel
                            )
                            val contextualNode = (trigger as? ReactionTrigger.Node)
                                ?.nodeId
                                ?.let(AnimationTree::node)
                                ?.takeIf(AvatarActivityPlans::supportsContextualExecution)
                            if (contextualNode != null) {
                                requestedNodeId = contextualNode.id
                                requestedTopic = AnimationType.MOVE
                            } else {
                                requestedNodeId = null
                                requestedTopic =
                                    (trigger as? ReactionTrigger.Topic)?.type ?: current.animationType
                            }
""",
    "feed intent preservation"
)
once(
    """            LaunchedEffect(avatar?.species, requestedTopic) {
                val species = avatar?.species ?: return@LaunchedEffect

                requestedTopic?.let { topic ->
""",
    """            LaunchedEffect(avatar?.species, requestedTopic, requestedNodeId) {
                val species = avatar?.species ?: return@LaunchedEffect

                // Ein genauer Skill-/Reminder-Knoten geht vor dem groben Thema. Die Entscheidung
                // liest ausschliesslich bereits vorhandenen Zustand und liefert eine bestehende
                // PlayRoutine zurueck; Bewegung, Ortswechsel und Effekte bleiben damit bei der
                // bisherigen Ausfuehrungsschicht.
                val contextualNodeId = requestedNodeId
                if (contextualNodeId != null) {
                    val node = AnimationTree.node(contextualNodeId)
                    if (node != null && AvatarActivityPlans.supportsContextualExecution(node)) {
                        val now = System.currentTimeMillis()
                        val (unlocked, avatarLevel) = withContext(Dispatchers.IO) {
                            val db = AppDatabase.getInstance(context)
                            val unlockedNodes = AvatarUnlockRepository(db).unlockedNodes(presenceProfileId)
                            val xp = db.avatarPlayStateDao().getForProfile(presenceProfileId)?.xp ?: 0
                            unlockedNodes to PlayModeXp.levelFor(xp)
                        }
                        val resolved = AvatarActivityPlans.resolve(
                            current = AvatarActivityBus.currentIfFresh(now),
                            dropped = node,
                            context = ActivityContext(
                                place = currentPlace,
                                unlockedNodeIds = unlocked,
                                avatarLevel = avatarLevel
                            )
                        )
                        if (resolved != null) {
                            currentTopic = resolved.topic
                            AvatarActivityBus.set(resolved.plan.resultingActivity, now)
                            // Absichtlich KEIN moveToPlace hier: Ob er lokal uebt oder erst zum
                            // Sportplatz geht, steckt als vorhandener GoToPlace-Schritt in genau
                            // dieser kontextuell aufgeloesten Routine.
                            runRoutine(resolved.routine, species)
                            requestedNodeId = null
                            requestedTopic = null
                            return@LaunchedEffect
                        }
                    }
                    // Defensive Rueckstufung: Wird ein Knoten spaeter entfernt oder noch nicht
                    // unterstuetzt, darf er die Schleife nicht blockieren. Das grobe Thema darunter
                    // laeuft dann auf demselben Weg weiter wie vor dieser Evolution.
                    requestedNodeId = null
                }

                requestedTopic?.let { topic ->
""",
    "contextual requested handler"
)
once(
    "                        onAsk = { topic -> talkOpen = false; requestedTopic = topic },",
    """                        onAsk = { topic ->
                            talkOpen = false
                            requestedNodeId = null
                            requestedTopic = topic
                        },""",
    "talk request reset"
)

dock.write_text(s)

evolution = Path("EVOLUTION.md")
e = evolution.read_text()
marker = "\n### Initialer Erkenntnisstand\n"
if e.count(marker) != 1:
    raise SystemExit(f"EVOLUTION marker: expected exactly one match, got {e.count(marker)}")
entry = """
### 2026-09-04 - Reminder werden zu kontextabhaengigen Handlungen: Fussball als erster Schnitt

- **Version:** Protokoll bleibt 0.5. Die Aenderung erweitert das bestehende Verhalten im Spielmodus;
  kein neues Datenmodell, keine Room-Migration und keine neue persistente Weltarchitektur.
- **Ausgangsproblem und Nutzerwirkung:** Reminder und freigeschaltete Skills endeten bislang trotz
  vorhandener mehrstufiger Play-Routinen oft in einer generischen Reaktion oder einer zufaelligen
  Einlage NACH der eigentlichen Alltagshandlung. Insbesondere schrumpfte eine Bibliotheksanimation
  wie `Football` nach dem Fuettern auf das grobe Thema `MOVE`; der folgende Ablauf kannte dadurch
  weder die genaue Absicht noch den Ort, an dem sie ausgesprochen wurde. Skillfortschritt war so
  nur eingeschraenkt am tatsaechlichen Verhalten des Wesens ablesbar.
- **Getroffene Architekturentscheidung:** Kein paralleler `ContextualActionResolver` und kein
  zweites Handlungssystem. Die bereits vorhandenen `AvatarActivityPlans`/`AvatarActivityBus`
  bilden die Entscheidungsebene; ihr Ergebnis ist eine vorhandene `PlayRoutine`, die weiterhin
  von `DockScreen.runRoutine`, `PlayScene`, `RoutineStep` und `PlayEffects` ausgefuehrt wird.
  `ReactionTrigger` behaelt den exakten Skillbaum-Knoten eines Reminders bis zu dieser Entscheidung.
- **Fussball als vertikaler Schnitt:** `sport/ballsport`, `dribbling` und `schuss` werden anhand
  von aktuellem `PlayScene.Place`, echten Freischaltungen und dem bereits vorhandenen Avatar-Level
  konkretisiert. Auf Sportplatz, Park und Wiese bleibt die Handlung lokal; aus ungeeigneten
  Innenraeumen verwendet sie den bestehenden `GoToPlace(SPORT)`-Schritt und damit den sichtbaren
  Weg statt eines Teleports. Ein Anfaenger zeigt nur Ballkontakt; freigeschaltetes Dribbling fuegt
  erkennbare Dribbling-Sequenzen hinzu; nur ein freigeschalteter Schuss darf `AIM`/`KICK` erzeugen.
  Hoehere Avatar-Level verlaengern ausschliesslich bereits gelernte Faehigkeiten und schalten keine
  Knoten heimlich frei.
- **Laufende Weltaktivitaet:** Eine ausgewaehlte Ballsport-Handlung wird im bereits vorhandenen
  `AvatarActivityBus` als aktuelle Aktivitaet gesetzt. Damit kann ein weiterer Stufe-3-Skill an
  eine laufende Ballsport-Aktivitaet anschliessen; der Zustand bleibt sitzungsgebunden und laeuft
  wie zuvor nach fuenf Minuten ab, statt als zweite Persistenzschicht gespeichert zu werden.
- **Rueckwaertskompatibilitaet:** Andere Skillbereiche behalten Claudes bisherigen
  `SkillRepertoire`-Flourish-Weg. Nur Knoten, die bereits kontextuell in eine Routine uebersetzt
  werden, sind dort ausgeschlossen, damit derselbe Skill nicht zusaetzlich als zufaellige
  Einzelanimation ein zweites Mal erscheint. Die generische `requestedTopic`-Logik bleibt der
  Rueckfall fuer alle nicht unterstuetzten Intents.
- **Spezies:** Im ersten Fussball-Schnitt bewusst kein Entscheidungsfaktor. Die vorhandenen
  Speziesanimationen werden weiterhin von `runRoutine` verwendet, aber es existiert noch keine
  belegte Produktregel, nach der eine Spezies andere Fussballfaehigkeiten besitzen darf als eine
  andere. Eine solche Regel waere eine gesonderte Produktentscheidung.
- **Tests:** `AvatarActivityPlansTest` prueft lokale Park-Ausfuehrung, sichtbaren Wechsel aus einem
  ungeeigneten Innenraum, Freischalt-Gating fuer Dribbling und Schuss sowie die Regel, dass ein
  hoeheres Avatar-Level nur bereits gelernte Faehigkeiten erweitert. Bestehende
  `SkillRepertoire`-Tests schuetzen die unveraenderte Rueckfalllogik der uebrigen Bereiche.
- **Daten und Migration:** Keine Schema-, Migration- oder neue Preference-Aenderung. Verwendet
  werden ausschliesslich bestehende Freischaltungen, XP/Level, `PlayScene.Place` und der
  sitzungsgebundene `AvatarActivityBus`.
- **Ausstehende Geraetepruefung:** Die objektiven Auswahlregeln sind automatisiert pruefbar; ob
  sich Anfaenger, Dribbling und Schuss beim Zuschauen deutlich genug voneinander unterscheiden,
  muss zusaetzlich am Geraet beurteilt werden.
"""
e = e.replace(marker, "\n" + entry.strip() + "\n" + marker, 1)
evolution.write_text(e)
