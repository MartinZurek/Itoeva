# Nachlieferung der geprueften APK0.9.0

Alle Godot-Regressions- und Renderpruefungen sowie Export, Paket- und Signaturpruefung in Lauf39020500282 bestanden. Nur der abschliessende grosse Drive-PUT scheiterte mit HTTP502. Der direkte Connector-Ersatzweg lehnt Dateien ueber100MiB ab; die APK besitzt153846029Bytes.

Der begrenzte Nachlieferworkflow laedt deshalb ausschliesslich das bereits gepruefte Artefakt dieses Laufs. Er prueft Quellcommit, Paket, Zertifikat und festgehaltene Byte-/Hashwerte vor jeder Uebertragung. Danach uebertraegt er8MiB-Abschnitte und fragt bei Unterbrechung den vom Server bestaetigten Offset ab. Abschliessend vergleicht er Remote-Groesse, MD5 und gegebenenfalls SHA256.

Quellcommit: da608c141a69d77442a7ef6fe66e36b8aee8dc59. APK-SHA256: aaede525ab655be715b631ba883c1244bd0e1892e61a666db439078617d02201. MD5:91ceb276f2d5405f756dd1a585b99cb1. Signaturzertifikat unveraendert gegenueber0.8.0:30dbcf9daa7c26e4f76a1c2da674ee8b5e732186a058081e2dabd34eb82d58dc.

Keine App-Quell- oder Signierschluessel-Aenderung. Dieselbe bereits freigegebene Drive-Zieldatei und dasselbe vorhandene Servicekonto. `actions:read` dient allein dem Download des bezeichneten Artefakts; sonst nur Repository-Leserecht. Geheimnisse und Upload-Sitzung bleiben im temporaeren Verzeichnis und werden entfernt. Nur der abschliessende Lieferlauf entscheidet ueber den Uploadstatus.
