extends SceneTree

const WorldClock = preload("res://scripts/world_clock.gd")
var checks := 0
var failures := 0

func _initialize() -> void:
	call_deferred("run")

func check(ok: bool, label: String) -> void:
	checks += 1
	if not ok:
		failures += 1
		push_error(label)

func unix(stamp: String) -> float:
	return float(Time.get_unix_time_from_datetime_string(stamp))

func stamp(clock: RefCounted) -> String:
	return Time.get_datetime_string_from_datetime_dict(clock.local_datetime(), false)

func expect_time(clock: RefCounted, expected: String, label: String) -> void:
	check(stamp(clock) == expected, label + ": " + stamp(clock))

func run() -> void:
	var clock := WorldClock.new(unix("2026-01-15T12:34:56"))
	expect_time(clock, "2026-01-15T13:34:56", "Winterzeit in Berlin")
	check(clock.local_datetime().zone == "CET" and not clock.local_datetime().dst, "Winter hat CET und UTC+1")
	clock.utc_now_override = unix("2026-07-15T12:34:56")
	expect_time(clock, "2026-07-15T14:34:56", "Sommerzeit in Berlin")
	check(clock.local_datetime().zone == "CEST" and clock.local_datetime().offset_seconds == 7200, "Sommer hat CEST und UTC+2")
	for row in [
		["2026-03-29T00:59:59", "2026-03-29T01:59:59", 3600],
		["2026-03-29T01:00:00", "2026-03-29T03:00:00", 7200],
		["2026-10-25T00:59:59", "2026-10-25T02:59:59", 7200],
		["2026-10-25T01:00:00", "2026-10-25T02:00:00", 3600],
		["2025-03-30T01:00:00", "2025-03-30T03:00:00", 7200],
		["2025-10-26T01:00:00", "2025-10-26T02:00:00", 3600],
		["2027-03-28T01:00:00", "2027-03-28T03:00:00", 7200],
		["2027-10-31T01:00:00", "2027-10-31T02:00:00", 3600]]:
		clock.utc_now_override = unix(row[0])
		expect_time(clock, row[1], "Letzter Sonntag / Umstellung " + row[0])
		check(clock.local_datetime().offset_seconds == row[2], "UTC-Offset an Grenze " + row[0])
	clock.utc_now_override = unix("2026-03-28T12:00:00")
	clock.advance_hours(24.0)
	expect_time(clock, "2026-03-29T14:00:00", "24 echte Stunden Lesen ueber Fruehjahrswechsel")
	check(is_equal_approx(clock.sleep_until_morning(), 18.0 * 3600.0), "Schlafen nach Lesen addiert zum vorhandenen Versatz")
	expect_time(clock, "2026-03-30T08:00:00", "Lesen und Schlafen bilden eine gemeinsame Zeitleiste")
	clock.reset_to_real_time()
	clock.utc_now_override = unix("2026-10-24T12:00:00")
	clock.advance_hours(24.0)
	expect_time(clock, "2026-10-25T13:00:00", "24 echte Stunden Lesen ueber Herbstwechsel")
	check(is_equal_approx(clock.sleep_until_morning(), 19.0 * 3600.0), "Schlafdauer nach Herbst-Lesesprung")
	expect_time(clock, "2026-10-26T08:00:00", "Schlafen nach Herbst-Lesesprung")
	clock.reset_to_real_time()
	check(clock.utc_offset == 0.0, "Neuer Besuch setzt nur Spielzeitversatz zurueck")
	expect_time(clock, "2026-10-24T14:00:00", "Neuer Besuch folgt wieder der Echtzeit")
	clock.utc_now_override += 90.0
	expect_time(clock, "2026-10-24T14:01:30", "Echtzeit laeuft auch ohne Spielaktionen weiter")
	for row in [
		["2026-01-15T21:30:00", "2026-01-16T08:00:00", 9.5],
		["2026-07-15T20:30:00", "2026-07-16T08:00:00", 9.5],
		["2026-03-28T21:30:00", "2026-03-29T08:00:00", 8.5],
		["2026-10-24T20:30:00", "2026-10-25T08:00:00", 10.5],
		["2026-03-29T00:30:00", "2026-03-29T08:00:00", 5.5],
		["2026-10-25T00:30:00", "2026-10-25T08:00:00", 6.5],
		["2026-10-25T01:30:00", "2026-10-25T08:00:00", 5.5],
		["2026-01-15T04:00:00", "2026-01-15T08:00:00", 3.0],
		["2026-01-15T07:00:00", "2026-01-16T08:00:00", 24.0],
		["2026-12-31T22:30:00", "2027-01-01T08:00:00", 8.5]]:
		clock.reset_to_real_time()
		clock.utc_now_override = unix(row[0])
		var elapsed: float = clock.sleep_until_morning()
		expect_time(clock, row[1], "Schlafen bis 08:00 " + row[0])
		check(is_equal_approx(elapsed, float(row[2]) * 3600.0) and elapsed > 0.0,
			"Schlafdauer benutzt Zieltag-Offset " + row[0])
		check(clock.current_hour() == 8 and clock.phase() == "day", "Aufwachen ergibt Tag")
	clock.reset_to_real_time()
	clock.utc_now_override = unix("2026-07-15T09:00:00")
	clock.advance_hours(0.5)
	expect_time(clock, "2026-07-15T11:30:00", "Halbe Stunde Lesen")
	check(clock.local_datetime(unix("2026-01-15T09:00:00")).hour == 10,
		"Expliziter UTC-Eingang nutzt seinen eigenen Winter-Offset plus Spielversatz")
	for row in [[5, "night"], [6, "dawn"], [7, "dawn"], [8, "day"], [17, "day"],
		[18, "dusk"], [20, "dusk"], [21, "night"], [23, "night"]]:
		clock.reset_to_real_time()
		clock.utc_now_override = unix("2026-01-15T00:00:00") + (float(row[0]) - 1.0) * 3600.0
		check(clock.current_hour() == row[0] and clock.phase() == row[1], "Tagesphase bei " + str(row[0]))
	clock.reset_to_real_time()
	clock.utc_now_override = -1.0
	check(absf(clock.effective_utc() - Time.get_unix_time_from_system()) < 2.0, "Standardquelle ist echte System-UTC-Zeit")
	print("world_clock: %d checks, %d failures" % [checks, failures])
	quit(1 if failures > 0 else 0)
