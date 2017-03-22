package me.egg82.tcpp;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

import javax.swing.Timer;

import org.bstats.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.PluginManager;

import me.egg82.tcpp.enums.PermissionsType;
import me.egg82.tcpp.enums.PluginServiceType;
import me.egg82.tcpp.util.DisguiseHelper;
import me.egg82.tcpp.util.LibsDisguisesHelper;
import me.egg82.tcpp.util.nulls.NullDisguiseHelper;
import net.gravitydevelopment.updater.Updater;
import net.gravitydevelopment.updater.Updater.UpdateResult;
import net.gravitydevelopment.updater.Updater.UpdateType;
import ninja.egg82.enums.ServiceType;
import ninja.egg82.patterns.ServiceLocator;
import ninja.egg82.plugin.BasePlugin;
import ninja.egg82.plugin.enums.SpigotRegType;
import ninja.egg82.plugin.utils.ReflectUtil;
import ninja.egg82.registry.Registry;
import ninja.egg82.registry.interfaces.IRegistry;
import ninja.egg82.utils.Util;

public class TrollCommandsPlusPlus extends BasePlugin {
	//vars
	private Timer updateTimer = null;
	
	private int numCommands = 0;
	private int numEvents = 0;
	private int numPermissions = 0;
	private int numTicks = 0;
	
	//constructor
	public TrollCommandsPlusPlus() {
		
	}
	
