package com.hcbossbar;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;

public class BossCommand extends AbstractAsyncCommand {

    private final HC_BossBarPlugin plugin;

    public BossCommand(HC_BossBarPlugin plugin) {
        super("boss", "Mark or clear a boss target");
        this.plugin = plugin;
        this.addSubCommand(new MarkSubCommand());
        this.addSubCommand(new ClearSubCommand());
    }

    @Nonnull
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
        if (ctx.sender() instanceof Player player) {
            player.getPlayerRef().sendMessage(
                Message.raw("Usage: /boss mark | /boss clear").color("#FFAA00"));
        }
        return CompletableFuture.completedFuture(null);
    }

    private class MarkSubCommand extends AbstractAsyncCommand {
        public MarkSubCommand() {
            super("mark", "Mark the entity you're looking at as a boss");
        }

        @Nonnull
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            if (!(ctx.sender() instanceof Player player)) {
                return CompletableFuture.completedFuture(null);
            }

            Ref<EntityStore> ref = player.getReference();
            if (ref == null || !ref.isValid()) {
                return CompletableFuture.completedFuture(null);
            }

            Store<EntityStore> store = ref.getStore();
            World world = store.getExternalData().getWorld();
            PlayerRef playerRef = player.getPlayerRef();

            world.execute(() -> {
                Ref<EntityStore> targetRef = TargetUtil.getTargetEntity(ref, 16f, store);
                if (targetRef == null || !targetRef.isValid()) {
                    playerRef.sendMessage(Message.raw("No entity found. Look at an NPC and try again.").color("#FF5555"));
                    return;
                }

                // Check it has health stats (is a living entity)
                EntityStatMap statMap = store.getComponent(targetRef, EntityStatMap.getComponentType());
                if (statMap == null) {
                    playerRef.sendMessage(Message.raw("That entity has no health stats.").color("#FF5555"));
                    return;
                }

                // Get entity UUID
                UUIDComponent uuidComp = store.getComponent(targetRef, UUIDComponent.getComponentType());
                if (uuidComp == null) {
                    playerRef.sendMessage(Message.raw("That entity has no UUID.").color("#FF5555"));
                    return;
                }

                // Get boss name
                String bossName = "Boss";
                NPCEntity npc = store.getComponent(targetRef, NPCEntity.getComponentType());
                if (npc != null && npc.getRoleName() != null && !npc.getRoleName().isEmpty()) {
                    bossName = npc.getRoleName();
                }

                plugin.markBoss(player, playerRef, world, uuidComp.getUuid(), bossName);
                playerRef.sendMessage(Message.raw("Marked " + bossName + " as boss target.").color("#55FF55"));
            });

            return CompletableFuture.completedFuture(null);
        }
    }

    private class ClearSubCommand extends AbstractAsyncCommand {
        public ClearSubCommand() {
            super("clear", "Clear the current boss bar");
        }

        @Nonnull
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            if (!(ctx.sender() instanceof Player player)) {
                return CompletableFuture.completedFuture(null);
            }

            plugin.clearBoss(player);
            player.getPlayerRef().sendMessage(Message.raw("Boss bar cleared.").color("#FFAA00"));

            return CompletableFuture.completedFuture(null);
        }
    }
}
