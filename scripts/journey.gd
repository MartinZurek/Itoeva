extends RefCounted


const Routes = preload("res://scripts/world_routes.gd")
const TITLE = "Die Wacht am Schattenwald"
const INTRO = "Zwischen Dorf, Hain und Mühlenbach leben die Bewohner ihren Alltag. Doch hinter den letzten Fackeln beginnt Nachtwurz' Revier. Die alten Versorgungswege führen weiter zur Nebelwacht und zu Vharoks versiegeltem Grenztor. Was wird dort bewacht? Deine Reise verbindet das friedliche Dorf mit dieser Grenze."
const STEPS = [
	{
		"id": "heard", 
		"title": "Eine Nachricht vom Waldweg", 
		"goal": "PARK", 
		"text": "Lies Fennecs Botenbrief am Wegstein im Park oder frage einen anwesenden Bewohner nach dem Waldweg."
	}, 
	{
		"id": "woodland", 
		"title": "Den Wald kennenlernen", 
		"goal": "GREENWOOD", 
		"text": "Vom Park über die Wiese zum Waldrand. Nimm den Durchgang zum Waldpfad und erkunde den grünen Wald."
	}, 
	{
		"id": "clue_leaf", 
		"title": "Das eingerissene Botenband", 
		"goal": "GREENWOOD", 
		"text": "Untersuche das helle Band am Weg durch den grünen Wald."
	}, 
	{
		"id": "clue_seal", 
		"title": "Drei Ringe im Siegelrest", 
		"goal": "SHADOW_GATE", 
		"text": "Am Tor zum Schattenwald liegt ein Siegelrest. Vergleiche sein Zeichen mit dem Staub am Botenband."
	}, 
	{
		"id": "nightroot", 
		"title": "Nachtwurz auf der Lichtung", 
		"goal": "SHADOW_CLEARING", 
		"text": "Folge den letzten Fackeln zur dunklen Lichtung. In der Dorfwerkstatt kannst du dein Deck vorher testen."
	}, 
	{
		"id": "beacon", 
		"title": "Das Weglicht kehrt zurück", 
		"goal": "FOREST_ENTRANCE", 
		"text": "Kehre nach dem Sieg an den Waldsaum zurück und entzünde das Weglicht. Sein Schein bleibt sichtbar."
	}, 
	{
		"id": "border", 
		"title": "Der alte Grenzweg", 
		"goal": "SEAL_COURT", 
		"text": "Über die Nebelwacht und den Aschenstieg zum Siegelhof. Die dortigen Wesen versorgen und bewachen die Grenze."
	}, 
	{
		"id": "warden", 
		"title": "Vharok, Hüter des Grenztors", 
		"goal": "SEAL_COURT", 
		"text": "Überwinde Vharoks Schutzsiegel. Mit den beiden Waldspuren lässt sich das alte Grenztor öffnen."
	}, 
	{
		"id": "relay_guard", 
		"title": "Der Motor im Relaisgarten", 
		"goal": "RELAY_COURT", 
		"text": "Hinter dem Tor wartet ein alter Relaiswächter. Seine Drohnen verstärken den angekündigten Impuls: Schalte sie rechtzeitig aus."
	}, 
	{
		"id": "relay", 
		"title": "Die Weglichter antworten", 
		"goal": "RELAY_COURT", 
		"text": "Stimme nach dem Kampf die drei Relaisringe mit dem Siegelrest ab und berichte den Bewohnern von deiner Entdeckung."
	}
]

static func progress(save: Dictionary, species: String) -> Array[Dictionary]:
	var visited: Array = save.get("discovered", [])
	var events: Dictionary = save.get("story_events", {}).get(species, {})
	var result: Array[Dictionary] = []
	for entry in STEPS:
		var step: Dictionary = entry.duplicate()
		match str(step.id):
			"woodland": step.done = "GREENWOOD" in visited
			"nightroot": step.done = won(save, species, "")
			"border": step.done = "SEAL_COURT" in visited
			"warden": step.done = won(save, species, "vharok")
			"relay_guard": step.done = won(save, species, "relay_warden")
			_: step.done = events.get(step.id, false)
		if step.id == "beacon":
			step.done = beacon_restored(save, species)
			if step.done: step.text = "Das Weglicht brennt. Eine kleine Weglaterne auf dem Teetisch in der Stube erinnert an deine Rückkehr."
		if step.done and step.id == "clue_leaf": step.text = "Am Botenband haftet der helle Staub der alten Weglichter."
		if step.done and step.id == "clue_seal": step.text = "Drei ineinanderliegende Ringe: Der Siegelrest stammt von der alten Grenzwacht."
		result.append(step)
	return result

static func beacon_restored(save: Dictionary, species: String) -> bool:
	return bool(save.get("story_events", {}).get(species, {}).get("beacon", false))

static func remember_victory(save: Dictionary, species: String, enemy_id: String, claimed: bool) -> void :
	if not claimed or enemy_id not in ["", "vharok", "relay_warden"]: return
	if not save.has("journey_wins"): save.journey_wins = {}
	if not save.journey_wins.has(species): save.journey_wins[species] = {}
	save.journey_wins[species]["nightroot" if enemy_id.is_empty() else enemy_id] = true

static func won(save: Dictionary, species: String, enemy_id: String) -> bool:
	var id: = "nightroot" if enemy_id.is_empty() else enemy_id
	if save.get("journey_wins", {}).get(species, {}).get(id, false): return true
	var key: = species if enemy_id.is_empty() else species + ":" + enemy_id


	return bool(save.get("forest_encounters", {}).get(key, {}).get("rewards_claimed", false))

static func next_step(save: Dictionary, species: String) -> Dictionary:
	for step in progress(save, species):
		if not step.done: return step
	return {}

static func guidance(save: Dictionary, species: String, place: String) -> String:
	var step: = next_step(save, species)
	if step.is_empty(): return "Die bisherige Grenzreise ist abgeschlossen. Dorf, Rundwege und Decktests bleiben frei zugänglich."
	var route: = Routes.path_between(place, step.goal)
	if route.size() > 1: return "Nächster Weg: " + Routes.label(route[0]) + " → " + Routes.label(route[1]) + ". Ziel: " + Routes.label(step.goal) + "."
	if route.size() == 1: return "Du bist am Zielort. " + str(step.text)
	return str(step.text)
