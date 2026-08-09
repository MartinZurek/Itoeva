# Regeln, die JEDE App erbt, die :core einbindet (siehe consumerProguardFiles in
# core/build.gradle.kts). Hier stehen nur Dinge, die der Kern selbst mitbringt - was die Apps
# darueber hinaus brauchen, steht in ihrer eigenen proguard-rules.pro.

# ---------------------------------------------------------------------------------------------
# Enums, die als NAME persistiert werden
# ---------------------------------------------------------------------------------------------
# AnimationType landet ueber data/Converters.kt als String in der Datenbank und wird mit
# valueOf() zurueckgelesen. Benennt R8 die Konstanten um (aus GENERAL wird a), findet
# valueOf("GENERAL") sie nicht mehr.
#
# Besonders heimtueckisch: beide Lesestellen fangen den Fehler mit runCatching{} ab und fallen
# auf einen Standardwert zurueck. Es gaebe also keinen Absturz - die Erinnerungen wuerden nur
# stillschweigend die falsche Animation zeigen und Einstellungen sich zuruecksetzen. Ein Fehler,
# der ausschliesslich im Release-Build auftritt und ohne diese Regel kaum zu finden waere.
-keepclassmembers enum com.notime.glyphcore.** {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ---------------------------------------------------------------------------------------------
# WorkManager-Worker
# ---------------------------------------------------------------------------------------------
# WorkManager erzeugt Worker ueber ihren Klassennamen (der Name steht in WorkManagers eigener
# Datenbank und ueberlebt App-Updates). Wird die Klasse umbenannt oder wegoptimiert, findet
# WorkManager sie nicht mehr - der Watchdog liefe nie wieder, ausgerechnet das Sicherheitsnetz
# gegen stille Ausfaelle. Der Konstruktor muss mit erhalten bleiben, er wird reflektiv aufgerufen.
#
# Bewusst eine Regel fuer ALLE Worker des Kerns statt einer je Klasse: hier stand vorher nur der
# Watchdog namentlich, und der zweite Worker (ReminderRescheduleWorker, plant nach Reboot und
# App-Update alles neu) waere ohne eigenen Eintrag stillschweigend durchs Raster gefallen. Der
# Fehler haette sich ausschliesslich im Release gezeigt, und zwar als "nach einem Neustart kommen
# keine Erinnerungen mehr" - also genau da, wo er am teuersten ist.
-keep class com.notime.glyphcore.reminder.**Worker {
    <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ---------------------------------------------------------------------------------------------
# Room-Entities
# ---------------------------------------------------------------------------------------------
# Room greift ueber generierten Code zu, nicht reflektiv - die Entities selbst braeuchten keine
# Regel. Ihre Feldnamen tauchen aber in den generierten SQL-Abfragen als Spaltennamen auf; die
# Klassen zu behalten haelt Schema und Code garantiert deckungsgleich und kostet praktisch nichts.
-keep class com.notime.glyphcore.data.GlyphReminder { *; }
-keep class com.notime.glyphcore.data.LibraryAnimation { *; }
-keep class com.notime.glyphcore.data.BuiltInAnimationSelection { *; }
