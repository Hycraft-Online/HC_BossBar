package com.hcbossbar;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.Anchor;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;

public class BossBarHud extends CustomUIHud {

    private static final int BAR_MAX_WIDTH = 500;

    private final String initialBossName;

    public BossBarHud(@Nonnull PlayerRef playerRef, String initialBossName) {
        super(playerRef);
        this.initialBossName = initialBossName;
    }

    @Override
    protected void build(@Nonnull UICommandBuilder builder) {
        builder.append("BossBar.ui");
        builder.set("#BossName.Text", initialBossName);
    }

    public void updateHealth(float percent, float trailPercent, String bossName) {
        UICommandBuilder commands = new UICommandBuilder();

        commands.set("#BossName.Text", bossName);

        int fillWidth = Math.max(0, Math.min(BAR_MAX_WIDTH, (int) (BAR_MAX_WIDTH * percent)));
        Anchor fillAnchor = new Anchor();
        fillAnchor.setLeft(Value.of(0));
        fillAnchor.setWidth(Value.of(fillWidth));
        fillAnchor.setHeight(Value.of(14));
        commands.setObject("#BarFill.Anchor", fillAnchor);

        int trailWidth = Math.max(0, Math.min(BAR_MAX_WIDTH, (int) (BAR_MAX_WIDTH * trailPercent)));
        Anchor trailAnchor = new Anchor();
        trailAnchor.setLeft(Value.of(0));
        trailAnchor.setWidth(Value.of(trailWidth));
        trailAnchor.setHeight(Value.of(14));
        commands.setObject("#BarTrail.Anchor", trailAnchor);

        update(false, commands);
    }
}
