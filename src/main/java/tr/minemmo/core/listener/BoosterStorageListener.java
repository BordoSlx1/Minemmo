package tr.minemmo.core.listener;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import tr.minemmo.core.MineMMOCore;
import tr.minemmo.core.booster.Booster;
import tr.minemmo.core.booster.BoosterItemManager;
import tr.minemmo.core.booster.BoosterStorage;
import tr.minemmo.core.booster.BoosterStorageManager;

import java.util.List;
import java.util.Map;

public final class BoosterStorageListener
        implements Listener {

    public static final String TITLE =
            "§8Takviyeler";

    private final MineMMOCore plugin;
    private final BoosterStorageManager storageManager;
    private final BoosterItemManager itemManager;

    public BoosterStorageListener(
            MineMMOCore plugin
    ) {

        this.plugin = plugin;

        this.storageManager =
                plugin.getBoosterStorageManager();

        this.itemManager =
                plugin.getBoosterItemManager();
    }

    public void open(
            Player player
    ) {

        Inventory inventory =
                Bukkit.createInventory(
                        null,
                        BoosterStorageManager.STORAGE_SIZE,
                        TITLE
                );

        loadInventory(
                player,
                inventory
        );

        player.openInventory(
                inventory
        );
    }

    private void loadInventory(
            Player player,
            Inventory inventory
    ) {

        List<BoosterStorage> boosters =
                storageManager.getStoredBoosters(
                        player.getUniqueId()
                );

        for (BoosterStorage storage :
                boosters) {

            int slot =
                    storage.getSlot();

            if (slot < 0
                    || slot >= inventory.getSize()) {

                continue;
            }

            Booster booster =
                    plugin.getBoosterManager()
                            .get(
                                    storage.getBoosterId()
                            );

            if (booster == null) {
                continue;
            }

            ItemStack item =
                    createStorageItem(
                            booster
                    );

            if (item == null) {
                continue;
            }

            inventory.setItem(
                    slot,
                    item
            );
        }
    }

    private ItemStack createStorageItem(
            Booster booster
    ) {

        ItemStack item =
                itemManager.createItem(
                        booster
                );

        if (item == null) {
            return null;
        }

        /*
         * Storage içerisindeki lore artık
         * boosters.yml üzerinden geliyor.
         */
        item.setLore(
                booster.getStorageLore()
        );

        return item;
    }

    @EventHandler(
            priority = EventPriority.HIGHEST
    )
    public void onInventoryClick(
            InventoryClickEvent event
    ) {

        if (!event.getView()
                .getTitle()
                .equals(TITLE)) {

            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked()
                instanceof Player player)) {

            return;
        }

        /*
         * Sadece üst GUI alanındaki tıklamalar.
         */
        if (event.getRawSlot() < 0
                || event.getRawSlot()
                >= event.getView()
                .getTopInventory()
                .getSize()) {

            return;
        }

        /*
         * Sadece SAĞ TIK.
         */
        if (!event.isRightClick()) {
            return;
        }

        int slot =
                event.getRawSlot();

        BoosterStorage storage =
                storageManager.get(
                        player.getUniqueId(),
                        slot
                );

        if (storage == null) {
            return;
        }

        Booster booster =
                plugin.getBoosterManager()
                        .get(
                                storage.getBoosterId()
                        );

        if (booster == null) {

            player.sendMessage(
                    "§cBu booster artık kayıtlı değil."
            );

            return;
        }

        ItemStack item =
                itemManager.createItem(
                        booster
                );

        /*
         * Oyuncunun mevcut envanter durumunu
         * güvenlik amacıyla sakla.
         */
        ItemStack[] previousContents =
                player.getInventory()
                        .getStorageContents();

        /*
         * Önce oyuncunun envanterine
         * sığıp sığmadığını kontrol et.
         */
        if (!canFitItem(
                player,
                item
        )) {

            player.sendMessage(
                    "§cEnvanterinizde yeterli boş yer yok."
            );

            return;
        }

        /*
         * ÖNCE storage kaydını sil.
         *
         * Böylece veritabanında kayıt hâlâ dururken
         * oyuncuya item verilmesi gibi bir durum oluşmaz.
         */
        boolean removed =
                storageManager.remove(
                        player.getUniqueId(),
                        slot
                );

        if (!removed) {

            player.sendMessage(
                    "§cTakviye depodan alınamadı."
            );

            return;
        }

        /*
         * Storage kaydı başarıyla silindikten sonra
         * itemi oyuncunun envanterine ekle.
         */
        Map<Integer, ItemStack> leftovers =
                player.getInventory()
                        .addItem(item);

        /*
         * Beklenmedik şekilde itemin tamamı
         * veya bir kısmı eklenemezse:
         *
         * 1. Envanteri eski hâline getir.
         * 2. Storage kaydını geri oluştur.
         */
        if (!leftovers.isEmpty()) {

            player.getInventory()
                    .setStorageContents(
                            previousContents
                    );

            boolean restored =
                    storageManager.add(
                            player.getUniqueId(),
                            storage.getBoosterId(),
                            slot
                    );

            if (!restored) {

                plugin.getLogger().severe(
                        "Booster storage kaydı geri oluşturulamadı. "
                                + "Oyuncu: "
                                + player.getUniqueId()
                                + ", Booster: "
                                + storage.getBoosterId()
                                + ", Slot: "
                                + slot
                );
            }

            player.sendMessage(
                    "§cTakviye depodan alınamadı."
            );

            return;
        }

        /*
         * GUI'den itemi kaldır.
         */
        event.getView()
                .getTopInventory()
                .setItem(
                        slot,
                        null
                );

        player.sendMessage(
                "§a"
                        + booster.getDisplayName()
                        + " §7envanterinize geri alındı."
        );
    }

    @EventHandler(
            priority = EventPriority.HIGHEST
    )
    public void onInventoryDrag(
            InventoryDragEvent event
    ) {

        if (!event.getView()
                .getTitle()
                .equals(TITLE)) {

            return;
        }

        event.setCancelled(true);
    }

    @EventHandler(
            priority = EventPriority.HIGHEST
    )
    public void onInventoryClose(
            InventoryCloseEvent event
    ) {

        if (!event.getView()
                .getTitle()
                .equals(TITLE)) {

            return;
        }

        /*
         * Storage verisi her işlemde
         * doğrudan SQLite'a kaydedildiği için
         * kapanışta ayrıca işlem yapılmasına gerek yok.
         */
    }

    private boolean canFitItem(
            Player player,
            ItemStack item
    ) {

        int remaining =
                item.getAmount();

        for (ItemStack inventoryItem :
                player.getInventory()
                        .getStorageContents()) {

            if (inventoryItem == null
                    || inventoryItem.getType()
                    == Material.AIR) {

                remaining -=
                        item.getMaxStackSize();

                if (remaining <= 0) {
                    return true;
                }

                continue;
            }

            if (!inventoryItem.isSimilar(item)) {
                continue;
            }

            int space =
                    inventoryItem.getMaxStackSize()
                            - inventoryItem.getAmount();

            remaining -= space;

            if (remaining <= 0) {
                return true;
            }
        }

        return false;
    }
}