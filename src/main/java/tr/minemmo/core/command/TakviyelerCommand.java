package tr.minemmo.core.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tr.minemmo.core.MineMMOCore;

public final class TakviyelerCommand
        implements CommandExecutor {

    private final MineMMOCore plugin;

    public TakviyelerCommand(
            MineMMOCore plugin
    ) {

        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

        if (!(sender instanceof Player player)) {

            sender.sendMessage(
                    "§cBu komut yalnızca oyuncular "
                            + "tarafından kullanılabilir."
            );

            return true;
        }

        plugin.getBoosterStorageListener()
                .open(player);

        return true;
    }
}