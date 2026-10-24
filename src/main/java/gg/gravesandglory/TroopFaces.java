package gg.gravesandglory;

import java.util.Set;
import java.util.UUID;

/**
 * Which face a troop wears (texel-3 faces design F2): a troop model with face variants has one face bone a variant under its
 * head (tools/build_skins.py split_faces), and a draw shows the one bone of the variant this class picks. Pure, client side and
 * Minecraft-free (TroopFacesCheck compiles it alone): the entity UUID reaches every client in its spawn packet, so all players see the
 * same face with nothing in the save or on the wire.
 */
public final class TroopFaces {
    private TroopFaces() {}

    /**
     * The face of a UUID among count faces: a splitmix64 finaliser over the two words, floor-modded. Both words matter (the
     * second is mixed first), so UUIDs that differ in one bit land anywhere. A count of 1 or less is face 0. Fixed for ever: the
     * golden table of TroopFacesCheck pins it, since changing it reshuffles every soldier's face.
     */
    public static int variant(UUID id, int count) {
        if (count <= 1) return 0;
        long z = mix(id.getMostSignificantBits() ^ mix(id.getLeastSignificantBits()));
        return (int) Math.floorMod(z, (long) count);
    }

    private static long mix(long z) {
        z += 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /**
     * The face a draw wears. forced is the harness's override (negative for none; production never sets one) and beats everything;
     * a card or reveal portrait (portrait true: a client-built mob with a random UUID) wears the model's card face, so a card is the
     * same every time it is built; a pinned troop (fixed 0 or more: a singular character) keeps that face whatever its UUID; every
     * other troop wears the face its UUID hashes to. A troop with one face or none wears face 0.
     */
    public static int pick(UUID id, int count, int fixed, int card, boolean portrait, int forced) {
        if (count <= 1) return 0;
        if (forced >= 0) return Math.floorMod(forced, count);
        if (portrait) return Math.floorMod(card, count);
        if (fixed >= 0) return Math.floorMod(fixed, count);
        return variant(id, count);
    }

    /**
     * Whether a bone with these variants shows when the draw wears this face: a bone with none always shows; otherwise it shows for
     * the variants it names, the draw's variant taken floor-modded into [0, count) first, so exactly one of a troop's face bones shows
     * whatever number comes in.
     */
    public static boolean shows(Set<Integer> variants, int variant, int count) {
        return variants.isEmpty() || variants.contains(count <= 0 ? 0 : Math.floorMod(variant, count));
    }
}
