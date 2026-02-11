package com.hcbossbar;

import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;

public class BossDefeatedOverlay extends CustomUIHud {

    private static final int FADE_STEPS = 40;
    private static final long FADE_STEP_MS = 50;
    private static final long FADE_DURATION_MS = FADE_STEPS * FADE_STEP_MS; // 2000ms
    private static final float MAX_ALPHA = 0.90f;

    private final String bossName;
    private final Player player;
    private final int durationMs;

    public BossDefeatedOverlay(@Nonnull PlayerRef playerRef, Player player, String bossName, int durationMs) {
        super(playerRef);
        this.player = player;
        this.bossName = bossName != null && !bossName.isEmpty() ? bossName : "Boss";
        this.durationMs = durationMs;
    }

    @Override
    protected void build(@Nonnull UICommandBuilder builder) {
        builder.append("BossDefeated.ui");
        builder.set("#BossNameLabel.Text", letterSpace(bossName));
    }

    public void startFadeSequence() {
        float stepAlpha = MAX_ALPHA / FADE_STEPS;

        // Make visible on first step
        HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
            UICommandBuilder commands = new UICommandBuilder();
            commands.set("#BossDefeatedRoot.Visible", true);
            update(false, commands);
        }, FADE_STEP_MS, TimeUnit.MILLISECONDS);

        // Fade in over 2s
        for (int i = 0; i < FADE_STEPS; i++) {
            final float alpha = (i + 1) * stepAlpha;
            long delayMs = (i + 1) * FADE_STEP_MS;
            HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> updateAlpha(alpha), delayMs, TimeUnit.MILLISECONDS);
        }

        // Fade out over final 2s
        long fadeOutStart = durationMs - FADE_DURATION_MS;
        for (int i = 0; i < FADE_STEPS; i++) {
            final float alpha = MAX_ALPHA - ((i + 1) * stepAlpha);
            long delayMs = fadeOutStart + ((i + 1) * FADE_STEP_MS);
            HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> updateAlpha(alpha), delayMs, TimeUnit.MILLISECONDS);
        }

        // Auto-hide after full duration
        HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
            HudWrapper.hideCustomHud(player, "BossDefeated");
        }, durationMs, TimeUnit.MILLISECONDS);
    }

    private void updateAlpha(float alpha) {
        int alphaInt = Math.max(0, Math.min(255, (int) (alpha * 255)));
        String bgColor = String.format("#000000%02x", alphaInt);

        // Text fades in faster than background
        float textAlpha = Math.min(1.0f, alpha / 0.45f);
        int textAlphaInt = Math.max(0, Math.min(255, (int) (textAlpha * 255)));
        String goldWithAlpha = String.format("#c8a84e%02x", textAlphaInt);

        // Subtitle is dimmer gold
        int subtitleAlphaInt = Math.max(0, Math.min(255, (int) (textAlpha * 0.8f * 255)));
        String dimGoldWithAlpha = String.format("#9a7b3a%02x", subtitleAlphaInt);

        // Lines fade with text
        String lineWithAlpha = String.format("#c8a84e%02x", (int) (textAlpha * 0.6f * 255));

        UICommandBuilder commands = new UICommandBuilder();
        commands.set("#BossDefeatedRoot.Background", bgColor);
        commands.set("#BossNameLabel.Style.TextColor", goldWithAlpha);
        commands.set("#DefeatedLabel.Style.TextColor", dimGoldWithAlpha);
        commands.set("#TopLine.Background", lineWithAlpha);
        commands.set("#MidLine.Background", goldWithAlpha);
        commands.set("#BottomLine.Background", lineWithAlpha);

        update(false, commands);
    }

    private static String letterSpace(String text) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            if (i > 0) {
                sb.append(text.charAt(i) == ' ' ? "   " : " ");
            }
            sb.append(text.charAt(i));
        }
        return sb.toString();
    }
}
