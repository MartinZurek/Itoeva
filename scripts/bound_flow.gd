extends RefCounted

var active: = false
var phase: = 0.0
var count: = 0
var stride: = 2.8
var height: = 0.32
var glide: = false
var bank: = 0.0
var heading: = Vector2.ZERO
var clearance: = 1.0
var launch_distance: = 2.8
var launch_turn: = 0.0
var plant_side: = 1.0
var wing_height: = 0.0
var wing_velocity: = 0.0
var pose: Dictionary = {}
var low_stride: = false
func reset() -> void :
	active = false;phase = 0.0;bank = 0.0;heading = Vector2.ZERO;wing_height = 0;wing_velocity = 0;pose.clear()
func _choose(profile: Dictionary) -> void :
	var wings: bool = profile.get("wings", false)
	var i: int = count % profile.flow_strides.size()
	if wings:
		stride = float(profile.flow_strides[i]) * lerpf(0.9, 1.0, clearance)
		height = float(profile.flow_heights[i]) * lerpf(0.7, 1.0, clearance) * lerpf(1.0, 0.9, launch_turn)
	else:


		low_stride = count % 7 in [2, 5] or launch_turn > 0.22 or clearance < 0.65
		stride = clampf(launch_distance * float(profile.flow_strides[i]) / 2.8 * lerpf(0.93, 1.0, clearance), 2.5, 3.15)
		height = (0.055 if low_stride else float(profile.flow_heights[i])) * lerpf(0.8, 1.0, clearance)
	glide = false
	for index in profile.get("glides", []):
		if int(index) == i: glide = true
func advance(delta: float, wants: bool, moving: bool, speed: float, direction: Vector2, profile: Dictionary) -> Dictionary:
	var dt: = maxf(0.0, delta)
	var wings: bool = profile.get("wings", false)
	if not active:
		if not wants: return pose
		active = true;phase = 0;_choose(profile)
	if direction.length_squared() > 0.001:
		var next: = direction.normalized()
		var angular: = atan2(heading.cross(next), heading.dot(next)) / maxf(dt, 0.001) if heading.length_squared() > 0.001 else 0.0
		bank = lerpf(bank, clampf(angular * 0.018, - 0.1, 0.1), 1.0 - exp( - dt * 9.0));heading = next
	else: bank = lerpf(bank, 0.0, 1.0 - exp( - dt * 9.0))
	phase += dt * (maxf(speed, 0.0) / maxf(0.1, stride) if moving and wants else 2.6)
	if wings and not wants: phase = minf(phase, 1.0 - 1e-06)
	if phase >= 1.0 and wants and not moving: phase = 1.0 - 1e-06
	elif phase >= 1.0:
		phase = fposmod(phase, 1.0);count += 1
		if not wants and ( not wings or wing_height < 0.015): reset();return pose
		_choose(profile)
	var contact: float = profile.get("contact", 0.18)
	if not wings and low_stride: contact = 0.42
	var air: = clampf((phase - contact) / (1.0 - contact), 0.0, 1.0)
	var flying: = phase > contact
	var arc: = 4.0 * air * (1.0 - air) if flying else 0.0
	var power: = 0.0
	var wing: = 0.0; var thrust: = 1.0; var pivot: = 0.0; var compression: = 0.0
	var pitch: = 0.0; var lift: = height * arc
	if wings:


		power = pow(sin(PI * clampf((phase - 0.22) / 0.24, 0.0, 1.0)), 2)
		if phase < 0.22: wing = 0.82 * smoothstep(0.0, 0.22, phase)
		elif phase < 0.46: wing = lerpf(0.82, - 0.56, smoothstep(0.22, 0.46, phase))
		else: wing = lerpf( - 0.56, 0.28, smoothstep(0.46, 0.74, phase))
		if glide and phase > 0.64: wing -= 0.24 * pow(sin(PI * (phase - 0.64) / 0.36), 2)
		var loft: = height * (0.8 + 0.18 * sin(TAU * (phase - 0.3)))
		if count == 0 or count % 3 == 0: loft *= smoothstep(0.2, 0.4, phase)
		if count % 3 == 2: loft *= 1.0 - smoothstep(0.72, 0.95, phase)
		if not wants: loft = 0;wing = 0

		var omega: = 15.0; var x: = wing_height - loft; var step: = (wing_velocity + omega * x) * dt; var decay: = exp( - omega * dt)
		wing_height = maxf(0.0, loft + (x + step) * decay);wing_velocity = (wing_velocity - omega * step) * decay
		lift = wing_height;flying = lift > 0.025
		thrust = 1.0 + 0.14 * (power - 0.12) if wants else 1.0
		pitch = 0.018 * power + bank
		if not wants and wing_height < 0.008: reset();return pose
		air = clampf(lift / maxf(height, 0.01), 0, 1)
	else:
		if not flying:
			var load: = phase / contact
			compression = sin(PI * load) * (0.4 if low_stride else 1.0)
			thrust = 1.0 - (0.05 if low_stride else 0.12) * sin(TAU * load)
			if load < 0.5: pivot = 0.38;pitch = - 0.075 * sin(TAU * load)
			else: pivot = - 0.4;pitch = 0.09 * sin(TAU * (load - 0.5))
		else: pitch = 0.055 * (1.0 - 2.0 * air) * arc
		pitch += bank
	var sequence: Array = profile.flow_frames
	var frame_index: = 0
	if not flying: frame_index = 0 if phase < contact * 0.5 else 1
	elif air < 0.18: frame_index = 2
	elif air < 0.6: frame_index = 3
	elif air < 0.82: frame_index = 4
	else: frame_index = 5
	if wings and flying: frame_index = 3 if power > 0.25 or glide else 4
	var drawing: int = sequence[frame_index]
	if not wings and low_stride: drawing = int(profile.get("low_frames", sequence)[frame_index])
	else:
		if frame_index == 3 and profile.has("air_poses"): drawing = int(profile.air_poses[count % profile.air_poses.size()])
		if frame_index == 4 and profile.has("reach_poses"): drawing = int(profile.reach_poses[count % profile.reach_poses.size()])
	pose.phase = phase;pose.air_phase = air;pose.airborne = flying;pose.lift = lift;pose.pitch = pitch
	pose.wing = wing;pose.compression = compression;pose.frame = drawing;pose.glide = glide
	pose.count = count;pose.stride = stride;pose.thrust = thrust;pose.pivot = pivot
	pose.low_stride = not wings and low_stride
	return pose
