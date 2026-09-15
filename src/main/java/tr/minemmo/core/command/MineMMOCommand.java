package tr.minemmo.core.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import tr.minemmo.core.MineMMOCore;
import tr.minemmo.core.booster.Booster;
import tr.minemmo.core.booster.BoosterItemManager;

import java.util.ArrayList;
import java.util.List;

public final class MineMMOCommand implements TabExecutor {

    private final MineMMOCore mineMMOCore;

    public MineMMOCommand(
            MineMMOCore mineMMOCore
    ) {

        this.mineMMOCore =
                mineMMOCore;
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

        if (args.length == 0) {

            sendHelp(sender);

            return true;
        }

        switch (args[0].toLowerCase()) {

            case "info":
                return handleInfo(sender);

            case "givebooster":
                return handleGiveBooster(
                        sender,
                        args
                );

            case "clearboosters":
                return handleClearBoosters(
                        sender
                );

            case "reload":
                return handleReload(
                        sender
                );

            case "mythiccheck":
                return handleMythicCheck(
                        sender
                );

            default:

                sendHelp(sender);

                return true;
        }
    }

    private void sendHelp(
            CommandSender sender
    ) {

        sender.sendMessage(
                Component.text(
                        "=== MineMMO-Core ==="
                ).color(
                        NamedTextColor.GOLD
                )
        );

        sender.sendMessage(
                Component.text(
                        "/minemmo info"
                ).color(
                        NamedTextColor.YELLOW
                )
        );

        sender.sendMessage(
                Component.text(
                        "/minemmo givebooster <booster>"
                ).color(
                        NamedTextColor.YELLOW
                )
        );

        sender.sendMessage(
                Component.text(
                        "/minemmo clearboosters"
                ).color(
                        NamedTextColor.YELLOW
                )
        );

        sender.sendMessage(
                Component.text(
                        "/minemmo reload"
                ).color(
                        NamedTextColor.YELLOW
                )
        );

        sender.sendMessage(
                Component.text(
                        "/minemmo mythiccheck"
                ).color(
                        NamedTextColor.YELLOW
                )
        );
    }

    private boolean handleInfo(
            CommandSender sender
    ) {

        sender.sendMessage(
                Component.text(
                        "MineMMO-Core 0.5.0"
                ).color(
                        NamedTextColor.GREEN
                )
        );

        sender.sendMessage(
                Component.text(
                        "Yüklü booster sayısı: "
                                + mineMMOCore
                                .getBoosterManager()
                                .getCount()
                ).color(
                        NamedTextColor.GRAY
                )
        );

        boolean mythicMobsEnabled =
                mineMMOCore
                        .getMythicMobsHook()
                        .isEnabled();

        sender.sendMessage(
                Component.text(
                        "MythicMobs entegrasyonu: "
                                + (
                                mythicMobsEnabled
                                        ? "Aktif"
                                        : "Pasif"
                        )
                ).color(
                        mythicMobsEnabled
                                ? NamedTextColor.GREEN
                                : NamedTextColor.RED
                )
        );

        sender.sendMessage(
                Component.text(
                        "Yüklü oyuncu booster verisi: "
                                + mineMMOCore
                                .getPlayerBoosterManager()
                                .getLoadedPlayerCount()
                ).color(
                        NamedTextColor.GRAY
                )
        );

        return true;
    }

    private boolean handleGiveBooster(
            CommandSender sender,
            String[] args
    ) {

        if (!(sender instanceof Player player)) {

            sender.sendMessage(
                    Component.text(
                            "Bu komut yalnızca oyuncular "
                                    + "tarafından kullanılabilir."
                    ).color(
                            NamedTextColor.RED
                    )
            );

            return true;
        }

        if (args.length < 2) {

            sender.sendMessage(
                    Component.text(
                            "Kullanım: "
                                    + "/minemmo givebooster <booster>"
                    ).color(
                            NamedTextColor.RED
                    )
            );

            return true;
        }

        String boosterId =
                args[1];

        Booster booster =
                mineMMOCore
                        .getBoosterManager()
                        .get(
                                boosterId
                        );

        if (booster == null) {

            sender.sendMessage(
                    Component.text(
                            "Bu isimde bir booster bulunamadı: "
                                    + boosterId
                    ).color(
                            NamedTextColor.RED
                    )
            );

            return true;
        }

        BoosterItemManager boosterItemManager =
                mineMMOCore
                        .getBoosterItemManager();

        ItemStack item =
                boosterItemManager.createItem(
                        booster
                );

        player.getInventory().addItem(
                item
        );

        sender.sendMessage(
                Component.text(
                        "Booster verildi: "
                                + booster.getId()
                ).color(
                        NamedTextColor.GREEN
                )
        );

        return true;
    }

