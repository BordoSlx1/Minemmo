
package tr.minemmo.core.booster;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class BoosterItemManager {

    private static final String BOOSTER_ID_KEY =
            "booster_id";

    private final NamespacedKey boosterIdKey;

    public BoosterItemManager(
            JavaPlugin plugin
    ) {

        this.boosterIdKey =
                new NamespacedKey(
                        plugin,
                        BOOSTER_ID_KEY
                );
    }

    public ItemStack createItem(
            Booster booster
    ) {

        if (booster == null) {

            throw new IllegalArgumentException(
                    "Booster null olamaz."
            );
        }

        ItemStack item =
                new ItemStack(
                        booster.getMaterial()
                );

        ItemMeta meta =
                item.getItemMeta();

        if (meta == null) {

            throw new IllegalStateException(
                    "Booster item'i için "
                            + "ItemMeta oluşturulamadı."
            );
        }

        /*
         * Display Name
         */

        meta.setDisplayName(
                booster.getDisplayName()
        );

        /*
         * Lore
         */

        List<String> lore =
                new ArrayList<>(
                        booster.getLore()
                );

        meta.setLore(
                lore
        );

        /*
         * Booster ID
         */

        PersistentDataContainer container =
                meta.getPersistentDataContainer();

        container.set(
                boosterIdKey,
                PersistentDataType.STRING,
                booster.getId()
        );

        item.setItemMeta(
                meta
        );

        return item;
    }

    public boolean isBoosterItem(
            ItemStack item
    ) {

        return getBoosterId(item) != null;
    }

    public String getBoosterId(
            ItemStack item
    ) {

        if (item == null
                || item.getType() == Material.AIR) {

            return null;
        }

        ItemMeta meta =
                item.getItemMeta();

        if (meta == null) {
            return null;
        }

        PersistentDataContainer container =
                meta.getPersistentDataContainer();

        return container.get(
                boosterIdKey,
                PersistentDataType.STRING
        );
    }

    public boolean isBoosterItem(
            ItemStack item,
            String boosterId
    ) {

        if (boosterId == null
                || boosterId.isBlank()) {

            return false;
        }

        String itemBoosterId =
                getBoosterId(item);

        return boosterId.equalsIgnoreCase(
                itemBoosterId
        );
    }
}

