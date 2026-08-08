# App-spezifische R8-Regeln. Was der gemeinsame Kern braucht, kommt automatisch ueber
# core/consumer-rules.pro dazu (Enums der Datenschicht, Watchdog-Worker, Room-Entities).

# ---------------------------------------------------------------------------------------------
# Enums, die als NAME in den SharedPreferences liegen
# ---------------------------------------------------------------------------------------------
# ClockStyle (gewaehltes Zifferblatt) und AvatarSpecies (gewaehlter Avatar) werden ueber .name
# gespeichert und mit valueOf() zurueckgelesen - dieselbe Falle wie bei den Kern-Enums: ohne
# diese Regel benennt R8 die Konstanten um, valueOf() scheitert, und weil beide Lesestellen
# runCatching{} verwenden, faellt die App still auf CLASSIC bzw. PUFFLING zurueck. Der Nutzer
# saehe nach dem Update seine Einstellungen zurueckgesetzt, ohne jede Fehlermeldung.
-keepclassmembers enum com.notime.glyphsim.** {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ---------------------------------------------------------------------------------------------
# Room-Entity dieses Moduls
# ---------------------------------------------------------------------------------------------
# Die Fuetterungs-Historie des Tamagotchi liegt nur in dieser App (siehe AppDatabase).
-keep class com.notime.glyphsim.data.AvatarFeedEvent { *; }
