package gg.gravesandglory;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.DyeableItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Locale;

/**
 * Draws the drop board's coins (design 2026-11-14, sections 5 and 6): the coin on the rail and in the pegs, the sparks and rings of its
 * hits, the slot's flare, the troop or piece that rises, and the cinematic's screen layer. Everything is placed from DropClient's
 * continuous clock each frame.
 */
final class DropRender {
    private DropRender() {}

    /** The coin's model (the drop_coin prop's part) and its size on the board. */
    static int COIN_CMD = 0; static float COIN_SCALE = 1f;
    /** How far the coin's centre stands out of the back plate's plane. */
    static final double COIN_FORWARD = 0.375;

    enum Sound { FLY, RELEASE, PEG, WALL, LAND, FLARE, JACKPOT, REVEAL }

    /** The sounds are the game's own (amethyst and copper suit the giants' crystal and bronze); the audio lead may swap cues in later. */
    static void sound(MinecraftClient c, Sound s, float volume, float pitch) { sound(c, s, volume, pitch, DropLayout.world(4.5, 5, 0)); }
    static void sound(MinecraftClient c, Sound s, float volume, float pitch, double[] at) {
        if (c.world == null) return;
        SoundEvent e = switch (s) {
            case FLY -> SoundEvents.BLOCK_BEACON_AMBIENT;
            case RELEASE -> SoundEvents.BLOCK_AMETHYST_BLOCK_BREAK;
            case PEG -> SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME;
            case WALL -> SoundEvents.BLOCK_COPPER_HIT;
            case LAND -> SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK;
            case FLARE -> SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE;
            case JACKPOT -> SoundEvents.UI_TOAST_CHALLENGE_COMPLETE;
            case REVEAL -> SoundEvents.BLOCK_BEACON_POWER_SELECT;
        };
        c.world.playSound(at[0], at[1], at[2], e, SoundCategory.PLAYERS, volume, pitch, false);
    }

    static int colour(String hex) { try { return Integer.parseInt(hex.replace("#", ""), 16); } catch (RuntimeException e) { return 0xffffff; } }
    static int slotColour(int slot) { return slot < 0 ? 0xffffff : colour(Spec.DROPSLOTS[slot].colour()); }
    /** The colour of the result's rarity (the gear rarities' border colours; a relic is the glimpur violet). */
    static int rarityColour(DropClient.View v) {
        String r = rarity(v);
        for (var row : Spec.GEARRARITIES) if (row.id().equals(r)) return colour(row.border());
        return 0xc9a8ff;
    }
    static String rarity(DropClient.View v) {
        if (v.card.has("troop")) { var t = EndingClient.troop(v.card.get("troop").getAsString()); return t == null ? "common" : t.rarity(); }
        return v.card.has("rarity") ? v.card.get("rarity").getAsString() : "";
    }
    /** The result's name, its rarity word and its third line. */
    static String[] words(DropClient.View v) {
        var c = v.card;
        if (c.has("troop")) {
            var t = EndingClient.troop(c.get("troop").getAsString());
            String r = t == null ? "" : t.rarity().substring(0, 1).toUpperCase(Locale.ROOT) + t.rarity().substring(1);
            boolean dupe = c.has("duplicate") && c.get("duplicate").getAsBoolean();
            int before = c.has("starsBefore") ? c.get("starsBefore").getAsInt() : 0, after = c.has("starsAfter") ? c.get("starsAfter").getAsInt() : 0, shards = c.has("shards") ? c.get("shards").getAsInt() : 0;
            String line = !dupe ? "Joins your collection" : shards > 0 ? "Full stars: +" + shards + " shards" : after > before ? "Rises to star " + after : "A copy toward the next star";
            return new String[]{t == null ? "" : t.name(), r, line};
        }
        String relic = c.has("relic") ? c.get("relic").getAsString() : "";
        boolean dupe = c.has("duplicate") && c.get("duplicate").getAsBoolean();
        if (!relic.isEmpty()) { var r = Gear.relic(relic); return new String[]{r == null ? relic : r.name(), "Relic", dupe ? "Already owned: +" + (c.has("refund") ? c.get("refund").getAsInt() : 0) + " gear shards" : "A new relic"}; }
        var it = c.has("item") ? Gear.item(c.get("item").getAsString()) : null;
        var r = Gear.rarity(rarity(v));
        return new String[]{it == null ? "" : it.name(), r == null ? "" : r.name(), dupe ? "Duplicate" : ""};
    }

