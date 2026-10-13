extends RefCounted

const HOUR_SECONDS := 3600.0
const DAY_SECONDS := 86400.0
const MORNING_HOUR := 8
var utc_now_override := -1.0
var utc_offset := 0.0

func _init(utc_override: float = -1.0) -> void:
	utc_now_override = utc_override

func utc_now() -> float:
	return utc_now_override if utc_now_override >= 0.0 else Time.get_unix_time_from_system()

func effective_utc(utc_unix: float = -1.0) -> float:
	return (utc_unix if utc_unix >= 0.0 else utc_now()) + utc_offset

func berlin_offset_seconds(utc_unix: float) -> int:
	# Godots Unix-Konvertierung beruecksichtigt keine Zeitzone. Die aktuelle
	# EU-Regel wechselt jeweils am letzten Sonntag um 01:00 UTC.
	var year: int = Time.get_datetime_dict_from_unix_time(int(utc_unix)).year
	var spring := _last_sunday_switch(year, 3)
	var autumn := _last_sunday_switch(year, 10)
	return 7200 if utc_unix >= spring and utc_unix < autumn else 3600

func _last_sunday_switch(year: int, month: int) -> int:
	var last_day := Time.get_unix_time_from_datetime_dict({"year": year, "month": month,
		"day": 31, "hour": 1, "minute": 0, "second": 0})
	var weekday: int = Time.get_datetime_dict_from_unix_time(last_day).weekday
	return last_day - weekday * 86400

func local_datetime(utc_unix: float = -1.0) -> Dictionary:
	var utc := effective_utc(utc_unix)
	var offset := berlin_offset_seconds(utc)
	var result := Time.get_datetime_dict_from_unix_time(int(utc + offset))
	result["dst"] = offset == 7200
	result["offset_seconds"] = offset
	result["zone"] = "CEST" if offset == 7200 else "CET"
	return result

func current_hour() -> int:
	return int(local_datetime().hour)

func phase() -> String:
	var hour := current_hour()
	if hour >= 6 and hour < 8: return "dawn"
	if hour >= 8 and hour < 18: return "day"
	if hour >= 18 and hour < 21: return "dusk"
	return "night"

func advance_hours(hours: float) -> void:
	if is_finite(hours) and hours > 0.0:
		utc_offset += hours * HOUR_SECONDS

func sleep_until_morning() -> float:
	var current_utc := effective_utc()
	var local := local_datetime()
	var morning_wall := float(Time.get_unix_time_from_datetime_dict({"year": local.year,
		"month": local.month, "day": local.day, "hour": MORNING_HOUR,
		"minute": 0, "second": 0}))
	if int(local.hour) >= MORNING_HOUR:
		morning_wall += DAY_SECONDS
	# 08:00 existiert auch an beiden Zeitumstellungstagen eindeutig. Erst den
	# naechsten lokalen Morgen waehlen, dann dessen eigenen UTC-Offset bestimmen.
	var target_utc := morning_wall - HOUR_SECONDS
	for iteration in range(2):
		target_utc = morning_wall - berlin_offset_seconds(target_utc)
	var elapsed := target_utc - current_utc
	utc_offset += elapsed
	return elapsed

func reset_to_real_time() -> void:
	# Der Sitzungsbeginn setzt nur gespielte Zeit zurueck; Tuerwechsel nicht.
	utc_offset = 0.0
