from pathlib import Path
import re


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly one match, got {count}")
    return text.replace(old, new, 1)


# 1) Context data: keep only decisions that are actually approved today.
path = Path("app-sim/src/main/java/com/notime/glyphsim/skilltree/AvatarActivity.kt")
s = path.read_text()
s = replace_once(
    s,
    """ * Kein zweiter Weltzustand: [place] kommt direkt aus `DockScreen.currentPlace`, die Freischaltungen
 * aus [AvatarUnlockRepository], die Stufe aus dem vorhandenen Play-Mode-Level. Die Ausfuehrung
 * bleibt vollstaendig bei [PlayRoutine] und `DockScreen.runRoutine`.
 */
data class ActivityContext(
    val place: PlayScene.Place,
    val unlockedNodeIds: Set<String>,
    val avatarLevel: Int
)
""",
    """ * Kein zweiter Weltzustand: [place] kommt direkt aus `DockScreen.currentPlace`, die Freischaltungen
 * aus [AvatarUnlockRepository]. Welche Avatar-Level welche Ablaufvarianten freischalten, ist laut
 * `Tagesablauf.md` weiterhin eine offene Produktentscheidung und wird hier deshalb bewusst NICHT
 * vorweggenommen. Die Ausfuehrung bleibt vollstaendig bei [PlayRoutine] und `DockScreen.runRoutine`.
 */
data class ActivityContext(
    val place: PlayScene.Place,
    val unlockedNodeIds: Set<String>
)
""",
    "ActivityContext level removal",
)
s = replace_once(
    s,
    """            if (dribblingLearned) {
                // Gelerntes Dribbling wird als erkennbare Folge sichtbar. Hoehere Avatar-Stufen
                // verlaengern nur eine BEREITS gelernte Faehigkeit; sie schalten nichts heimlich
                // frei.
                add(RoutineStep.Stroll((anchor + 0.16f).coerceAtMost(0.72f)))
                add(RoutineStep.Football(PlayEffects.FootballPhase.DRIBBLE))
                add(RoutineStep.Linger(5_000L))
                if (context.avatarLevel >= 3) {
                    add(RoutineStep.Stroll((anchor - 0.10f).coerceAtLeast(0.18f)))
                    add(RoutineStep.Football(PlayEffects.FootballPhase.DRIBBLE))
                    add(RoutineStep.Linger(4_000L))
                }
            }

            if (shotLearned) {
                add(RoutineStep.Football(PlayEffects.FootballPhase.AIM))
                add(RoutineStep.Linger(3_000L))
                add(RoutineStep.Football(PlayEffects.FootballPhase.KICK))
                add(RoutineStep.Linger(if (context.avatarLevel >= 4) 7_000L else 5_000L))
            }
""",
    """            if (dribblingLearned) {
                // Gelerntes Dribbling wird als erkennbare Folge sichtbar. Eine zusaetzliche
                // Level-Schwelle gibt es bewusst nicht: Welche Level Varianten freischalten,
                // bleibt laut Tagesablauf.md eine offene Produktentscheidung.
                add(RoutineStep.Stroll((anchor + 0.16f).coerceAtMost(0.72f)))
                add(RoutineStep.Football(PlayEffects.FootballPhase.DRIBBLE))
                add(RoutineStep.Linger(5_000L))
            }

            if (shotLearned) {
                add(RoutineStep.Football(PlayEffects.FootballPhase.AIM))
                add(RoutineStep.Linger(3_000L))
                add(RoutineStep.Football(PlayEffects.FootballPhase.KICK))
                add(RoutineStep.Linger(5_000L))
            }
""",
    "remove level gated football variants",
)
path.write_text(s)

