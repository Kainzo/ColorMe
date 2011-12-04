package de.xghostkillerx.colorme;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.TextWrapper;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

//Economy (Vault)
import net.milkbowl.vault.Vault;
import net.milkbowl.vault.economy.Economy;


/**
 * ColorMe for CraftBukkit/Bukkit
 * Handles some general stuff!
 * 
 * Refer to the forum thread:
 * 
 * Refer to the dev.bukkit.org page:
 * 
 *
 * @author xGhOsTkiLLeRx
 * @thanks to Valrix for the original ColorMe plugin!!
 * 
 */

public class ColorMe extends JavaPlugin {
	public static final Logger log = Logger.getLogger("Minecraft");
	private final ColorMePlayerListener playerListener = new ColorMePlayerListener(this);
	public Economy economy = null;
	public FileConfiguration config;
	public FileConfiguration colors;
	public File configFile;
	public File colorsFile;

	// Shutdown
	public void onDisable() {
		PluginDescriptionFile pdfFile = this.getDescription();
		log.info(pdfFile.getName() + " " + pdfFile.getVersion()	+ " has been disabled!");
	}

	// Start
	public void onEnable() {

		// Events
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvent(Event.Type.PLAYER_JOIN, playerListener, Event.Priority.Normal, this);

		// Player colors config
		colorsFile = new File(getDataFolder(), "players.color");
		if (!colorsFile.exists()) {
			colorsFile.getParentFile().mkdirs();
			copy(getResource("players.color"), colorsFile);
		}
		colors = YamlConfiguration.loadConfiguration(colorsFile);

		// Config
		configFile = new File(getDataFolder(), "config.yml");
		if (!configFile.exists()) {
			configFile.getParentFile().mkdirs();
			copy(getResource("config.yml"), configFile);
		}
		config = this.getConfig();
		loadConfig();

		// Message
		PluginDescriptionFile pdfFile = this.getDescription();
		log.info(pdfFile.getName() + " " + pdfFile.getVersion() + " is enabled!");

		// Check for Vault
		Plugin x = this.getServer().getPluginManager().getPlugin("Vault");
		if (x != null & x instanceof Vault) {
			// If Vault is enabled, load the economy
			log.info(String.format(pdfFile.getName() + " loaded Vault successfully"));
			setupEconomy();
		} else {
			// Else tell the admin about the missing of Vault
			log.warning(String.format("Vault was NOT found! Running without economy!"));
		}
	}

	// Loads the config at the start
	public void loadConfig() {
		config.options().header("For help please refer to  or ");
		config.addDefault("costs", 0);
		config.options().copyDefaults(true);
		saveConfig();
	}

	// Reloads the config via command /colorme reload
	public void loadConfigAgain() {
		try {
			config.load(configFile);
			saveConfig();
			colors.load(colorsFile);
			saveColors();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Try to save the color YML
	public void saveColors() {
		try {
			colors.save(colorsFile);
		} catch (Exception e) {
			log.warning("ColorMe failed to save the colors! Please report this!");
		}

	}

	// If no config is found, copy the default one(s)!
	private void copy(InputStream in, File file) {
		try {
			OutputStream out = new FileOutputStream(file);
			byte[] buf = new byte[1024];
			int len;
			while((len=in.read(buf))>0){
				out.write(buf,0,len);
			}
			out.close();
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Initialized to work with Vault
	private Boolean setupEconomy()
	{
		RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
		if (economyProvider != null) {
			economy = economyProvider.getProvider();
		}
		return (economy != null);
	}

	// Refer to ColorMeCommands
	public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
		ColorMeCommands cmd = new ColorMeCommands(this);
		return cmd.ColorMeCommand(sender, command, commandLabel, args);
	}

	/*
	 * Different Actions
	 */

	// Return the player's name color
	public String getColor(String name) {
		return (colors.contains(name.toLowerCase())) ? colors.getString(name.toLowerCase()) : "";
	}

	// Set player's color and update displayname if online
	public boolean setColor(String name, String color) {
		String oldColor = getColor(name);
		String newColor = findColor(color);
		// If the colors are the same return false
		if (oldColor.equalsIgnoreCase(newColor)) {
			return false;
		}
		// If the color is not suitable return fale
		if (newColor.equals(color)) {
			return false;
		}
		// Update the displayname to the new color
		else {
			colors.set(name.toLowerCase(), newColor);
			saveColors();
			if (getServer().getPlayerExact(name) != null) {
				Player player = getServer().getPlayerExact(name);
				player.setDisplayName(ChatColor.valueOf(newColor) + ChatColor.stripColor(player.getDisplayName()) + ChatColor.WHITE);
			}
			return true;
		}
	}

	// Iterate through colors to try and find a match (resource expensive)
	public String findColor(String color) {
		String col;
		for (int i = 0; i <= 15; i++) {
			col = ChatColor.getByCode(i).name();
			if (color.equalsIgnoreCase(col.toLowerCase().replace("_", ""))) return col;
		}
		return color;
	}

	// Check if a player has a color or not
	public boolean hasColor(String name) {
		if (colors.contains(name.toLowerCase())) {
			return (colors.getString(name.toLowerCase()).trim().length()>1) ? true : false;
		}
		return false;
	}

	// Removes a color if exists, otherwise returns false
	public boolean removeColor(String name) {
		name = name.toLowerCase();
		if (hasColor(name)) {
			colors.set(name, "");
			saveColors();
			return true;
		}
		return false;
	}

	//
	public boolean self(Player player, String n) {
		return (player.equals(getServer().getPlayerExact(n))) ? true : false;
	}

	// The list of colors
	public void list(CommandSender sender) {
		sender.sendMessage("Color List:");
		String color;
		String msg = "";
		for (int i = 0; i < ChatColor.values().length; i++) {
			color = ChatColor.getByCode(i).name();
			if (msg.length() == 0) {
				msg = ChatColor.valueOf(color) + color.toLowerCase().replace("_", "")+' ';
				continue;
			}
			msg += (i == ChatColor.values().length-1) ? ChatColor.valueOf(color)+color.toLowerCase().replace("_", "") : ChatColor.valueOf(color)+color.toLowerCase().replace("_", "")+' ';
			TextWrapper.wrapText(msg);
		}
		sender.sendMessage(msg);
	}
}
