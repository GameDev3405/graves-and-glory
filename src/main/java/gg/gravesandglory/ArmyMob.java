package gg.gravesandglory;

/** Marks the entities that fight as troops: the Army* subclasses of the mobs the troops sheet names. ArmyMobs.isTroop reads it, and the load filter keeps these and discards every other entity that carries a battle tag. */
public interface ArmyMob {}