# 2) DockScreen no longer performs an unnecessary XP lookup for an unapproved level rule.
path = Path("app-sim/src/main/java/com/notime/glyphsim/ui/DockScreen.kt")
s = path.read_text()
s = replace_once(
    s,
    """                        val (unlocked, avatarLevel) = withContext(Dispatchers.IO) {
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
""",
    """                        val unlocked = withContext(Dispatchers.IO) {
                            val db = AppDatabase.getInstance(context)
                            AvatarUnlockRepository(db).unlockedNodes(presenceProfileId)
                        }
                        val resolved = AvatarActivityPlans.resolve(
                            current = AvatarActivityBus.currentIfFresh(now),
                            dropped = node,
                            context = ActivityContext(
                                place = currentPlace,
                                unlockedNodeIds = unlocked
                            )
                        )
""",
    "DockScreen level lookup removal",
)
path.write_text(s)

# 3) Autonomous MOVE must keep showing contextual football unlocks until the ambient path itself
# resolves them contextually. Exact reminder execution returns before this flourish path.
path = Path("app-sim/src/main/java/com/notime/glyphsim/skilltree/SkillRepertoire.kt")
s = path.read_text()
s = replace_once(
    s,
    """ * Der erste Stand spielte jeden Skill als kurze Einlage NACH einer ansonsten unveraenderten
 * Handlung. Das bleibt der Rueckfall fuer Bereiche, die noch keinen eigenen vertikalen Schnitt
 * besitzen. Sobald ein Knoten aber von [AvatarActivityPlans] kontextuell aufgeloest wird, gehoert
 * er IN die Handlung selbst und darf hier nicht ein zweites Mal gewuerfelt werden. So entstehen
 * keine zwei konkurrierenden Handlungssysteme: Der Skillbaum entscheidet das Repertoire, die
 * vorhandene PlayRoutine fuehrt es aus.
""",
    """ * Der erste Stand spielte jeden Skill als kurze Einlage NACH einer ansonsten unveraenderten
 * Handlung. Bei einem EXAKTEN Reminder-/Skill-Intent kann [AvatarActivityPlans] ihn inzwischen
 * stattdessen IN eine kontextuelle Routine uebersetzen; dieser Pfad kehrt in `DockScreen` vor dem
 * Ambient-Flourish zurueck und kann daher nicht doppelt spielen. Der autonome PERFORM-Pfad besitzt
 * dagegen noch keinen exakten Intent. Dort bleibt die Einlage absichtlich erhalten, damit eine
 * freigeschaltete Fussballfaehigkeit im normalen Avatarleben nicht unsichtbar wird.
""",
    "SkillRepertoire architecture docs",
)
s = replace_once(
    s,
    """     * Alle freigeschalteten, gezeichneten Knoten UNTERHALB des Themas, die noch als klassische
     * Einlage gebraucht werden - in Baumreihenfolge.
     *
     * Der Wirtsknoten selbst fehlt bewusst: Seine Reaktion IST die Handlung, die der Ablauf gerade
     * gespielt hat. Kontextuell ausgefuehrte Skills fehlen ebenfalls: Deren Freischaltung hat die
     * Routine bereits veraendert und soll nicht danach nochmals als Einzelanimation auftauchen.
""",
    """     * Alle freigeschalteten, gezeichneten Knoten UNTERHALB des Themas - in Baumreihenfolge.
     *
     * Der Wirtsknoten selbst fehlt bewusst: Seine Reaktion IST die Handlung, die der Ablauf gerade
     * gespielt hat. Kontextuell aufloesbare Skills bleiben hier enthalten, weil dieser Leser den
     * autonomen PERFORM-Pfad versorgt; der exakte Contextual-Intent-Pfad ruft ihn nicht auf.
""",
    "SkillRepertoire skillsFor docs",
)
s = replace_once(
    s,
    """                    node.id in unlocked &&
                    host in AnimationTree.fallbackChain(node.id) &&
                    !AvatarActivityPlans.supportsContextualExecution(node)
""",
    """                    node.id in unlocked &&
                    host in AnimationTree.fallbackChain(node.id)
""",
    "retain contextual skills in ambient repertoire",
)
path.write_text(s)

