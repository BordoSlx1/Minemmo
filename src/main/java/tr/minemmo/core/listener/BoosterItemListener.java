package tr.minemmo.core.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import tr.minemmo.core.MineMMOCore;
import tr.minemmo.core.booster.Booster;
import tr.minemmo.core.booster.BoosterItemManager;
import tr.minemmo.core.booster.BoosterStorage;
import tr.minemmo.core.booster.BoosterStorageManager;

public final class BoosterItemListener
        implements Listener {

    private final MineMMOCore plugin;
    private final BoosterItemManager boosterItemManager;
    private final BoosterStorageManager storageManager;

    public BoosterItemListener(
            MineMMOCore plugin
    ) {

        this.plugin = plugin;

        this.boosterItemManager =
                plugin.getBoosterItemManager();

        this.storageManager =
                plugin.getBoosterStorageManager();
    }

    @EventHandler(
            priority = EventPriority.NORMAL
    )
    public void onPlayerInteract(
            PlayerInteractEvent event
    ) {

        /*
         * Sadece ana el kullanılınca çalışır.
         */
        if (event.getHand()
                != EquipmentSlot.HAND) {

            return;
        }

        Action action =
                event.getAction();

        /*
         * Sadece sağ tık ile booster
         * depoya gönderilir.
         */
        if (action != Action.RIGHT_CLICK_AIR
                && action != Action.RIGHT_CLICK_BLOCK) {

            return;
        }

        Player player =
                event.getPlayer();

        ItemStack item =
                player.getInventory()
                        .getItemInMainHand();

        String boosterId =
                boosterItemManager.getBoosterId(
                        item
                );

        /*
         * Elindeki item booster değilse
         * hiçbir şey yapma.
         */
        if (boosterId == null) {
            return;
        }

        /*
         * Vanilla item kullanımını engelle.
         */
        event.setCancelled(true);

        Booster booster =
                plugin.getBoosterManager()
                        .get(boosterId);

        if (booster == null) {

            player.sendMessage(
                    "§cBu booster artık kayıtlı değil."
            );

            return;
        }

        /*
         * Aynı booster zaten storage'da mı?
         */
        for (BoosterStorage storage :
                storageManager.getStoredBoosters(
                        player.getUniqueId()
                )) {

            if (storage.getBoosterId()
                    .equalsIgnoreCase(
                            booster.getId()
                    )) {

                player.sendMessage(
                        "§cBu takviye zaten "
                                + "deponuzda bulunuyor."
                );

                return;
            }
        }

        /*
         * Storage'da boş slot ara.
         */
        int emptySlot =
                storageManager.findEmptySlot(
                        player.getUniqueId()
                );

        if (emptySlot == -1) {

            player.sendMessage(
                    "§cTakviye deponuz dolu."
            );

            return;
        }

        /*
         * Itemin hâlâ oyuncunun elinde
         * olduğunu kontrol et.
         */
        ItemStack currentItem =
                player.getInventory()
                        .getItemInMainHand();

        if (!boosterItemManager.isBoosterItem(
                currentItem,
                booster.getId()
        )) {

            player.sendMessage(
                    "§cBooster itemi artık elinizde değil."
            );

            return;
        }

        /*
         * Önce storage kaydını oluştur.
         *
         * Başarısız olursa item tüketilmez.
         */
        boolean added =
                storageManager.add(
                        player.getUniqueId(),
                        booster.getId(),
                        emptySlot
                );

        if (!added) {

            player.sendMessage(
                    "§cBooster depoya eklenemedi."
            );

            return;
        }

        /*
         * Storage kaydı başarıyla oluşturuldu.
         * Şimdi itemi oyuncunun elinden tüket.
         */
        removeOneItem(
                player,
                currentItem
        );

        player.sendMessage(
                "§a"
                        + booster.getDisplayName()
                        + " §7takviye deponuza eklendi."
        );

        plugin.getLogger().info(
                "Booster storage'a eklendi: "
                        + player.getUniqueId()
                        + " -> "
                        + booster.getId()
                        + " (Slot "
                        + emptySlot
                        + ")"
        );
    }

    private void removeOneItem(
            Player player,
            ItemStack item
    ) {

        int amount =
                item.getAmount();

        if (amount <= 1) {

            player.getInventory()
                    .setItemInMainHand(
                            null
                    );

            return;
        }

        item.setAmount(
                amount - 1
        );
    }
}