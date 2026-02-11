package com.hcbossbar;

import com.hypixel.hytale.server.core.universe.world.World;

import java.util.UUID;

public class BossTracker {
    private final UUID bossEntityUuid;
    private final BossBarHud hud;
    private final World world;
    private final String displayName;
    private float trailPercent = 1.0f;
    private float lastActualPercent = 1.0f;
    private long lastDamageTime = 0;

    private static final long TRAIL_DELAY_MS = 300;
    private static final float TRAIL_LERP_SPEED = 0.4f;

    public BossTracker(UUID bossEntityUuid, BossBarHud hud, World world, String displayName) {
        this.bossEntityUuid = bossEntityUuid;
        this.hud = hud;
        this.world = world;
        this.displayName = displayName;
    }

    public UUID getBossEntityUuid() {
        return bossEntityUuid;
    }

    public BossBarHud getHud() {
        return hud;
    }

    public World getWorld() {
        return world;
    }

    public String getDisplayName() {
        return displayName;
    }

    public float getTrailPercent() {
        return trailPercent;
    }

    /**
     * Update trail and return the current trail percent for rendering.
     * The trail holds briefly after damage, then lerps toward actual health.
     */
    public float updateTrail(float actualPercent) {
        if (actualPercent < lastActualPercent) {
            // Damage was taken - mark the time
            lastDamageTime = System.currentTimeMillis();
        }
        lastActualPercent = actualPercent;

        long elapsed = System.currentTimeMillis() - lastDamageTime;
        if (elapsed > TRAIL_DELAY_MS && trailPercent > actualPercent) {
            // Lerp trail toward actual health
            trailPercent = trailPercent + (actualPercent - trailPercent) * TRAIL_LERP_SPEED;
            // Snap if very close
            if (trailPercent - actualPercent < 0.005f) {
                trailPercent = actualPercent;
            }
        }

        // Trail should never be below actual health (healing case)
        if (trailPercent < actualPercent) {
            trailPercent = actualPercent;
        }

        return trailPercent;
    }
}
