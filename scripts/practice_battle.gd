extends "res://scripts/card_battle.gd"


func claim_victory() -> bool:
	if phase != "victory" or playback_pending or rewards_claimed: return false
	result = {"members": [], "items": []};rewards_claimed = true
	message = "Training geschafft · Dein Deck bleibt unverändert."
	return true