# 4) Tests: remove obsolete avatarLevel arguments and replace the balancing test.
path = Path("app-sim/src/test/java/com/notime/glyphsim/skilltree/AvatarActivityPlansTest.kt")
s = path.read_text()
s, n = re.subn(r",\n\s*avatarLevel = \d+", "", s)
if n < 6:
    raise SystemExit(f"AvatarActivityPlansTest avatarLevel removals: expected >=6, got {n}")
old = """    @Test
    fun `Avatar Level verlaengert nur bereits gelerntes Dribbling`() {
        val levelOne = requireNotNull(
            AvatarActivityPlans.resolve(
                current = null,
                dropped = node("sport/ballsport"),
                context = ActivityContext(
                    place = PlayScene.Place.SPORT,
                    unlockedNodeIds = setOf(
                        "sport",
                        "sport/ballsport",
                        "sport/ballsport/dribbling"
                    )
                )
            )
        )
        val levelThree = requireNotNull(
            AvatarActivityPlans.resolve(
                current = null,
                dropped = node("sport/ballsport"),
                context = ActivityContext(
                    place = PlayScene.Place.SPORT,
                    unlockedNodeIds = setOf(
                        "sport",
                        "sport/ballsport",
                        "sport/ballsport/dribbling"
                    )
                )
            )
        )

        assertEquals(1, footballPhases(levelOne).count { it == PlayEffects.FootballPhase.DRIBBLE })
        assertEquals(2, footballPhases(levelThree).count { it == PlayEffects.FootballPhase.DRIBBLE })
    }

"""
new = """    @Test
    fun `Dribbling Variante haengt nur an der Freischaltung nicht an einer erfundenen Levelschwelle`() {
        val resolved = requireNotNull(
            AvatarActivityPlans.resolve(
                current = null,
                dropped = node("sport/ballsport"),
                context = ActivityContext(
                    place = PlayScene.Place.SPORT,
                    unlockedNodeIds = setOf(
                        "sport",
                        "sport/ballsport",
                        "sport/ballsport/dribbling"
                    )
                )
            )
        )

        assertEquals(1, footballPhases(resolved).count { it == PlayEffects.FootballPhase.DRIBBLE })
    }

"""
s = replace_once(s, old, new, "replace level behavior test")
path.write_text(s)

path = Path("app-sim/src/test/java/com/notime/glyphsim/skilltree/SkillRepertoireTest.kt")
s = path.read_text()
marker = """    /**
     * Die Garantie liegt im Baum, nicht in einer Pruefung hier: MEDICINE steht in
"""
addition = """    @Test
    fun `kontextuelle Fussballskills bleiben im autonomen MOVE Repertoire sichtbar`() {
        val offen = setOf("sport/ballsport", "sport/ballsport/dribbling", "sport/ballsport/schuss")
        val skills = SkillRepertoire.skillsFor(AnimationType.MOVE, offen)
        assertTrue("Dribbling fehlt im autonomen Repertoire", "sport/ballsport/dribbling" in skills)
        assertTrue("Schuss fehlt im autonomen Repertoire", "sport/ballsport/schuss" in skills)
    }

"""
if addition not in s:
    s = replace_once(s, marker, addition + marker, "add ambient contextual skill regression")
path.write_text(s)