    // ---- what rises ------------------------------------------------------------------------------------------------------------------

    private static void buildRiser(MinecraftClient c, DropClient.View v) {
        v.riserBuilt = true;
        try {
            if (v.card.has("troop")) {
                var spec = EndingClient.troop(v.card.get("troop").getAsString());
                if (spec == null) return;
                MobEntity m = ArmyMobs.portrait(spec.entity(), c.world);
                m.setCustomNameVisible(false); m.setSilent(true);
                RankSync.setLocal(m, new RankSync.Rank(1, 1, 1));
                m.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Registries.ITEM.get(new Identifier(spec.weapon()))));
                m.equipStack(EquipmentSlot.HEAD, new ItemStack(Registries.ITEM.get(new Identifier(spec.helmet()))));
                var chest = new ItemStack(Registries.ITEM.get(new Identifier(spec.chest())));
                if (chest.getItem() instanceof DyeableItem dye) dye.setColor(chest, 0xb0a080);
                m.equipStack(EquipmentSlot.CHEST, chest);
                if (ArmyMobs.tags(spec).contains("shield")) m.equipStack(EquipmentSlot.OFFHAND, GravesAndGlory.Battle.shield(spec, GravesAndGlory.Battle.PORTRAIT_FIELD));
                TroopModels.dress(m, "linen");
                m.setId(-7300 - Math.floorMod(v.id, 64));
                v.riser = m;
            } else if (v.card.has("item") && !v.card.get("item").getAsString().isEmpty()) {
                ItemStack s = GearLooks.stack(new Gear.Piece(0, v.card.get("item").getAsString(), rarity(v), 50, Gear.forge().maxDurability()));
                v.riser = s.isEmpty() ? new ItemStack(Items.NETHER_STAR) : s;
            } else v.riser = new ItemStack(Items.NETHER_STAR);
        } catch (RuntimeException e) {
            System.err.println("[Graves and Glory] drop board: the result could not be built: " + e);
        }
    }

    static void forget(DropClient.View v) {
        if (v.riser instanceof LivingEntity e) { TroopMotion.forget(e.getId()); MotionEvents.Client.forget(e.getId()); }
        v.riser = null;
    }

    static void tick(MinecraftClient c) {
        for (var v : DropClient.views.values()) if (v.riser instanceof LivingEntity e) e.age++;
    }

    // ---- the world layer -------------------------------------------------------------------------------------------------------------

    private static double lastFrame;

    static void world(WorldRenderContext ctx) {
        var c = MinecraftClient.getInstance();
        if (DropClient.views.isEmpty() || c.world == null) return;
        double now = DropClient.now(), dt = Math.max(0, Math.min(0.1, now - lastFrame));
        lastFrame = now;
        var cam = ctx.camera().getPos();
        MatrixStack m = ctx.matrixStack();
        var consumers = c.getBufferBuilders().getEntityVertexConsumers();
        var b = Spec.DROPBOARD[0];
        double flare = b.flareTenths() / 10.0, reveal = b.revealTenths() / 10.0;
        for (var v : DropClient.views.values()) {
            var phase = v.phase(now);
            if (phase == DropClient.Phase.DONE) continue;
            // the coin on the rail eases after the mouse (or the watcher's last word of it)
            if (v.path == null && !v.released) { double[] vel = {v.xVel}; v.x = DropClient.damp(v.x, v.targetX, vel, 0, v.mine ? 0.07 : 0.18, dt); v.xVel = vel[0]; }
            double[] q = v.coin(now);
            if (v.lastCoin != null && v.path != null && phase == DropClient.Phase.DROP) v.maxStep = Math.max(v.maxStep, Math.hypot(q[0] - v.lastCoin[0], q[1] - v.lastCoin[1]));
            v.lastCoin = q;
            double since = v.sinceLanding(now);
            int tint = slotColour(v.slot());

            // hits passed since the last frame: a ring, a spark
            if (v.path != null) {
                int sample = (int) ((now - v.pathAt) / DropSim.SAMPLE);
                var hits = v.path.hits();
                int rung = 0;
                while (v.nextHit < hits.size() && hits.get(v.nextHit).sample() <= sample) {
                    var h = hits.get(v.nextHit++);
                    if (rung++ > 3) continue;
                    float speed = Math.min(1f, h.speed() / 9f);
                    double[] at = DropLayout.world(q[0], q[1], COIN_FORWARD);
                    if (h.what() >= 0) {
                        sound(c, Sound.PEG, 0.25f + 0.6f * speed, 0.8f + 0.8f * speed + (h.what() % 5) * 0.03f, at);
                        double[] peg = DropLayout.world(DropSim.pegX(h.what() / 16, h.what() % 16), DropSim.FIRST_ROW + h.what() / 16, COIN_FORWARD + 0.1);
                        for (int i = 0; i < 2 + (int) (speed * 4); i++) c.world.addParticle(ParticleTypes.ELECTRIC_SPARK, peg[0], peg[1], peg[2], (Math.random() - .5) * .25, (Math.random() - .3) * .25, (Math.random() - .5) * .25);
                    } else if (h.what() == DropSim.FLOOR_HIT) {
                        if (!v.landed) { v.landed = true; sound(c, Sound.LAND, 0.9f, 0.9f, at); }
                    } else sound(c, Sound.WALL, 0.2f + 0.5f * speed, 0.9f + 0.5f * speed, at);
                }
                // the trail
                if (phase == DropClient.Phase.DROP && Math.random() < dt * 45) {
                    double[] at = DropLayout.world(q[0], q[1], COIN_FORWARD - 0.1);
                    c.world.addParticle(new DustParticleEffect(new Vector3f(0.82f, 0.72f, 1f), 0.6f), at[0], at[1], at[2], 0, 0, 0);
                }
            }

            // the flare: once, as the coin comes to rest
            if (since >= 0 && !v.flared) {
                v.flared = true;
                int mult = DropMath.multiplier(v.slot());
                sound(c, Sound.FLARE, 1f, mult >= 10 ? 0.7f : mult >= 5 ? 0.85f : mult >= 3 ? 1.0f : mult >= 2 ? 1.15f : 1.3f);
                if (mult >= 10) sound(c, Sound.JACKPOT, 1f, 1f);
                if (v.mine) DropClient.shake(mult >= 10 ? 1.0 : mult >= 5 ? 0.6 : mult >= 3 ? 0.25 : 0.1);
                float[] rgb = rgb(tint);
                double[] at = DropLayout.world(v.slot() + 0.5, 10.6, COIN_FORWARD);
                for (int i = 0; i < 14 + mult * 5; i++) c.world.addParticle(new DustParticleEffect(new Vector3f(rgb[0], rgb[1], rgb[2]), 1.1f), at[0] + (Math.random() - .5) * .8, at[1] + Math.random() * 1.2, at[2] + (Math.random() - .5) * .8, 0, 0.1, 0);
            }
            if (since >= flare && !v.revealed) {
                v.revealed = true;
                if (!v.riserBuilt) buildRiser(c, v);
                String r = rarity(v);
                sound(c, Sound.REVEAL, 0.9f, r.equals("legendary") ? 0.6f : r.equals("epic") ? 0.8f : r.equals("rare") ? 1.0f : 1.25f);
            }

            m.push();
            m.translate(-cam.x, -cam.y, -cam.z);
            // the beam and the slot's glow
            if (since >= 0) {
                VertexConsumer vc = consumers.getBuffer(RenderLayer.getLightning());
                Matrix4f mat = m.peek().getPositionMatrix();
                float rise = (float) Math.min(1, since / 0.25), fade = (float) Math.max(0, 1 - Math.max(0, since - flare - reveal * 0.6) / (reveal * 0.4));
                float a = rise * fade;
                int mult = DropMath.multiplier(v.slot());
                double height = 4 + mult * 0.7, sx = v.slot() + 0.5;
                beam(vc, mat, sx, 0.42, 11 - height * rise, 11, COIN_FORWARD + 0.25, tint, 0.55f * a);
                beam(vc, mat, sx, 0.2, 11 - height * rise * 1.15, 11, COIN_FORWARD + 0.3, 0xffffff, 0.5f * a);
                beam(vc, mat, sx, 0.5, 9.6, 11, 0.06, tint, 0.6f * a * (0.75f + 0.25f * (float) Math.sin(now * 9)));
                if (mult >= 10) for (int k = 0; k < 9; k++) { // every rune answers, outward from the slot
                    float wave = (float) Math.max(0, 1 - Math.abs(since * 6 - Math.abs(k - v.slot())) / 1.5);
                    if (k != v.slot() && wave > 0) beam(vc, mat, k + 0.5, 0.45, 8.2, 11, COIN_FORWARD + 0.2, slotColour(k), 0.5f * wave * fade);
                }
            }
            // the coin: it sinks away as the result rises
            float coinScale = since < flare ? 1f : (float) Math.max(0, 1 - (since - flare) / 0.35);
            if (coinScale > 0.01f) {
                double[] at = DropLayout.world(q[0], q[1], COIN_FORWARD);
                m.push();
                m.translate(at[0], at[1], at[2]);
                m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180 - DropLayout.viewYaw()));
                double roll = v.path == null ? (v.x - 4.5) / DropSim.COIN : v.roll[Math.min(v.roll.length - 1, Math.max(0, (int) ((now - v.pathAt) / DropSim.SAMPLE)))];
                m.multiply(RotationAxis.POSITIVE_Z.rotation((float) roll));
                m.scale(COIN_SCALE * coinScale, COIN_SCALE * coinScale, COIN_SCALE * coinScale);
                ItemStack coin = new ItemStack(Items.PAPER);
                if (COIN_CMD != 0) coin.getOrCreateNbt().putInt("CustomModelData", COIN_CMD);
                c.getItemRenderer().renderItem(coin, ModelTransformationMode.FIXED, LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, m, consumers, c.world, v.id);
                m.pop();
            }
            // the roller's name over a coin that is not this client's own
            if (!v.mine && since < flare) {
                double[] at = DropLayout.world(q[0], Math.min(q[1], 0.4) - 0.75, COIN_FORWARD + 0.1);
                m.push();
                m.translate(at[0], at[1], at[2]);
                m.multiply(ctx.camera().getRotation());
                m.scale(-0.03f, -0.03f, 0.03f);
                var font = c.textRenderer;
                font.draw(Text.literal(v.name), -font.getWidth(v.name) / 2f, 0, 0xffeee9df, false, m.peek().getPositionMatrix(), consumers, net.minecraft.client.font.TextRenderer.TextLayerType.NORMAL, 0x60000000, LightmapTextureManager.MAX_LIGHT_COORDINATE);
                m.pop();
            }
            // what rises
            if (since >= flare && v.riser != null) {
                double t = since - flare, up = smooth(t / 0.9), sx = v.slot() + 0.5;
                boolean troop = v.riser instanceof LivingEntity;
                double[] at = DropLayout.world(sx, (troop ? 10.9 : 10.3) - up * (troop ? 1.3 : 1.6), 0.45 + up * 0.9);
                float yaw = (float) (180 - DropLayout.viewYaw() + 20 * Math.sin(t * 1.3) + (1 - up) * 160);
                float size = (float) (0.35 + 0.65 * up);
                int halo = rarityColour(v);
                VertexConsumer vc = consumers.getBuffer(RenderLayer.getLightning());
                beam(vc, m.peek().getPositionMatrix(), sx, 0.9 * up, (troop ? 9.9 : 8.7) - 1.3 * up, (troop ? 9.9 : 8.7) + 1.3 * up, 0.35, halo, 0.28f * (float) up);
                if (Math.random() < dt * 30) { float[] rgb = rgb(halo); c.world.addParticle(new DustParticleEffect(new Vector3f(rgb[0], rgb[1], rgb[2]), 0.8f), at[0] + (Math.random() - .5) * 1.2, at[1] + Math.random() * 1.6, at[2] + (Math.random() - .5) * 1.2, 0, 0.03, 0); }
                if (v.riser instanceof LivingEntity e) {
                    place(e, at[0], at[1], at[2], yaw + 180);
                    boolean hud = c.options.hudHidden; c.options.hudHidden = true;
                    try {
                        m.push();
                        m.translate(at[0], at[1], at[2]); m.scale(size, size, size); m.translate(-at[0], -at[1], -at[2]);
                        c.getEntityRenderDispatcher().render(e, at[0], at[1], at[2], e.getYaw(), 1f, m, consumers, LightmapTextureManager.MAX_LIGHT_COORDINATE);
                        m.pop();
                    } catch (RuntimeException ex) { System.err.println("[Graves and Glory] drop board: the troop could not be drawn: " + ex); v.riser = null; }
                    finally { c.options.hudHidden = hud; }
                } else if (v.riser instanceof ItemStack s) {
                    m.push();
                    m.translate(at[0], at[1], at[2]);
                    m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw));
                    m.scale(1.5f * size, 1.5f * size, 1.5f * size);
                    c.getItemRenderer().renderItem(s, ModelTransformationMode.FIXED, LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, m, consumers, c.world, v.id);
                    m.pop();
                }
            }
            m.pop();
        }
        consumers.draw();
    }

    private static double smooth(double t) { t = Math.max(0, Math.min(1, t)); return t * t * (3 - 2 * t); }
    private static float[] rgb(int c) { return new float[]{(c >> 16 & 255) / 255f, (c >> 8 & 255) / 255f, (c & 255) / 255f}; }

    /** A glowing upright strip in the board's frame: centred on bx, half as wide as `half`, from board y `top` down to `bottom`, bright at the bottom and fading out at the top; both faces. */
    private static void beam(VertexConsumer vc, Matrix4f mat, double bx, double half, double top, double bottom, double forward, int colour, float alpha) {
        if (alpha <= 0.003f || half <= 0) return;
        float[] c = rgb(colour);
        double[] a = DropLayout.world(bx - half, bottom, forward), b = DropLayout.world(bx + half, bottom, forward), d = DropLayout.world(bx + half, top, forward), e = DropLayout.world(bx - half, top, forward);
        double[][] quad = {a, b, d, e};
        float[] alphas = {alpha, alpha, 0, 0};
        for (int i = 0; i < 4; i++) vc.vertex(mat, (float) quad[i][0], (float) quad[i][1], (float) quad[i][2]).color(c[0], c[1], c[2], alphas[i]).next();
        for (int i = 3; i >= 0; i--) vc.vertex(mat, (float) quad[i][0], (float) quad[i][1], (float) quad[i][2]).color(c[0], c[1], c[2], alphas[i]).next();
    }

    private static void place(LivingEntity e, double x, double y, double z, float yaw) {
        e.refreshPositionAndAngles(x, y, z, yaw, 0);
        e.prevX = e.lastRenderX = x; e.prevY = e.lastRenderY = y; e.prevZ = e.lastRenderZ = z;
        e.bodyYaw = e.prevBodyYaw = e.headYaw = e.prevHeadYaw = yaw; e.prevYaw = yaw; e.prevPitch = 0;
    }

    // ---- the screen layer (DropScreen draws it) --------------------------------------------------------------------------------------

    static void screen(DrawContext g, int w, int h) {
        var v = DropClient.mine;
        if (v == null) return;
        var c = MinecraftClient.getInstance();
        var font = c.textRenderer;
        var b = Spec.DROPBOARD[0];
        double now = DropClient.now(), t = now - v.began, fly = b.flyTenths() / 10.0;
        var phase = v.phase(now);
        double in = smooth(t / 0.6), out = phase == DropClient.Phase.RETURN ? 1 - smooth((v.sinceLanding(now) - (b.flareTenths() + b.revealTenths()) / 10.0) / (b.returnTenths() / 10.0)) : 1;
        int bar = (int) (Math.max(18, h / 10) * in * out);
        g.fill(0, 0, w, bar, DarkUi.LETTERBOX); g.fill(0, h - bar, w, h, DarkUi.LETTERBOX);
        if (phase == DropClient.Phase.AIM || phase == DropClient.Phase.FLY && t > 0.5) {
            g.drawCenteredTextWithShadow(font, "Move to aim   ·   Click or Space to drop", w / 2, h - bar - 26, DarkUi.IVORY);
            double left = Math.max(0, Math.min(1, 1 - (t - fly) / b.aimSeconds()));
            int bw = 160, x0 = w / 2 - bw / 2, y0 = h - bar - 12;
            g.fill(x0 - 1, y0 - 1, x0 + bw + 1, y0 + 4, 0xa0000000);
            g.fill(x0, y0, x0 + (int) (bw * left), y0 + 3, left < 0.25 ? 0xffe0704a : DarkUi.BRASS);
        } else if (phase == DropClient.Phase.DROP || phase == DropClient.Phase.WAIT) {
            g.drawCenteredTextWithShadow(font, HintText.expand("Click or {key:menu} to skip", GravesAndGloryClient::hint), w / 2, h - bar + 6, DarkUi.MUTED);
        }
        double since = v.sinceLanding(now), flare = b.flareTenths() / 10.0;
        if (since >= 0 && v.slot() >= 0) {
            // the multiplier, struck large as the coin lands
            double pop = smooth(since / 0.18), away = since < flare ? 1 : Math.max(0, 1 - (since - flare) / 0.4);
            if (away > 0.02) {
                String text = "x" + DropMath.multiplier(v.slot());
                float scale = (float) ((3 + 2.2 * (1 - pop)) * (h / 360.0));
                int alpha = (int) (255 * pop * away);
                g.getMatrices().push();
                g.getMatrices().translate(w / 2f, h * 0.24f, 0); g.getMatrices().scale(scale, scale, 1);
                g.drawText(font, text, -font.getWidth(text) / 2, -4, alpha << 24 | slotColour(v.slot()), true);
                g.getMatrices().pop();
            }
            if (since >= flare + 0.5) {
                String[] words = words(v);
                int alpha = (int) (255 * smooth((since - flare - 0.5) / 0.4) * out);
                if (alpha > 8) {
                    int y = h - bar - 58, colour = rarityColour(v);
                    int half = Math.max(90, font.getWidth(words[0]) * 135 / 200 + 24);
                    g.fill(w / 2 - half, y - 6, w / 2 + half, y + 44, (alpha * 150 / 255) << 24 | 0x0b0d0f);
                    g.fill(w / 2 - half, y - 6, w / 2 + half, y - 5, alpha << 24 | colour);
                    g.getMatrices().push();
                    g.getMatrices().translate(w / 2f, y, 0); g.getMatrices().scale(1.35f, 1.35f, 1);
                    g.drawText(font, words[0], -font.getWidth(words[0]) / 2, 0, alpha << 24 | 0xeee9df, true);
                    g.getMatrices().pop();
                    g.drawCenteredTextWithShadow(font, words[1], w / 2, y + 17, alpha << 24 | colour);
                    if (!words[2].isEmpty()) g.drawCenteredTextWithShadow(font, words[2], w / 2, y + 30, alpha << 24 | (DarkUi.MUTED & 0xffffff));
                }
            }
        }
    }
}
