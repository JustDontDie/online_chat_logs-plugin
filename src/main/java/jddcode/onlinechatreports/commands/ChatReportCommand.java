package jddcode.onlinechatreports.commands;

import jddcode.onlinechatreports.Main;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class ChatReportCommand implements CommandExecutor
{
    private Main main;

    public ChatReportCommand(Main main)
    {
        this.main = main;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (command.getName().equalsIgnoreCase("chatreport"))
        {
            if (!(sender instanceof Player))
            {
                sender.sendMessage("[Online Chat Report] This command can only be used by a player!");

                return true;
            }

            Player player = (Player) sender;

            if (args.length == 0)
            {
                player.sendMessage(ChatColor.RED + "I need a name!");

                return true;
            }

            if (args.length == 1) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        try (Connection connection = main.getHikari().getConnection())
                        {
                            //Find the amount of messages sent by player in database so we can get a exact limit amount
                            ResultSet resultSetAmount = connection.createStatement().executeQuery("SELECT count(*) FROM chat_messages WHERE name='" + args[0] + "';");

                            resultSetAmount.next();

                            int amount = resultSetAmount.getInt(1);
                            int limit = main.getConfig().getInt("limit");

                            if (amount == 0) {
                                player.sendMessage(ChatColor.RED + "That player does not exist!");

                                if (!resultSetAmount.getStatement().isClosed()) resultSetAmount.getStatement().closeOnCompletion();

                                return;
                            } else if (limit > amount) limit = amount;

                            if (!resultSetAmount.getStatement().isClosed()) resultSetAmount.getStatement().closeOnCompletion();

                            //Create report
                            ResultSet resultSet = connection.createStatement().executeQuery("SELECT * FROM chat_messages WHERE name='" + args[0] + "' ORDER BY `message` DESC LIMIT " + limit);
                            if (resultSet.next())
                            {
                                UUID randomUUID = UUID.randomUUID();

                                StringBuilder messages = new StringBuilder();
                                resultSet.first();

                                while (!resultSet.isAfterLast())
                                {
                                    messages.append(resultSet.getString(4)).append(resultSet.isLast() ? "" : "\n");

                                    resultSet.next();
                                }

                                connection.createStatement().execute("INSERT INTO chat_reports(uuid, name, messages) VALUES ('" + randomUUID.toString() + "', '" + args[0] + "', '" + messages.toString() + "');");

                                player.sendMessage(ChatColor.GREEN + "Created report! You can view it here:");

                                TextComponent message = new TextComponent("Link to chat report");
                                message.setColor(net.md_5.bungee.api.ChatColor.DARK_GREEN);
                                message.setUnderlined(true);
                                message.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, main.getConfig().getString("website-url") + "/report/report.php?uuid=" + randomUUID.toString()));

                                player.spigot().sendMessage(message);

                                if (!resultSet.getStatement().isClosed()) resultSet.getStatement().closeOnCompletion();
                            }
                        }
                        catch (SQLException e)
                        {
                            e.printStackTrace();
                            player.sendMessage(ChatColor.RED + "Error creating report! Please report this to a dev ASAP!");
                        }
                    }
                }.runTaskAsynchronously(this.main);
                return true;
            }

            return true;
        }
        return true;
    }
}