    private boolean handleClearBoosters(
            CommandSender sender
    ) {

        if (!(sender instanceof Player player)) {

            sender.sendMessage(
                    Component.text(
                            "Bu komut yalnızca oyuncular "
                                    + "tarafından kullanılabilir."
                    ).color(
                            NamedTextColor.RED
                    )
            );

            return true;
        }

        boolean cleared =
                mineMMOCore
                        .getPlayerBoosterManager()
                        .clearAll(
                                player.getUniqueId()
                        );

        if (cleared) {

            sender.sendMessage(
                    Component.text(
                            "Aktif boosterlarınız temizlendi."
                    ).color(
                            NamedTextColor.GREEN
                    )
            );

        } else {

            sender.sendMessage(
                    Component.text(
                            "Temizlenecek aktif booster bulunamadı."
                    ).color(
                            NamedTextColor.YELLOW
                    )
            );
        }

        return true;
    }

    private boolean handleReload(
            CommandSender sender
    ) {

        try {

            mineMMOCore
                    .getBoosterManager()
                    .reload();

            sender.sendMessage(
                    Component.text(
                            "MineMMO-Core configleri "
                                    + "yeniden yüklendi."
                    ).color(
                            NamedTextColor.GREEN
                    )
            );

            sender.sendMessage(
                    Component.text(
                            "Yüklü booster sayısı: "
                                    + mineMMOCore
                                    .getBoosterManager()
                                    .getCount()
                    ).color(
                            NamedTextColor.GRAY
                    )
            );

        } catch (Exception exception) {

            sender.sendMessage(
                    Component.text(
                            "Config yeniden yüklenirken "
                                    + "hata oluştu."
                    ).color(
                            NamedTextColor.RED
                    )
            );

            mineMMOCore.getLogger().log(
                    java.util.logging.Level.SEVERE,
                    "Config reload hatası.",
                    exception
            );
        }

        return true;
    }

    private boolean handleMythicCheck(
            CommandSender sender
    ) {

        boolean enabled =
                mineMMOCore
                        .getMythicMobsHook()
                        .isEnabled();

        if (enabled) {

            sender.sendMessage(
                    Component.text(
                            "MythicMobs entegrasyonu aktif."
                    ).color(
                            NamedTextColor.GREEN
                    )
            );

            sender.sendMessage(
                    Component.text(
                            "MythicMob servisi hazır."
                    ).color(
                            NamedTextColor.GRAY
                    )
            );

        } else {

            sender.sendMessage(
                    Component.text(
                            "MythicMobs entegrasyonu aktif değil."
                    ).color(
                            NamedTextColor.RED
                    )
            );
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args
    ) {

        List<String> completions =
                new ArrayList<>();

        if (args.length == 1) {

            List<String> commands =
                    List.of(
                            "info",
                            "givebooster",
                            "clearboosters",
                            "reload",
                            "mythiccheck"
                    );

            String input =
                    args[0].toLowerCase();

            for (String option : commands) {

                if (option.startsWith(input)) {

                    completions.add(
                            option
                    );
                }
            }

            return completions;
        }

        if (args.length == 2
                && args[0].equalsIgnoreCase(
                "givebooster"
        )) {

            String input =
                    args[1].toLowerCase();

            for (Booster booster :
                    mineMMOCore
                            .getBoosterManager()
                            .getAll()) {

                String boosterId =
                        booster.getId();

                if (boosterId
                        .toLowerCase()
                        .startsWith(input)) {

                    completions.add(
                            boosterId
                    );
                }
            }
        }

        return completions;
    }
}