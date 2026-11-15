extends "res://scripts/card_view.gd"

const PreviewReveal = preload("res://scripts/summoning_reveal.gd")
var reveal: Control
var skip_reveal: Button
var pause_reveal: Button
var preview_status: Label

func _build() -> void:
	super._build()
	var card := Cards.get_card(encounter.subject)
	var rarity: String = card.get("collect_rarity", card.get("rarity", "Spezialkarte"))
	_label("%s · %s" % [card.name, rarity], Rect2(38, 531, 680, 34), 23, GOLD)
	preview_status = _label("", Rect2(730, 530, 500, 30), 17, Color("b7e5e9"))
	_button("← Vorige Karte", Rect2(40, 604, 230, 62), encounter.app.navigate_preview.bind(-1))
	_button("Wiederholen", Rect2(286, 604, 230, 62), encounter.app.navigate_preview.bind(0))
	_button("Nächste Karte →", Rect2(532, 604, 230, 62), encounter.app.navigate_preview.bind(1))
	_button("Zur Kartenliste", Rect2(780, 604, 228, 62), encounter.app.show_menu)

func build_commands() -> void:
	pass

func refresh() -> void:
	super.refresh()
	for label in [hand_label, queue_label, crystal_label, enemy_turn_hint]:
		label.hide()
	if is_instance_valid(preview_status):
		preview_status.text = "Einspieler läuft" if model.playback_pending else "Effekt angewendet"

func play_event(elapsed: float = 0.0) -> void:
	super.play_event(elapsed)
	if not event.has("preview_card"):
		return
	if not is_instance_valid(reveal):
		reveal = PreviewReveal.new()
		reveal.card = Cards.get_card(event.preview_card)
		reveal.z_index = 8
		layout.add_child(reveal)
		skip_reveal = _button("Zur Wirkung", Rect2(1030, 637, 207, 54), _skip_reveal)
		skip_reveal.z_index = 9
		pause_reveal = _button("Pause", Rect2(1100, 15, 137, 53), encounter.app.show_battle_pause)
		pause_reveal.z_index = 9
	_update_reveal(elapsed)

func _skip_reveal() -> void:
	event_time = float(event.preview_cut)
	encounter.busy = model.animation_duration() - event_time
	encounter.app.battle_audio.event_clock = event_time
	_update_reveal(event_time)

func _update_reveal(elapsed: float) -> void:
	var visible_now: bool = model.playback_pending and elapsed < float(event.preview_cut)
	reveal.visible = visible_now
	skip_reveal.visible = visible_now
	pause_reveal.visible = visible_now
	if visible_now:
		reveal.progress = clampf(elapsed / float(event.preview_cut), 0.0, 1.0)
		reveal.queue_redraw()

func _process(delta: float) -> void:
	super._process(delta)
	if is_instance_valid(reveal) and not encounter.suspended:
		_update_reveal(event_time)