	//public
	public void onLoad() {
		super.onLoad();
		
		Object[] enums = null;
		
		enums = Util.getStaticFields(PluginServiceType.class);
		String[] services = Arrays.copyOf(enums, enums.length, String[].class);
		for (String s : services) {
			ServiceLocator.provideService(s, Registry.class);
		}
		
		PluginManager manager = getServer().getPluginManager();
		
		if (manager.getPlugin("LibsDisguises") != null) {
			if (manager.getPlugin("ProtocolLib") != null) {
				Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "[TrollCommands++] Enabling support for LibsDisguises.");
				ServiceLocator.provideService(PluginServiceType.DISGUISE_HELPER, LibsDisguisesHelper.class);
			} else {
				Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[TrollCommands++] LibsDisguises requires ProtocolLib to function, which was not found. The /control and /scare commands have been disabled.");
				ServiceLocator.provideService(PluginServiceType.DISGUISE_HELPER, NullDisguiseHelper.class);
			}
		} else if (manager.getPlugin("iDisguise") != null) {
			if (manager.getPlugin("ProtocolLib") != null) {
				Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "[TrollCommands++] Enabling support for iDisguise.");
				ServiceLocator.provideService(PluginServiceType.DISGUISE_HELPER, DisguiseHelper.class);
			} else {
				Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[TrollCommands++] iDisguise requires ProtocolLib to function, which was not found. The /control and /scare commands have been disabled.");
				ServiceLocator.provideService(PluginServiceType.DISGUISE_HELPER, NullDisguiseHelper.class);
			}
		} else {
			Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[TrollCommands++] Neither LibsDisguises nor iDisguise was found. The /control and /scare commands have been disabled.");
			ServiceLocator.provideService(PluginServiceType.DISGUISE_HELPER, NullDisguiseHelper.class);
		}
		
		updateTimer = new Timer(24 * 60 * 60 * 1000, onUpdateTimer);
	}
	
	public void onEnable() {
		super.onEnable();
		
		try {
			@SuppressWarnings("unused")
			Metrics m = new Metrics(this);
		} catch (Exception ex) {
			
		}
		
		numCommands = ReflectUtil.addCommandsFromPackage(commandHandler, "me.egg82.tcpp.commands");
		numEvents = ReflectUtil.addEventsFromPackage(eventListener, "me.egg82.tcpp.events");
		numPermissions = ReflectUtil.addPermissionsFromClass(permissionsManager, PermissionsType.class);
		numTicks = ReflectUtil.addTicksFromPackage(tickHandler, "me.egg82.tcpp.ticks");
		
		enableMessage(Bukkit.getConsoleSender());
		checkUpdate();
		updateTimer.setRepeats(true);
		updateTimer.start();
	}
	public void onDisable() {
		commandHandler.clearCommands();
		eventListener.clearEvents();
		permissionsManager.clearPermissions();
		tickHandler.clearTickCommands();
		
		disableMessage(Bukkit.getConsoleSender());
	}
	
	//private
	private ActionListener onUpdateTimer = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			checkUpdate();
		}
	};
	private void checkUpdate() {
		Updater updater = new Updater(this, 100359, getFile(), UpdateType.NO_DOWNLOAD, false);
		if (updater.getResult() == UpdateResult.UPDATE_AVAILABLE) {
			int[] latest = parseVersion(updater.getLatestName());
			int[] current = parseVersion(getDescription().getVersion());
			
			for (int i = 0; i < Math.min(latest.length, current.length); i++) {
				if (latest[i] < current[i]) {
					return;
				}
			}
			
			Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "--== " + ChatColor.YELLOW + "TrollCommands++ UPDATE AVAILABLE (Latest: " + versionToString(latest) + " Current: " + versionToString(current) + ") " + ChatColor.GREEN + " ==--");
		}
	}
	private int[] parseVersion(String name) {
		int versionIndex = Math.max(0, name.lastIndexOf('v') + 1);
		String versionString = name.substring(versionIndex);
		
		int firstDot = versionString.indexOf('.');
		int middleDot = versionString.indexOf('.', firstDot + 1);
		int lastDot = versionString.lastIndexOf('.');
		
		int first = Integer.parseInt(versionString.substring(0, firstDot));
		int middle = Integer.parseInt(versionString.substring(firstDot + 1, middleDot));
		int last = Integer.parseInt(versionString.substring(lastDot + 1));
		
		return new int[] {first, middle, last};
	}
	private String versionToString(int[] version) {
		String retVal = "";
		for (int i = 0; i < version.length; i++) {
			retVal += version[i] + ".";
		}
		retVal = retVal.substring(0, retVal.length() - 1);
		
		return retVal;
	}
	
	private void enableMessage(ConsoleCommandSender sender) {
		IRegistry initReg = (IRegistry) ServiceLocator.getService(ServiceType.INIT_REGISTRY);
		
		sender.sendMessage(ChatColor.AQUA + "  _______        _ _  _____                                          _                 ");
		sender.sendMessage(ChatColor.AQUA + " |__   __|      | | |/ ____|                                        | |      _     _   ");
		sender.sendMessage(ChatColor.AQUA + "    | |_ __ ___ | | | |     ___  _ __ ___  _ __ ___   __ _ _ __   __| |___ _| |_ _| |_ ");
		sender.sendMessage(ChatColor.AQUA + "    | | '__/ _ \\| | | |    / _ \\| '_ ` _ \\| '_ ` _ \\ / _` | '_ \\ / _` / __|_   _|_   _|");
		sender.sendMessage(ChatColor.AQUA + "    | | | | (_) | | | |___| (_) | | | | | | | | | | | (_| | | | | (_| \\__ \\ |_|   |_|  ");
		sender.sendMessage(ChatColor.AQUA + "    |_|_|  \\___/|_|_|\\_____\\___/|_| |_| |_|_| |_| |_|\\__,_|_| |_|\\__,_|___/            ");
		sender.sendMessage(ChatColor.GREEN + "[Version " + getDescription().getVersion() + "] " + ChatColor.RED + numCommands + " commands " + ChatColor.LIGHT_PURPLE + numEvents + " events " + ChatColor.WHITE + numPermissions + " permissions " + ChatColor.YELLOW + numTicks + " tick handlers");
		sender.sendMessage(ChatColor.WHITE + "[TrollCommands++] " + ChatColor.GRAY + "Attempting to load compatibility with Bukkit version " + initReg.getRegister(SpigotRegType.GAME_VERSION));
	}
	private void disableMessage(ConsoleCommandSender sender) {
		Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "--== " + ChatColor.LIGHT_PURPLE + "TrollCommands++ Disabled" + ChatColor.GREEN + " ==--");
	}
}