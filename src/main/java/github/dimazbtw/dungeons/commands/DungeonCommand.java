package github.dimazbtw.dungeons.commands;

import github.dimazbtw.dungeons.Main;
import github.dimazbtw.dungeons.models.Dungeon;
import github.dimazbtw.dungeons.models.DungeonSession;
import github.dimazbtw.dungeons.models.PlayerData;
import me.saiintbrisson.minecraft.command.annotation.Command;
import me.saiintbrisson.minecraft.command.annotation.Optional;
import me.saiintbrisson.minecraft.command.command.Context;
import me.saiintbrisson.minecraft.command.target.CommandTarget;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class DungeonCommand {

    private final Main plugin;

    public DungeonCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Command(
        name = "dungeon",
        aliases = {"dungeons", "dg"},
        description = "Main dungeon command",
        target = CommandTarget.PLAYER
    )
    public void dungeonCommand(Context<Player> context) {
        plugin.getMenuManager().openMainMenu(context.getSender());
    }

    @Command(
        name = "dungeon.menu",
        aliases = {"m"},
        description = "Open dungeon menu",
        target = CommandTarget.PLAYER
    )
    public void menuCommand(Context<Player> context) {
        plugin.getMenuManager().openMainMenu(context.getSender());
    }

    @Command(
        name = "dungeon.play",
        aliases = {"p", "list"},
        description = "Open dungeon selection",
        target = CommandTarget.PLAYER
    )
    public void playCommand(Context<Player> context) {
        plugin.getMenuManager().openDungeonListMenu(context.getSender());
    }

    @Command(
        name = "dungeon.join",
        aliases = {"j", "enter"},
        description = "Join a dungeon",
        usage = "<dungeon>",
        target = CommandTarget.PLAYER
    )
    public void joinCommand(Context<Player> context, String dungeonId) {
        Player player = context.getSender();

        Dungeon dungeon = plugin.getDungeonManager().getDungeon(dungeonId);
        if (dungeon == null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("dungeon", dungeonId);
            plugin.getMessageManager().sendMessage(player, "dungeon-not-found", placeholders);
            return;
        }

        DungeonSession session = plugin.getSessionManager().getSessionByDungeon(dungeonId);
        if (session == null) {
            session = plugin.getSessionManager().createSession(dungeon);
        }

        plugin.getSessionManager().joinSession(player, session);
    }

    @Command(
        name = "dungeon.leave",
        aliases = {"l", "exit", "quit"},
        description = "Leave current dungeon",
        target = CommandTarget.PLAYER
    )
    public void leaveCommand(Context<Player> context) {
        Player player = context.getSender();

        if (!plugin.getSessionManager().isInDungeon(player)) {
            plugin.getMessageManager().sendMessage(player, "not-in-dungeon");
            return;
        }

        plugin.getSessionManager().leaveSession(player);
    }

    @Command(
        name = "dungeon.stats",
        aliases = {"s", "statistics"},
        description = "View your dungeon stats",
        target = CommandTarget.PLAYER
    )
    public void statsCommand(Context<Player> context) {
        Player player = context.getSender();
        PlayerData data = plugin.getPlayerDataManager().getData(player.getUniqueId());

        if (data == null) {
            plugin.getMessageManager().sendMessage(player, "error-occurred");
            return;
        }

        player.sendMessage(plugin.getMessageManager().getMessage("stats-header"));

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("value", String.valueOf(data.getDungeonsCompleted()));
        player.sendMessage(plugin.getMessageManager().getMessage("stats-dungeons-completed", placeholders));

        placeholders.put("value", String.valueOf(data.getMobsKilled()));
        player.sendMessage(plugin.getMessageManager().getMessage("stats-mobs-killed", placeholders));

        placeholders.put("value", String.valueOf(data.getDeaths()));
        player.sendMessage(plugin.getMessageManager().getMessage("stats-deaths", placeholders));

        placeholders.put("value", data.getFormattedPlayTime());
        player.sendMessage(plugin.getMessageManager().getMessage("stats-playtime", placeholders));
    }

    @Command(
        name = "dungeon.rewards",
        aliases = {"r", "reward"},
        description = "Open rewards menu",
        target = CommandTarget.PLAYER
    )
    public void rewardsCommand(Context<Player> context) {
        plugin.getMenuManager().openRewardsMenu(context.getSender());
    }

    @Command(
        name = "dungeon.enchants",
        aliases = {"e", "upgrades", "gear"},
        description = "Open enchants menu",
        target = CommandTarget.PLAYER
    )
    public void enchantsCommand(Context<Player> context) {
        plugin.getMenuManager().openEnchantsMenu(context.getSender());
    }

    @Command(
        name = "dungeon.perks",
        aliases = {"stats", "attributes", "skills"},
        description = "Open perks/attributes menu",
        target = CommandTarget.PLAYER
    )
    public void perksCommand(Context<Player> context) {
        plugin.getMenuManager().openPerksMenu(context.getSender());
    }

    @Command(
        name = "dungeon.equipment",
        aliases = {"equip", "armor", "gear"},
        description = "Open equipment menu",
        target = CommandTarget.PLAYER
    )
    public void equipmentCommand(Context<Player> context) {
        plugin.getMenuManager().openEquipmentMenu(context.getSender());
    }

    @Command(
        name = "dungeon.help",
        aliases = {"h", "?"},
        description = "Show help message"
    )
    public void helpCommand(Context<CommandSender> context) {
        CommandSender sender = context.getSender();

        sender.sendMessage(plugin.getMessageManager().getMessage("help-header"));
        sender.sendMessage(plugin.getMessageManager().getMessage("help-join"));
        sender.sendMessage(plugin.getMessageManager().getMessage("help-leave"));
        sender.sendMessage(plugin.getMessageManager().getMessage("help-list"));
        sender.sendMessage(plugin.getMessageManager().getMessage("help-stats"));
        sender.sendMessage(plugin.getMessageManager().getMessage("help-menu"));

        if (sender.hasPermission("dungeons.admin")) {
            sender.sendMessage("");
            sender.sendMessage(plugin.getMessageManager().getMessage("admin-help-header"));
            sender.sendMessage(plugin.getMessageManager().getMessage("admin-help-reload"));
            sender.sendMessage(plugin.getMessageManager().getMessage("admin-help-setspawn"));
            sender.sendMessage(plugin.getMessageManager().getMessage("admin-help-forcestart"));
            sender.sendMessage(plugin.getMessageManager().getMessage("admin-help-forcestop"));
        }
    }

    // Admin commands

    @Command(
        name = "dungeon.reload",
        aliases = {"rl"},
        description = "Reload plugin configuration",
        permission = "dungeons.admin"
    )
    public void reloadCommand(Context<CommandSender> context) {
        plugin.reload();
        plugin.getMessageManager().sendMessage((Player) context.getSender(), "reload-success");
    }

    @Command(
        name = "dungeon.forcestart",
        aliases = {"fs", "start"},
        description = "Force start current dungeon",
        permission = "dungeons.admin",
        target = CommandTarget.PLAYER
    )
    public void forceStartCommand(Context<Player> context) {
        Player player = context.getSender();

        DungeonSession session = plugin.getSessionManager().getPlayerSession(player);
        if (session == null) {
            plugin.getMessageManager().sendMessage(player, "not-in-dungeon");
            return;
        }

        if (session.isInProgress() || session.isBossRound()) {
            player.sendMessage("§cDungeon already in progress!");
            return;
        }

        session.cancelCountdownTask();
        session.setState(DungeonSession.SessionState.IN_PROGRESS);
        session.setStartTime(System.currentTimeMillis());
        session.setCurrentRound(0);

        String title = plugin.getMessageManager().getTitleMain("dungeon-start");
        String subtitle = plugin.getMessageManager().getTitleSub("dungeon-start");

        for (Player p : session.getOnlinePlayers()) {
            p.sendTitle(title, subtitle, 10, 40, 10);
            plugin.getScoreboardManager().applyInGameScoreboard(p, session);
        }

        session.broadcast(plugin.getMessageManager().getMessageWithPrefix("dungeon-started"));

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> 
            plugin.getSessionManager().startNextRound(session), 40L);
    }

    @Command(
        name = "dungeon.forcestop",
        aliases = {"stop", "end"},
        description = "Force stop current dungeon",
        permission = "dungeons.admin",
        target = CommandTarget.PLAYER
    )
    public void forceStopCommand(Context<Player> context) {
        Player player = context.getSender();

        DungeonSession session = plugin.getSessionManager().getPlayerSession(player);
        if (session == null) {
            plugin.getMessageManager().sendMessage(player, "not-in-dungeon");
            return;
        }

        plugin.getSessionManager().endSession(session, false);
        player.sendMessage("§cDungeon force stopped!");
    }

    @Command(
        name = "dungeon.setspawn",
        aliases = {"ss"},
        description = "Set spawn location for dungeon",
        usage = "<dungeon> <type>",
        permission = "dungeons.admin",
        target = CommandTarget.PLAYER
    )
    public void setSpawnCommand(Context<Player> context, String dungeonId, @Optional String type) {
        Player player = context.getSender();

        if (type == null) {
            plugin.getMessageManager().sendMessage(player, "spawn-types");
            return;
        }

        Dungeon dungeon = plugin.getDungeonManager().getDungeon(dungeonId);
        if (dungeon == null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("dungeon", dungeonId);
            plugin.getMessageManager().sendMessage(player, "dungeon-not-found", placeholders);
            return;
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("type", type);
        placeholders.put("dungeon", dungeonId);

        switch (type.toLowerCase()) {
            case "entry" -> {
                dungeon.addEntryLocation(player.getLocation());
                plugin.getDungeonManager().saveLocation(dungeonId, "entry", player.getLocation());
                plugin.getMessageManager().sendMessage(player, "spawn-set", placeholders);
            }
            case "exit" -> {
                dungeon.setExitLocation(player.getLocation());
                plugin.getDungeonManager().saveLocation(dungeonId, "exit", player.getLocation());
                plugin.getMessageManager().sendMessage(player, "spawn-set", placeholders);
            }
            case "mobs-spawn" -> {
                dungeon.addMobSpawnLocation(player.getLocation());
                plugin.getDungeonManager().saveLocation(dungeonId, "mobs-spawn", player.getLocation());
                plugin.getMessageManager().sendMessage(player, "spawn-set", placeholders);
            }
            case "boss-spawn" -> {
                dungeon.setBossSpawnLocation(player.getLocation());
                plugin.getDungeonManager().saveLocation(dungeonId, "boss-spawn", player.getLocation());
                plugin.getMessageManager().sendMessage(player, "spawn-set", placeholders);
            }
            case "clear-entry" -> {
                plugin.getDungeonManager().clearLocations(dungeonId, "entry");
                player.sendMessage("§aLocalizações de entrada limpas!");
            }
            case "clear-mobs" -> {
                plugin.getDungeonManager().clearLocations(dungeonId, "mobs-spawn");
                player.sendMessage("§aLocalizações de spawn de mobs limpas!");
            }
            default -> plugin.getMessageManager().sendMessage(player, "invalid-spawn-type");
        }
    }

    @Command(
        name = "dungeon.givepoints",
        aliases = {"gp"},
        description = "Give dungeon points to a player",
        usage = "<player> <amount>",
        permission = "dungeons.admin"
    )
    public void givePointsCommand(Context<CommandSender> context, Player target, int amount) {
        PlayerData data = plugin.getPlayerDataManager().getData(target.getUniqueId());
        if (data != null) {
            data.addPoints(amount);
            context.getSender().sendMessage("§aGave " + amount + " points to " + target.getName());
            target.sendMessage("§aYou received " + amount + " dungeon points!");
        }
    }

    @Command(
        name = "dungeon.ranking",
        aliases = {"rank", "top", "leaderboard"},
        description = "Open ranking menu",
        target = CommandTarget.PLAYER
    )
    public void rankingCommand(Context<Player> context) {
        plugin.getMenuManager().openRankingMenu(context.getSender());
    }
}