# 5) Evolution history: correct the level claim and explain the dual execution paths.
path = Path("EVOLUTION.md")
s = path.read_text()
s = replace_once(
    s,
    """- **Fussball als vertikaler Schnitt:** `sport/ballsport`, `dribbling` und `schuss` werden anhand
  von aktuellem `PlayScene.Place`, echten Freischaltungen und dem bereits vorhandenen Avatar-Level
  konkretisiert. Auf Sportplatz, Park und Wiese bleibt die Handlung lokal; aus ungeeigneten
  Innenraeumen verwendet sie den bestehenden `GoToPlace(SPORT)`-Schritt und damit den sichtbaren
  Weg statt eines Teleports. Ein Anfaenger zeigt nur Ballkontakt; freigeschaltetes Dribbling fuegt
  erkennbare Dribbling-Sequenzen hinzu; nur ein freigeschalteter Schuss darf `AIM`/`KICK` erzeugen.
  Hoehere Avatar-Level verlaengern ausschliesslich bereits gelernte Faehigkeiten und schalten keine
  Knoten heimlich frei.
""",
    """- **Fussball als vertikaler Schnitt:** `sport/ballsport`, `dribbling` und `schuss` werden anhand
  von aktuellem `PlayScene.Place` und echten Freischaltungen konkretisiert. Auf Sportplatz, Park
  und Wiese bleibt die Handlung lokal; aus ungeeigneten Innenraeumen verwendet sie den bestehenden
  `GoToPlace(SPORT)`-Schritt und damit den sichtbaren Weg statt eines Teleports. Ein Anfaenger zeigt
  nur Ballkontakt; freigeschaltetes Dribbling fuegt eine erkennbare Dribbling-Sequenz hinzu; nur ein
  freigeschalteter Schuss darf `AIM`/`KICK` erzeugen. Avatar-Level wird bewusst noch NICHT fuer
  Varianten benutzt: Welche Level welche Ablaufe freischalten, ist in `Tagesablauf.md` weiterhin
  eine `OPEN DECISION` und wird durch diesen Schnitt nicht vorweggenommen.
""",
    "EVOLUTION level decision",
)
s = replace_once(
    s,
    """- **Rueckwaertskompatibilitaet:** Andere Skillbereiche behalten Claudes bisherigen
  `SkillRepertoire`-Flourish-Weg. Nur Knoten, die bereits kontextuell in eine Routine uebersetzt
  werden, sind dort ausgeschlossen, damit derselbe Skill nicht zusaetzlich als zufaellige
  Einzelanimation ein zweites Mal erscheint. Die generische `requestedTopic`-Logik bleibt der
  Rueckfall fuer alle nicht unterstuetzten Intents.
""",
    """- **Rueckwaertskompatibilitaet:** Andere Skillbereiche behalten Claudes bisherigen
  `SkillRepertoire`-Flourish-Weg. Auch kontextuell aufloesbare Fussballknoten bleiben im autonomen
  PERFORM-Repertoire sichtbar, solange dieser Pfad keinen exakten Skill-Intent besitzt. Eine
  Doppelung entsteht nicht: Der exakte Reminder-/Skill-Pfad fuehrt die kontextuelle Routine aus und
  kehrt davor zurueck, statt danach noch `SkillRepertoire.pick` aufzurufen. Die generische
  `requestedTopic`-Logik bleibt der Rueckfall fuer alle nicht unterstuetzten Intents.
""",
    "EVOLUTION ambient compatibility",
)
s = replace_once(
    s,
    """- **Tests:** `AvatarActivityPlansTest` prueft lokale Park-Ausfuehrung, sichtbaren Wechsel aus einem
  ungeeigneten Innenraum, Freischalt-Gating fuer Dribbling und Schuss sowie die Regel, dass ein
  hoeheres Avatar-Level nur bereits gelernte Faehigkeiten erweitert. Bestehende
  `SkillRepertoire`-Tests schuetzen die unveraenderte Rueckfalllogik der uebrigen Bereiche.
- **Daten und Migration:** Keine Schema-, Migration- oder neue Preference-Aenderung. Verwendet
  werden ausschliesslich bestehende Freischaltungen, XP/Level, `PlayScene.Place` und der
  sitzungsgebundene `AvatarActivityBus`.
""",
    """- **Tests:** `AvatarActivityPlansTest` prueft lokale Park-Ausfuehrung, sichtbaren Wechsel aus einem
  ungeeigneten Innenraum sowie striktes Freischalt-Gating fuer Dribbling und Schuss. Ein eigener
  Regressionstest in `SkillRepertoireTest` schuetzt, dass diese Unlocks im autonomen MOVE-Alltag
  weiterhin sichtbar bleiben. Level-Schwellen werden ausdruecklich nicht getestet oder erfunden.
- **Daten und Migration:** Keine Schema-, Migration- oder neue Preference-Aenderung. Verwendet
  werden ausschliesslich bestehende Freischaltungen, `PlayScene.Place` und der sitzungsgebundene
  `AvatarActivityBus`.
""",
    "EVOLUTION tests and data",
)
path.write_text(s)

