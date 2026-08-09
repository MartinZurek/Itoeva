package com.notime.glyphcore.data

import kotlinx.coroutines.flow.Flow

class LibraryAnimationRepository(private val dao: LibraryAnimationDao) {

    fun observeAll(): Flow<List<LibraryAnimation>> = dao.observeAll()

    /**
     * Legt die mitgelieferten Animationen an und haelt ihre Zeichnung aktuell.
     *
     * **Warum nicht nur beim ersten Start.** Vorher hiess das `seedIfEmpty` und brach ab, sobald
     * irgendetwas in der Bibliothek stand. Eine korrigierte Zeichnung - etwa TAMA, dessen
     * Buchstaben zu klein waren und beim Ueberblenden ineinanderliefen - erreichte damit nur
     * Neuinstallationen. Wer die App schon nutzte, behielt die alte Fassung dauerhaft, ohne
     * Moeglichkeit das zu aendern ausser die App-Daten zu loeschen (und damit Erinnerungen und
     * Fuetterungshistorie gleich mit).
     *
     * Abgeglichen wird ueber das Label. Aktualisiert werden ausschliesslich Posen und Emoji;
     * [LibraryAnimation.isSelected] bleibt unberuehrt, weil die Auswahl im Picker dem Nutzer
     * gehoert. Selbst hinzugefuegte Animationen bleiben ebenfalls unangetastet - sie tragen kein
     * Label aus [DefaultLibraryAnimations].
     */
    suspend fun seedOrRefresh() {
        val defaults = DefaultLibraryAnimations.seed()
        val existing = dao.allLabels().toSet()

        val missing = defaults.filter { it.label !in existing }
        if (missing.isNotEmpty()) dao.insertAll(missing)

        defaults.filter { it.label in existing }
            .forEach { dao.updateArtwork(it.label, it.framesData, it.emoji) }
    }

    /**
     * Waehlt aus bzw. ab. Beim AUSWAEHLEN entscheidet die Datenbank in derselben Anweisung, ob
     * noch Platz im Picker ist - `false` heisst abgelehnt, weil [limit] erreicht war. Abwaehlen
     * gelingt immer. Siehe [LibraryAnimationDao.selectIfWithinLimit].
     */
    suspend fun setSelected(id: Long, selected: Boolean, limit: Int): Boolean {
        if (!selected) {
            dao.setSelected(id, false)
            return true
        }
        if (dao.selectIfWithinLimit(id, limit) > 0) return true
        // Keine Zeile geaendert heisst nicht zwingend "abgelehnt": es kann auch sein, dass die
        // Animation laengst ausgewaehlt war (die Abfrage klammert solche Zeilen bewusst aus, sonst
        // zaehlten sie ein zweites Mal gegen die Obergrenze). Der gewuenschte Zustand ist dann
        // erreicht - dem Nutzer hier "Picker voll" zu melden, waere schlicht falsch.
        return dao.isSelected(id) == true
    }

    /** Anzahl aktuell im Picker ausgewaehlter Bibliotheks-Animationen (fuer die Gesamt-Obergrenze, siehe ViewModel). */
    suspend fun countSelected(): Int = dao.countSelected()

    /**
     * Liefert die abspielbaren Frames einer Bibliotheks-Animation, oder null falls geloescht.
     * Die in [LibraryAnimation.framesData] gespeicherten Punktlisten sind reine An/Aus-Posen
     * ("Keyframes") - [FrameCrossfade.withCrossfades] verbindet sie mit denselben weichen
     * Helligkeits-Ueberblendungen wie die 12 fest eingebauten AnimationTypes, statt sie hart
     * hintereinander zu zeigen (siehe FrameCrossfade.kt).
     */
    suspend fun framesFor(id: Long): List<IntArray>? {
        val animation = dao.getById(id) ?: return null
        val keyframes = FrameCodec.decode(animation.framesData)
        return FrameCrossfade.withCrossfades(ReminderFrameGrid.SIZE, keyframes)
    }
}
