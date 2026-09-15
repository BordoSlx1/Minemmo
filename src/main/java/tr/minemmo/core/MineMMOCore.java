package tr.minemmo.core;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import tr.minemmo.core.booster.BoosterBonusManager;
import tr.minemmo.core.booster.BoosterEffectManager;
import tr.minemmo.core.booster.BoosterManager;
import tr.minemmo.core.booster.BoosterItemManager;
import tr.minemmo.core.booster.PlayerBoosterManager;
import tr.minemmo.core.command.MineMMOCommand;
import tr.minemmo.core.command.TakviyelerCommand;
import tr.minemmo.core.data.DatabaseManager;
import tr.minemmo.core.data.PlayerDataManager;
import tr.minemmo.core.integration.MythicMobDeathService;
import tr.minemmo.core.integration.MythicMobService;
import tr.minemmo.core.integration.MythicMobsHook;
import tr.minemmo.core.listener.BoosterBonusListener;
import tr.minemmo.core.listener.BoosterDamageListener;
import tr.minemmo.core.listener.BoosterItemListener;
import tr.minemmo.core.listener.MythicMobDeathListener;
import tr.minemmo.core.listener.PlayerDataListener;
import tr.minemmo.core.booster.BoosterStorageManager;
import tr.minemmo.core.listener.BoosterStorageListener;

