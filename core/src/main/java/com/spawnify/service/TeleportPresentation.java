package com.spawnify.service;

public record TeleportPresentation(
        boolean messageEnabled,
        String messageText,
        String messageKey,
        boolean completionMessageEnabled,
        String completionMessageText,
        String completionMessageKey,
        boolean titleEnabled,
        String titleText,
        String titleSubtitle,
        int titleFadeIn,
        int titleStay,
        int titleFadeOut,
        boolean soundEnabled,
        String soundName,
        float soundVolume,
        float soundPitch,
        boolean particlesEnabled,
        String particlesName,
        int particlesCount,
        double particlesSpeed,
        double particlesOffset,
        boolean blindnessEnabled
) {
}
