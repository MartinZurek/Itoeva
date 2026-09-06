package androidx.room

// Room-Platzhalter. `LibraryAnimation` ist eine Room-Entitaet, wird im Offline-Harness aber nur
// als Datenklasse gebraucht - fuer die Bibliotheks-Motive im Fingerabdruck-Test. Die Annotationen
// tragen keine Bedeutung fuer das, was hier geprueft wird; Room selbst laeuft nur auf dem Geraet.
@Retention(AnnotationRetention.SOURCE) annotation class Entity(val tableName: String = "")
@Retention(AnnotationRetention.SOURCE) annotation class PrimaryKey(val autoGenerate: Boolean = false)