import java.sql.SQLException;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public final class MineMMOCore extends JavaPlugin {

    private static MineMMOCore instance;

    private DatabaseManager databaseManager;
    private PlayerDataManager playerDataManager;

    private BoosterManager boosterManager;
    private BoosterItemManager boosterItemManager;
    private BoosterEffectManager boosterEffectManager;
    private BoosterBonusManager boosterBonusManager;
    private PlayerBoosterManager playerBoosterManager;
    private BoosterStorageManager boosterStorageManager;

    private BoosterStorageListener boosterStorageListener;

    private MythicMobsHook mythicMobsHook;
    private MythicMobService mythicMobService;
    private MythicMobDeathService mythicMobDeathService;

    private int autosaveTaskId = -1;
    private int boosterExpirationTaskId = -1;

    @Override
    public void onEnable() {

        instance = this;

        saveDefaultConfig();

        if (!initializeDatabase()) {
            return;
        }

        initializePlayerDataManager();
        initializeBoosterManager();
        initializeBoosterItemManager();
        initializeBoosterStorageManager();
        initializePlayerBoosterManager();
        initializeBoosterEffectManager();
        initializeBoosterBonusManager();

        initializeMythicMobsHook();
        initializeMythicMobDeathService();

        registerListeners();
        registerCommands();

        startAutosave();
        startBoosterExpirationTask();

        getLogger().info(
                "======================================"
        );

        getLogger().info(
                "       MineMMO-Core v0.5.0"
        );

        getLogger().info(
                "======================================"
        );

        getLogger().info(
                "MineMMO-Core başarıyla başlatıldı."
        );

        getLogger().info(
                "Paper API: 26.1.2"
        );

        getLogger().info(
                "Java: 25"
        );

        getLogger().info(
                "SQLite: Aktif"
        );

        getLogger().info(
                "Booster sistemi: Aktif"
        );

        getLogger().info(
                "Booster efekt sistemi: Aktif"
        );

        getLogger().info(
                "Booster bonus sistemi: Aktif"
        );

        getLogger().info(
                "Oyuncu booster sistemi: Aktif"
        );

        getLogger().info(
                "Booster storage sistemi: Aktif"
        );

        getLogger().info(
                "Booster süre kontrolü: Aktif"
        );

        getLogger().info(
                "MythicMobs entegrasyonu: "
                        + (
                        mythicMobsHook != null
                                && mythicMobsHook.isEnabled()
                                ? "Aktif"
                                : "Pasif"
                )
        );

        getLogger().info(
                "MythicMob ölüm sistemi: Aktif"
        );

        getLogger().info(
                "Autosave: 5 dakika"
        );

        getLogger().info(
                "======================================"
        );
    }

    @Override
    public void onDisable() {

        getLogger().info(
                "MineMMO-Core kapatılıyor..."
        );

        stopAutosave();
        stopBoosterExpirationTask();

        if (playerDataManager != null) {

            try {

                playerDataManager.saveAllSync();

                getLogger().info(
                        "Oyuncu verileri güvenli şekilde kaydedildi."
                );

            } catch (Exception exception) {

                getLogger().log(
                        Level.SEVERE,
                        "Oyuncu verileri kapatılırken "
                                + "kaydedilemedi.",
                        exception
                );
            }
        }

        if (databaseManager != null) {
            databaseManager.close();
        }

        if (playerDataManager != null) {
            playerDataManager.shutdown();
        }

        getLogger().info(
                "MineMMO-Core başarıyla kapatıldı."
        );

        instance = null;
    }

    private boolean initializeDatabase() {

        databaseManager =
                new DatabaseManager(this);

        try {

            databaseManager.connect();

            return true;

        } catch (SQLException exception) {

            getLogger().severe(
                    "======================================"
            );

            getLogger().severe(
                    "MineMMO-Core veritabanı başlatılamadı!"
            );

            getLogger().severe(
                    "Plugin güvenli şekilde kapatılıyor."
            );

            getLogger().severe(
                    "Hata: "
                            + exception.getMessage()
            );

            getLogger().severe(
                    "======================================"
            );

            getServer()
                    .getPluginManager()
                    .disablePlugin(this);

            return false;
        }
    }

    private void initializePlayerDataManager() {

        playerDataManager =
                new PlayerDataManager(
                        this,
                        databaseManager
                );
    }

    private void initializeBoosterManager() {

        boosterManager =
                new BoosterManager(this);

        boosterManager.load();

        getLogger().info(
                "Booster sistemi başlatıldı. "
                        + "Kayıtlı booster: "
                        + boosterManager.getCount()
        );
    }

    private void initializeBoosterItemManager() {

        boosterItemManager =
                new BoosterItemManager(this);

        getLogger().info(
                "Booster item sistemi başlatıldı."
        );
    }

    private void initializePlayerBoosterManager() {

        playerBoosterManager =
                new PlayerBoosterManager(
                        this,
                        boosterManager,
                        databaseManager
                );

        getLogger().info(
                "Oyuncu booster yöneticisi başlatıldı."
        );
    }

    private void initializeBoosterEffectManager() {

        boosterEffectManager =
                new BoosterEffectManager(
                        this,
                        playerBoosterManager
                );

        getLogger().info(
                "Booster efekt sistemi başlatıldı."
        );
    }

    private void initializeBoosterBonusManager() {

        boosterBonusManager =
                new BoosterBonusManager(
                        playerBoosterManager,
                        boosterManager
                );

        getLogger().info(
                "Booster bonus sistemi başlatıldı."
        );
    }

    private void initializeMythicMobsHook() {

        mythicMobsHook =
                new MythicMobsHook(this);

        boolean enabled =
                getConfig().getBoolean(
                        "integrations.mythicmobs.enabled",
                        false
                );

        mythicMobsHook.initialize(enabled);

        mythicMobService =
                new MythicMobService(
                        mythicMobsHook
                );
    }

    private void initializeMythicMobDeathService() {

        mythicMobDeathService =
                new MythicMobDeathService(this);

        getLogger().info(
                "MythicMob ölüm servisi başlatıldı."
        );
    }

    private void initializeBoosterStorageManager() {

        boosterStorageManager =
                new BoosterStorageManager(
                        this,
                        boosterManager,
                        databaseManager
                );

        getLogger().info(
                "Booster storage sistemi başlatıldı."
        );
    }

    private void registerListeners() {

        getServer()
                .getPluginManager()
                .registerEvents(
                        new PlayerDataListener(
                                playerDataManager,
                                playerBoosterManager,
                                boosterEffectManager
                        ),
                        this
                );

        getServer()
                .getPluginManager()
                .registerEvents(
                        new BoosterItemListener(this),
                        this
                );

        getServer()
                .getPluginManager()
                .registerEvents(
                        new BoosterBonusListener(
                                boosterBonusManager
                        ),
                        this
                );

        getServer()
                .getPluginManager()
                .registerEvents(
                        new BoosterDamageListener(
                                boosterBonusManager
                        ),
                        this
                );

        getServer()
                .getPluginManager()
                .registerEvents(
                        new MythicMobDeathListener(this),
                        this
                );

        boosterStorageListener =
                new BoosterStorageListener(this);

        getServer()
                .getPluginManager()
                .registerEvents(
                        boosterStorageListener,
                        this
                );
    }

    private void registerCommands() {

    MineMMOCommand mineMMOCommand =
            new MineMMOCommand(this);

    if (getCommand("minemmo") != null) {

        getCommand("minemmo")
                .setExecutor(mineMMOCommand);

        getCommand("minemmo")
                .setTabCompleter(mineMMOCommand);
    }

    TakviyelerCommand takviyelerCommand =
            new TakviyelerCommand(this);

    if (getCommand("takviyeler") != null) {

        getCommand("takviyeler")
                .setExecutor(takviyelerCommand);
    }
}

    private void startAutosave() {

        stopAutosave();

        long intervalTicks =
                20L * 60L * 5L;

        autosaveTaskId =
                getServer()
                        .getScheduler()
                        .runTaskTimerAsynchronously(
                                this,
                                () -> {

                                    if (playerDataManager
                                            == null) {

                                        return;
                                    }

                                    getLogger().info(
                                            "Autosave başlatılıyor..."
                                    );

                                    playerDataManager
                                            .saveAllAsync()
                                            .exceptionally(
                                                    exception -> {

                                                        getLogger().log(
                                                                Level.SEVERE,
                                                                "Autosave sırasında hata oluştu.",
                                                                exception
                                                        );

                                                        return null;
                                                    }
                                            );
                                },
                                intervalTicks,
                                intervalTicks
                        )
                        .getTaskId();

        getLogger().info(
                "5 dakikalık autosave başlatıldı."
        );
    }

    private void startBoosterExpirationTask() {

        stopBoosterExpirationTask();

        long intervalTicks =
                20L * 5L;

        boosterExpirationTaskId =
                getServer()
                        .getScheduler()
                        .runTaskTimer(
                                this,
                                () -> {

                                    if (playerBoosterManager
                                            == null) {

                                        return;
                                    }

                                    playerBoosterManager
                                            .removeExpiredAsync(
                                                    this::handleExpiredBoosters
                                            );
                                },
                                intervalTicks,
                                intervalTicks
                        )
                        .getTaskId();

        getLogger().info(
                "Booster süre kontrolü başlatıldı. "
                        + "Kontrol aralığı: 5 saniye."
        );
    }

    private void handleExpiredBoosters(
            Set<UUID> cleanedPlayers
    ) {

        if (cleanedPlayers == null
                || cleanedPlayers.isEmpty()) {

            return;
        }

        for (UUID playerUuid :
                cleanedPlayers) {

            Player player =
                    Bukkit.getPlayer(
                            playerUuid
                    );

            if (player == null
                    || !player.isOnline()) {

                continue;
            }

            if (boosterEffectManager
                    == null) {

                continue;
            }

            boosterEffectManager
                    .reconcile(player);
        }
    }

    private void stopBoosterExpirationTask() {

        if (boosterExpirationTaskId == -1) {
            return;
        }

        getServer()
                .getScheduler()
                .cancelTask(
                        boosterExpirationTaskId
                );

        boosterExpirationTaskId = -1;

        getLogger().info(
                "Booster süre kontrolü durduruldu."
        );
    }

    private void stopAutosave() {

        if (autosaveTaskId == -1) {
            return;
        }

        getServer()
                .getScheduler()
                .cancelTask(
                        autosaveTaskId
                );

        autosaveTaskId = -1;

        getLogger().info(
                "Autosave durduruldu."
        );
    }

    public static MineMMOCore getInstance() {
        return instance;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public BoosterManager getBoosterManager() {
        return boosterManager;
    }

    public BoosterItemManager getBoosterItemManager() {
        return boosterItemManager;
    }

    public BoosterEffectManager getBoosterEffectManager() {
        return boosterEffectManager;
    }

    public BoosterBonusManager getBoosterBonusManager() {
        return boosterBonusManager;
    }

    public PlayerBoosterManager getPlayerBoosterManager() {
        return playerBoosterManager;
    }

    public BoosterStorageManager getBoosterStorageManager() {
        return boosterStorageManager;
    }

    public BoosterStorageListener getBoosterStorageListener() {
        return boosterStorageListener;
    }

    public MythicMobsHook getMythicMobsHook() {
        return mythicMobsHook;
    }

    public MythicMobService getMythicMobService() {
        return mythicMobService;
    }

    public MythicMobDeathService getMythicMobDeathService() {
        return mythicMobDeathService;
    }
}