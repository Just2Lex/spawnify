package com.spawnify.config;

import com.spawnify.service.TeleportPresentation;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;

public final class PluginConfig {

    private final JavaPlugin plugin;

    public PluginConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
    }

    private FileConfiguration config() {
        return plugin.getConfig();
    }

    private boolean bool(String path, boolean fallback) {
        return config().getBoolean(path, fallback);
    }

    private int integer(String path, int fallback) {
        return Math.max(Integer.MIN_VALUE, config().getInt(path, fallback));
    }

    private int boundedInt(String path, int fallback, int min, int max) {
        return clamp(config().getInt(path, fallback), min, max);
    }

    private double dbl(String path, double fallback) {
        return config().getDouble(path, fallback);
    }

    private String str(String path, String fallback) {
        String value = config().getString(path);
        return value == null ? fallback : value;
    }

    public int guiRows() {
        return boundedInt("settings.gui.rows", 6, 2, 6);
    }

    public String guiTitle() {
        return str("settings.gui.title", "&8Spawnify - Spawns");
    }

    public boolean fillerEnabled() {
        return bool("settings.gui.filler-enabled", true);
    }

    public Material fillerMaterial() {
        Material material = Material.matchMaterial(str("settings.gui.filler-material", "GRAY_STAINED_GLASS_PANE").toUpperCase(Locale.ROOT));
        return material == null ? Material.GRAY_STAINED_GLASS_PANE : material;
    }

    public boolean allowIdentifierSelection() {
        return bool("settings.selection.allow-identifier-argument", true);
    }

    public String singleSelectionBehavior() {
        return normalizeBehavior(str("settings.selection.single-behavior",
                bool("settings.selection.direct-teleport-when-single", true) ? "DIRECT" : "GUI"));
    }

    public String multipleSelectionBehavior() {
        return normalizeBehavior(str("settings.selection.multiple-behavior",
                bool("settings.selection.open-gui-when-multiple", true) ? "GUI" : "DIRECT"));
    }

    public boolean directTeleportWhenSingle() {
        return "DIRECT".equalsIgnoreCase(singleSelectionBehavior());
    }

    public boolean openGuiWhenMultiple() {
        return "GUI".equalsIgnoreCase(multipleSelectionBehavior());
    }

    public int teleportDelaySeconds() {
        return Math.max(0, config().getInt("settings.teleport.delay-seconds", 5));
    }

    public int cooldownSeconds() {
        return Math.max(0, config().getInt("settings.teleport.cooldown-seconds", 30));
    }

    public boolean useBlindness() {
        return bool("settings.teleport.use-blindness", true);
    }

    public boolean preserveSavedOrientation() {
        return bool("settings.teleport.preserve-saved-orientation", true);
    }

    public boolean forceSavedOrientation() {
        return bool("settings.teleport.force-saved-orientation", true);
    }

    public boolean cancelOnMove() {
        return bool("settings.teleport.cancel-on-move", false);
    }

    public boolean personalSpawnEnabled() {
        return bool("settings.personal.enabled", true);
    }

    public boolean defaultWorldSpawnFallback() {
        return bool("settings.selection.default-world-spawn-fallback", true);
    }

    public boolean teleportCountdownTitleEnabled() {
        return bool("settings.teleport.countdown.title.enabled", false);
    }

    public boolean teleportCountdownSubtitleEnabled() {
        return bool("settings.teleport.countdown.subtitle.enabled", true);
    }

    public String teleportCountdownTitleText() {
        return str("settings.teleport.countdown.title.text", "&aТелепортация");
    }

    public String teleportCountdownSubtitleText() {
        return str("settings.teleport.countdown.subtitle.text", "&2%seconds%...");
    }

    public String teleportCountdownSubtitleMessageKey() {
        return str("settings.teleport.countdown.subtitle.message-key", "teleport.countdown.subtitle");
    }

    public int teleportCountdownTitleFadeIn() {
        return Math.max(0, config().getInt("settings.teleport.countdown.title.fade-in", 10));
    }

    public int teleportCountdownTitleStay() {
        return Math.max(0, config().getInt("settings.teleport.countdown.title.stay", 20));
    }

    public int teleportCountdownTitleFadeOut() {
        return Math.max(0, config().getInt("settings.teleport.countdown.title.fade-out", 10));
    }

    public boolean connectionTitleEnabled() {
        return bool("settings.connection.title.enabled", false);
    }

    public boolean connectionTitleFirstJoinEnabled() {
        return bool("settings.connection.title.first-join-enabled", true);
    }

    public boolean connectionTitleRepeatJoinEnabled() {
        return bool("settings.connection.title.repeat-join-enabled", true);
    }

    public String connectionTitleText() {
        return str("settings.connection.title.text", "&aSpawnify");
    }

    public String connectionSubtitleText() {
        return str("settings.connection.subtitle.text", "&7Добро пожаловать");
    }

    public int connectionTitleFadeIn() {
        return Math.max(0, config().getInt("settings.connection.title.fade-in", 10));
    }

    public int connectionTitleStay() {
        return Math.max(0, config().getInt("settings.connection.title.stay", 40));
    }

    public int connectionTitleFadeOut() {
        return Math.max(0, config().getInt("settings.connection.title.fade-out", 10));
    }

    public boolean firstJoinEnabled() {
        return bool("settings.join.first-join.enabled", true);
    }

    public String firstJoinSpawnId() {
        return str("settings.join.first-join.spawn-id", "first-join");
    }

    public int firstJoinTeleportDelaySeconds() {
        return Math.max(0, config().getInt("settings.join.first-join.teleport-delay-seconds", 0));
    }

    public boolean firstJoinApplyCooldown() {
        return bool("settings.join.first-join.apply-cooldown", false);
    }

    public int firstJoinCooldownSeconds() {
        return Math.max(0, config().getInt("settings.join.first-join.cooldown-seconds", cooldownSeconds()));
    }

    public boolean firstJoinFallbackToWorldSpawn() {
        return bool("settings.join.first-join.fallback-to-world-spawn", true);
    }

    public String firstJoinTargetMode() {
        return normalizeBehavior(str("settings.join.first-join.target-mode", "BEST_AVAILABLE"));
    }

    public TeleportPresentation firstJoinPresentation() {
        return presentation(
                "settings.join.first-join.presentation",
                "join.first.message",
                "&aПервый вход: телепорт на спавн.",
                "teleport.complete",
                "&aТелепортация завершена.",
                "&aПервый вход",
                "&7Добро пожаловать на сервер",
                false
        );
    }

    public boolean repeatJoinEnabled() {
        return bool("settings.join.repeat-join.enabled", false);
    }

    public String repeatJoinTargetMode() {
        return normalizeBehavior(str("settings.join.repeat-join.target-mode",
                bool("settings.join.repeat-join.teleport-to-world-spawn", false) ? "WORLD_SPAWN" : "BEST_AVAILABLE"));
    }

    public String repeatJoinSpawnId() {
        return str("settings.join.repeat-join.spawn-id", "");
    }

    public int repeatJoinTeleportDelaySeconds() {
        return Math.max(0, config().getInt("settings.join.repeat-join.teleport-delay-seconds", 0));
    }

    public boolean repeatJoinApplyCooldown() {
        return bool("settings.join.repeat-join.apply-cooldown", false);
    }

    public int repeatJoinCooldownSeconds() {
        return Math.max(0, config().getInt("settings.join.repeat-join.cooldown-seconds", cooldownSeconds()));
    }

    public boolean repeatJoinFallbackToWorldSpawn() {
        return bool("settings.join.repeat-join.fallback-to-world-spawn", true);
    }

    public TeleportPresentation repeatJoinPresentation() {
        return presentation(
                "settings.join.repeat-join.presentation",
                "join.repeat.message",
                "&aВход: телепорт на спавн мира.",
                "teleport.complete",
                "&aТелепортация завершена.",
                "&aС возвращением",
                "&7Добро пожаловать обратно",
                false
        );
    }

    public boolean deathEnabled() {
        return bool("settings.death.enabled", true);
    }

    public String deathSpawnId() {
        return str("settings.death.spawn-id", "death");
    }

    public int deathRespawnDelaySeconds() {
        return Math.max(0, config().getInt("settings.death.respawn-delay-seconds", 0));
    }

    public int deathTeleportDelaySeconds() {
        return Math.max(0, config().getInt("settings.death.teleport-delay-seconds", 0));
    }

    public boolean deathApplyCooldown() {
        return bool("settings.death.apply-cooldown", false);
    }

    public int deathCooldownSeconds() {
        return Math.max(0, config().getInt("settings.death.cooldown-seconds", cooldownSeconds()));
    }

    public boolean deathFallbackToWorldSpawn() {
        return bool("settings.death.fallback-to-world-spawn", true);
    }

    public TeleportPresentation deathPresentation() {
        return presentation(
                "settings.death.presentation",
                "death.message",
                "&aТочка возрождения обновлена.",
                "teleport.complete",
                "&aТелепортация завершена.",
                "&cСмерть",
                "&7Точка возрождения обновлена",
                false
        );
    }

    public boolean voidEnabled() {
        return bool("settings.void.enabled", true);
    }

    public double voidYThreshold() {
        return dbl("settings.void.y-threshold", -64.0D);
    }

    public int voidTeleportDelaySeconds() {
        return Math.max(0, config().getInt("settings.void.teleport-delay-seconds", 0));
    }

    public int voidTeleportCooldownSeconds() {
        return Math.max(0, config().getInt("settings.void.teleport-cooldown-seconds", 5));
    }

    public boolean voidApplyCooldown() {
        return bool("settings.void.apply-cooldown", false);
    }

    public TeleportPresentation voidPresentation() {
        return presentation(
                "settings.void.presentation",
                "void.message",
                "&cВы упали в бездну. Телепорт на спавн.",
                "teleport.complete",
                "&aТелепортация завершена.",
                "&cБездна",
                "&7Телепортация на спавн",
                false
        );
    }

    public TeleportPresentation globalTeleportPresentation() {
        return presentation(
                "settings.teleport.presentation",
                "teleport.spawn.message",
                "&aТелепорт на спавн: &f%spawn%",
                "teleport.complete",
                "&aТелепортация завершена.",
                "&aТелепортация",
                "&7Ожидайте...",
                false
        );
    }

    private TeleportPresentation presentation(
            String basePath,
            String fallbackMessageKey,
            String fallbackMessageText,
            String fallbackCompletionKey,
            String fallbackCompletionText,
            String fallbackTitleText,
            String fallbackSubtitleText,
            boolean fallbackBlindness) {

        boolean messageEnabled = bool(basePath + ".message.enabled", true);
        String messageKey = str(basePath + ".message.key", fallbackMessageKey);
        String messageText = str(basePath + ".message.text", fallbackMessageText);

        boolean completionEnabled = bool(basePath + ".completion.enabled", true);
        String completionKey = str(basePath + ".completion.key", fallbackCompletionKey);
        String completionText = str(basePath + ".completion.text", fallbackCompletionText);

        boolean titleEnabled = bool(basePath + ".title.enabled", true);
        String titleText = str(basePath + ".title.text", fallbackTitleText);
        String titleSubtitle = str(basePath + ".title.subtitle", fallbackSubtitleText);
        int fadeIn = Math.max(0, config().getInt(basePath + ".title.fade-in", 10));
        int stay = Math.max(0, config().getInt(basePath + ".title.stay", 40));
        int fadeOut = Math.max(0, config().getInt(basePath + ".title.fade-out", 10));

        boolean soundEnabled = bool(basePath + ".effects.sound.enabled", false);
        String soundName = str(basePath + ".effects.sound.name", "ENTITY_ENDERMAN_TELEPORT");
        float soundVolume = (float) dbl(basePath + ".effects.sound.volume", 1.0D);
        float soundPitch = (float) dbl(basePath + ".effects.sound.pitch", 1.0D);

        boolean particlesEnabled = bool(basePath + ".effects.particles.enabled", false);
        String particlesName = str(basePath + ".effects.particles.name", "PORTAL");
        int particlesCount = Math.max(0, config().getInt(basePath + ".effects.particles.count", 20));
        double particlesSpeed = dbl(basePath + ".effects.particles.speed", 0.15D);
        double particlesOffset = dbl(basePath + ".effects.particles.offset", 0.35D);

        boolean blindnessEnabled = bool(basePath + ".blindness.enabled", fallbackBlindness);

        return new TeleportPresentation(
                messageEnabled,
                messageText,
                messageKey,
                completionEnabled,
                completionText,
                completionKey,
                titleEnabled,
                titleText,
                titleSubtitle,
                fadeIn,
                stay,
                fadeOut,
                soundEnabled,
                soundName,
                soundVolume,
                soundPitch,
                particlesEnabled,
                particlesName,
                particlesCount,
                particlesSpeed,
                particlesOffset,
                blindnessEnabled
        );
    }

    private String normalizeBehavior(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
