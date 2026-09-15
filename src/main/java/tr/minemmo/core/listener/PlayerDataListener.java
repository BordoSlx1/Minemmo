package tr.minemmo.core.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import tr.minemmo.core.booster.BoosterEffectManager;
import tr.minemmo.core.booster.PlayerBoosterManager;
import tr.minemmo.core.data.PlayerDataManager;

public final class PlayerDataListener implements Listener {

    private final PlayerDataManager playerDataManager;
    private final PlayerBoosterManager playerBoosterManager;
    private final BoosterEffectManager boosterEffectManager;

    public PlayerDataListener(
            PlayerDataManager playerDataManager,
            PlayerBoosterManager playerBoosterManager,
            BoosterEffectManager boosterEffectManager
    ) {
        this.playerDataManager = playerDataManager;
        this.playerBoosterManager = playerBoosterManager;
        this.boosterEffectManager = boosterEffectManager;
    }

    @EventHandler
    public void onPlayerJoin(
            PlayerJoinEvent event
    ) {

        playerDataManager.loadAsync(
                event.getPlayer().getUniqueId()
        );

        playerBoosterManager.load(
                event.getPlayer().getUniqueId()
        );

        boosterEffectManager.reconcile(
                event.getPlayer()
        );
    }

    @EventHandler
    public void onPlayerQuit(
            PlayerQuitEvent event
    ) {

        boosterEffectManager.remove(
                event.getPlayer(),
                null
        );

        playerBoosterManager.clear(
                event.getPlayer().getUniqueId()
        );

        playerDataManager.unloadAsync(
                event.getPlayer().getUniqueId()
        );
    }
}

