package com.hcbossbar;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;


import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class HC_BossBarPlugin extends JavaPlugin {

    private static HC_BossBarPlugin instance;

    private final Map<UUID, BossTracker> activeTrackers = new ConcurrentHashMap<>();

    public HC_BossBarPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    public static HC_BossBarPlugin getInstance() {
        return instance;
    }

    @Override
    public void setup() {
        instance = this;
        getCommandRegistry().registerCommand(new BossCommand(this));

        getEventRegistry().register(PlayerDisconnectEvent.class, event -> {
            UUID playerUuid = event.getPlayerRef().getUuid();
            activeTrackers.remove(playerUuid);
        });
    }

    @Override
    public void start() {
        HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(this::updateAllBossBars,
            250, 250, TimeUnit.MILLISECONDS);
        getLogger().at(Level.INFO).log("HC_BossBar started");
    }

    @Override
    public void shutdown() {
        activeTrackers.clear();
    }

    public void markBoss(Player player, PlayerRef playerRef, World world, UUID bossUuid, String bossName) {
        UUID playerUuid = playerRef.getUuid();

        // Ensure name is never null to prevent client crash
        if (bossName == null || bossName.isEmpty()) {
            bossName = "Boss";
        }

        // Clear existing boss bar if any
        BossTracker existing = activeTrackers.remove(playerUuid);
        if (existing != null) {
            HudWrapper.hideCustomHud(player, "BossBar");
        }

        BossBarHud hud = new BossBarHud(playerRef, bossName);
        BossTracker tracker = new BossTracker(bossUuid, hud, world, bossName);

        activeTrackers.put(playerUuid, tracker);
        HudWrapper.setCustomHud(player, playerRef, "BossBar", hud);
    }

    public void showBossDefeated(Player player, PlayerRef playerRef, World world, String bossName, int durationMs) {
        if (bossName == null || bossName.isEmpty()) {
            bossName = "Boss";
        }
        if (durationMs < 2000) {
            durationMs = 5000;
        }

        BossDefeatedOverlay overlay = new BossDefeatedOverlay(playerRef, player, bossName, durationMs);
        HudWrapper.setCustomHud(player, playerRef, "BossDefeated", overlay);
        overlay.startFadeSequence();
    }

    public void clearBoss(Player player) {
        UUID playerUuid = player.getPlayerRef().getUuid();
        BossTracker tracker = activeTrackers.remove(playerUuid);
        if (tracker != null) {
            HudWrapper.hideCustomHud(player, "BossBar");
        }
    }

    private void updateAllBossBars() {
        for (Map.Entry<UUID, BossTracker> entry : activeTrackers.entrySet()) {
            UUID playerUuid = entry.getKey();
            BossTracker tracker = entry.getValue();
            World world = tracker.getWorld();

            if (world == null) {
                activeTrackers.remove(playerUuid);
                continue;
            }

            try {
                world.execute(() -> {
                    Store<EntityStore> store = world.getEntityStore().getStore();

                    // Validate player entity ref before doing any work
                    Ref<EntityStore> playerEntityRef = store.getExternalData().getRefFromUUID(playerUuid);
                    if (playerEntityRef == null || !playerEntityRef.isValid()) {
                        activeTrackers.remove(playerUuid);
                        return;
                    }

                    // Resolve boss ref from UUID
                    Ref<EntityStore> bossRef = store.getExternalData().getRefFromUUID(tracker.getBossEntityUuid());
                    if (bossRef == null || !bossRef.isValid()) {
                        // Boss is gone - find player and hide bar
                        hideBossBarForPlayer(playerUuid, store);
                        return;
                    }

                    // Read health
                    EntityStatMap statMap = store.getComponent(bossRef, EntityStatMap.getComponentType());
                    if (statMap == null) {
                        hideBossBarForPlayer(playerUuid, store);
                        return;
                    }

                    EntityStatValue health = statMap.get(DefaultEntityStatTypes.getHealth());
                    if (health == null) {
                        hideBossBarForPlayer(playerUuid, store);
                        return;
                    }

                    float percent = health.asPercentage();
                    if (percent <= 0) {
                        // Boss died
                        hideBossBarForPlayer(playerUuid, store);
                        return;
                    }

                    float trailPercent = tracker.updateTrail(percent);
                    tracker.getHud().updateHealth(percent, trailPercent, tracker.getDisplayName());
                });
            } catch (Exception e) {
                getLogger().at(Level.WARNING).log("Error updating boss bar for " + playerUuid + ": " + e.getMessage());
            }
        }
    }

    private void hideBossBarForPlayer(UUID playerUuid, Store<EntityStore> store) {
        BossTracker tracker = activeTrackers.remove(playerUuid);
        if (tracker == null) return;

        // Find the player entity to hide the HUD
        Ref<EntityStore> playerEntityRef = store.getExternalData().getRefFromUUID(playerUuid);
        if (playerEntityRef == null || !playerEntityRef.isValid()) return;

        Player player = store.getComponent(playerEntityRef, Player.getComponentType());
        if (player == null) return;

        HudWrapper.hideCustomHud(player, "BossBar");
    }
}