# 6) Authoritative skill-tree handoff.
path = Path("SKILLBAUM.md")
s = path.read_text()
s = replace_once(
    s,
    """- `AvatarActivityBus` bleibt unbenutzt. Die Einlage hier braucht keinen mitgeführten Zustand, und
  ein Schreiber ohne Leser wäre nur eine zweite Karteileiche.
""",
    """- `AvatarActivityBus` war in P15 noch unbenutzt. Seit dem kontextuellen Fussball-Schnitt vom
  2026-09-04 hat er einen echten Leser und Schreiber: Ein exakter Ballsport-/Dribbling-/Schuss-Intent
  setzt die laufende Ballsport-Aktivitaet, damit eine folgende Stufe-3-Faehigkeit an dieselbe
  Beschaeftigung anschliessen kann. Der Zustand bleibt bewusst sitzungsgebunden und laeuft ab.
""",
    "SKILLBAUM AvatarActivityBus handoff",
)
s = replace_once(
    s,
    """      **gezeichneten** Nachkommen; der Wirtsknoten selbst fehlt bewusst (seine Reaktion IST die
      Handlung, die gerade lief — sie zu wiederholen wäre ein Echo, keine Fähigkeit).
""",
    """      **gezeichneten** Nachkommen; der Wirtsknoten selbst fehlt bewusst (seine Reaktion IST die
      Handlung, die gerade lief — sie zu wiederholen wäre ein Echo, keine Fähigkeit). Seit dem
      kontextuellen Fussball-Schnitt bleiben auch dessen Stufe-3-Knoten hier enthalten: Der autonome
      PERFORM-Pfad hat noch keinen exakten Skill-Intent und wuerde sie sonst komplett verlieren.
""",
    "SKILLBAUM repertoire handoff",
)
journal_marker = "| 2026-09-03 | P15 |"
if journal_marker not in s:
    raise SystemExit("SKILLBAUM journal marker missing")
new_row = "| 2026-09-04 | Kontextueller Fussball-Schnitt | **Reminder/Skills werden zu Absichten in der vorhandenen Welt.** `AvatarActivityPlans` loest Ballsport/Dribbling/Schuss anhand von Ort und echten Unlocks in bestehende `PlayRoutine`-Schritte auf; `DockScreen` behaelt dafuer den exakten `ReactionTrigger.Node`, und `AvatarActivityBus` ist jetzt tatsaechlich aktiv. Park/Wiese bleiben lokal, ungeeignete Innenraeume nutzen den sichtbaren `GoToPlace(SPORT)`-Weg. `TOUCH` ist die Anfaenger-Basis; `DRIBBLE` sowie `AIM`/`KICK` erscheinen nur nach echter Freischaltung. **Keine Level-Schwellen erfunden:** Welche Level Varianten freischalten, bleibt laut `Tagesablauf.md` offen. Im autonomen PERFORM-Pfad bleiben dieselben Fussball-Unlocks weiterhin als `SkillRepertoire`-Einlagen sichtbar, bis auch dieser Pfad einen exakten kontextuellen Intent besitzt. Fuer die naechste Sitzung: dieselbe Architektur nur vertikal fuer weitere Skills ausbauen, nicht daneben ein zweites Handlungssystem beginnen. |\n"
if new_row not in s:
    s = s.replace(journal_marker, new_row + journal_marker, 1)
path.write_text(s)
