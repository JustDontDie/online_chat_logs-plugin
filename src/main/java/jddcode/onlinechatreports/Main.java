package jddcode.onlinechatreports;

import com.zaxxer.hikari.HikariDataSource;
import jddcode.onlinechatreports.commands.ChatReportCommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;

public class Main extends JavaPlugin implements Listener
{
    private HikariDataSource hikari;

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) getDataFolder().mkdirs();
        File configFile = new File(getDataFolder(), "config.yml");

        if (!configFile.exists()) {
            saveDefaultConfig();
            getLogger().log(Level.INFO, "Since you're launching this for the first time, you'll need to edit the config to your liking before use. Disabling self...");
            Bukkit.getPluginManager().disablePlugin(Main.this);
            return;
        }

        String dbAddress = getConfig().getString("database-info.address");
        String dbPort = String.valueOf(getConfig().getInt("database-info.port"));
        String dbUsername = getConfig().getString("database-info.username");
        String dbPassword = getConfig().getString("database-info.password");
        String dbName = getConfig().getString("database-info.db-name");


        this.hikari = new HikariDataSource();
        this.hikari.setJdbcUrl("jdbc:mysql://" + dbAddress + ":" + dbPort + "/" + dbName + "?user=" + dbUsername + "&password=" + dbPassword);

        new BukkitRunnable() {
            @Override
            public void run() {
                try (Connection connection = hikari.getConnection())
                {
                    connection.createStatement().execute("CREATE TABLE IF NOT EXISTS chat_messages(`id` INT NOT NULL AUTO_INCREMENT, `uuid` VARCHAR(255), `name` VARCHAR(16), `message` VARCHAR(256), PRIMARY KEY (`id`));");
                    connection.createStatement().execute("CREATE TABLE IF NOT EXISTS chat_reports(`id` INT NOT NULL AUTO_INCREMENT, `uuid` VARCHAR(255), `name` VARCHAR(16), `messages` TEXT, PRIMARY KEY (`id`));");
                }
                catch (SQLException e)
                {
                    e.printStackTrace();
                    Bukkit.getPluginManager().disablePlugin(Main.this);
                    return;
                }
            }
        }.runTaskAsynchronously(this);

        getServer().getPluginManager().registerEvents(this, this);
        getCommand("chatreport").setExecutor(new ChatReportCommand(this));
    }

    @Override
    public void onDisable() {
        if (hikari != null) hikari.close();
    }

    public HikariDataSource getHikari()
    {
        return hikari;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Player who = e.getPlayer();
        String message = "[" + new SimpleDateFormat("yyyy-MM-dd KK:mm:ss aaa").format(new Date()) + "]" + who.getName() + ": " + e.getMessage();

        try (Connection connection = hikari.getConnection())
        {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO chat_messages (`uuid`, `name`, `message`) VALUES ('" + who.getUniqueId().toString() + "', '" + who.getName() + "', '" + message + "')");
            statement.execute();
        } catch (SQLException e1)
        {
            e1.printStackTrace();
        }
    }
}
