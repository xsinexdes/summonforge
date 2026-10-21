package com.summonforge.ability;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/** owner = jugador dueño (null si lanza un boss); scale = multiplicador por nivel. */
public record CastContext(LivingEntity caster, Player owner, Location origin, Vector direction, double scale) {}
