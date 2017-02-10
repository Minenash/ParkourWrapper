package com.minenash.ParkourWrapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.ChatColor;


public class ParkourWrapper extends JavaPlugin implements Listener {

	FileConfiguration data = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "data.yml"));
	FileConfiguration score = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "score.yml"));
	ArrayList<ParkourPlayer>  pps = new ArrayList<ParkourPlayer>();
	ArrayList<String> parkourList = new ArrayList<String>();
	ArrayList<String> leaderboard = new ArrayList<String>();
	List<String> restartBlocks = new ArrayList<String>();
	String prefix = ChatColor.translateAlternateColorCodes('&', getConfig().getString("plugin-prefix"));
	
	Server s;

	@Override
	public void onEnable() {
		getConfig().options().copyDefaults(true);
		saveConfig();
		addPermissions();
		saveFC();
		this.getServer().getPluginManager().registerEvents(this, this);
		timer();
		makeLeaderboard();
		makeRestartBlocksList();
		
	}

	@Override
	public void onDisable() {
		saveFC();
	}

	@SuppressWarnings("deprecation")
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (sender instanceof Player) {
			Player player = (Player) sender;

			// parkour
			if (cmd.getName().equalsIgnoreCase("parkour")) {

				// parkour [nothing]
				if (args.length == 0) {
					if (player.hasPermission("parkour.play")) {
						if(data.getString("spawnW") == null){
							player.sendMessage(prefix + ChatColor.RED + "The Parkour Spawn has not been set.");
							return true;
						}
						player.teleport(new Location(getServer().getWorld(data.getString("spawnW")),
								data.getInt("spawnX") + 0.5,
								data.getInt("spawnY") + 0.0,
								data.getInt("spawnZ") + 0.5,
								(float)data.getDouble("spawnYw"),
								(float)data.getDouble("spawnP")));
						player.sendMessage(prefix + ChatColor.YELLOW + "Welcome to Parkour!");
						return true;
					}
					player.sendMessage(prefix + ChatColor.RED + "You may not play Parkour!");
					return true;
				}

				// parkour new <name> <x1> <y1> <z1> <x2> <y2> <z2>
				if (args[0].equalsIgnoreCase("new")) {
					if (player.hasPermission("parkour.create")) {
						if(args.length < 8){
							player.sendMessage(ChatColor.RED + "Syntax: /parkour new <name> <x1> <y1> <z1> <x2> <y2> <z2>");
							return true;
						}
						if(createParkour(player, args))
						player.sendMessage(prefix + 
								ChatColor.GREEN + "You made a parkour that starts at, " + args[2] + " " + args[3] + " "
										+ args[4] + ", and ends at, " + args[5] + " " + args[6] + " " + args[7]);
						saveFC();
						return true;
					}
					player.sendMessage(ChatColor.RED + "You don't have the nessary permission.");
					return true;

				}

				// parkour delete <NAME>
				if (args[0].equalsIgnoreCase("delete")) {
					if (player.hasPermission("parkour.delete")) {
						if(args.length < 2){
							player.sendMessage(ChatColor.RED + "Syntax: /parkour delete <name>");
							return true;
						}
						data.set(args[1], null);
						player.sendMessage(prefix + ChatColor.GREEN + "Parkour " + args[1] + " has been deleted.");
						saveFC();
						return true;
					}
					player.sendMessage(ChatColor.RED + "You don't have the nessary permission.");
					return true;
				}

				// parkour edit <NAME> <name|start|end> <name|<x> <y> <z>>
				if (args[0].equalsIgnoreCase("edit")) {
					if (!player.hasPermission("parkour.edit")) {
						player.sendMessage(ChatColor.RED + "You don't have the nessary permission.");
						return true;
					}
					if(args.length < 3){
						player.sendMessage(ChatColor.RED + "Syntax: /parkour edit <name> <name|start|end>");
						return true;
					}
					// parkour edit(0) NAME(1) name(2) newname(3)
					if (args[2].equalsIgnoreCase("name")) {
						if(args.length < 4){
							player.sendMessage(ChatColor.RED + "Syntax: /parkour edit <name> name <new-name>");
							return true;
						}
						String[] newArgs = { "", args[3],
								data.getString(args[1] + ".start-cords.x"),
								data.getString(args[1] + ".start-cords.y"),
								data.getString(args[1] + ".start-cords.z"),
								data.getString(args[1] + ".end-cords.x"),
								data.getString(args[1] + ".end-cords.y"),
								data.getString(args[1] + ".end-cords.z"), 
						};
						data.set(args[1], null);
						createParkour(player, newArgs);

					}
					// parkour edit(0) NAME(1) start(2) X(3) Y(4) Z(5)
					if (args[2].equalsIgnoreCase("start")) {
						if(args.length < 6){
							player.sendMessage(ChatColor.RED + "Syntax: /parkour edit <name> start <x> <y> <z>");
							return true;
						}
						String[] newArgs = { "", args[1], args[3], args[4], args[5],
								data.getString(args[1] + ".end-cords.x"),
								data.getString(args[1] + ".end-cords.y"),
								data.getString(args[1] + ".end-cords.z"), 
								};
						data.set(args[1], null);
						createParkour(player, newArgs);
					}
					if (args[2].equalsIgnoreCase("end")) {
						String[] newArgs = { "", args[1], 
								data.getString(args[1] + ".start-cords.x"),
								data.getString(args[1] + ".start-cords.y"),
								data.getString(args[1] + ".start-cords.z"),
								args[3], args[4], args[5]
								};
						data.set(args[1], null);
						createParkour(player, newArgs);
					}
					saveFC();
					return true;
				}

				// parkour leaderboard/lb [NAME] [me|PLAYER_NAME]
				if (args[0].equalsIgnoreCase("leaderboard") || args[0].equalsIgnoreCase("lb")) {
					if (!player.hasPermission("parkour.leaderboard")) {
						player.sendMessage(ChatColor.RED + "You don't have the nessary permission.");
						return true;
					}
					if(args.length < 2){
						player.sendMessage(ChatColor.RED + "Syntax: /parkour leaderboard [name] [me|player_name]");
						return true;
					}
					if(args[1].equalsIgnoreCase("clear")){
						if(args.length < 3){
							player.sendMessage(ChatColor.RED + "Syntax: /parkour leaderboard clear <me|player_name|all> [name]");
							return true;
						}
						if(args[2].equalsIgnoreCase("all")){
							if(!player.hasPermission("parkour.clear.all")){
								player.sendMessage(ChatColor.RED + "You don't have the nessary permission.");
								return true;
							}
							if( args.length == 4){
								for(String key : score.getKeys(true)){
									if(key.contains(args[3])){
										score.set(key, null);
									}
									player.sendMessage(prefix + ChatColor.DARK_AQUA + args[3] + ChatColor.GREEN
											+ "'s Leaderboard has been reset.");
									reload();
								}
							}
							for(String key : score.getKeys(true)){
								score.set(key, null);
							}
							player.sendMessage(prefix + ChatColor.GREEN + "All LeaderBoards have been wiped clean");
							reload();
							return true;
						}
						if(args.length < 4){
							if(args[2].equals("me")){
								if(!player.hasPermission("parkour.clear.me")){
									player.sendMessage(ChatColor.RED + "You don't have the nessary permission.");
									return true;
								}
								for(String key : score.getKeys(false)){
									if(key.contains(player.getUniqueId().toString())){score.set(key, null);}
								}
								player.sendMessage(prefix + ChatColor.DARK_AQUA + player.getName() + ChatColor.GREEN
													+ " has been cleared from all Leaderboards.");
								reload();
								return true;
							}
							if(!player.hasPermission("parkour.clear.other")){
								player.sendMessage(ChatColor.RED + "You don't have the nessary permission.");
								return true;
							}
							for(String key : score.getKeys(false)){
								if(key.contains(UUID.fromString(args[2]).toString())){score.set(key, null);}
							}
							player.sendMessage(prefix + ChatColor.DARK_AQUA + args[2] + ChatColor.GREEN
												+ " has been cleared from all Leaderboards.");
							reload();
							return true;
							
							
						}
						else{
							if(args[2].equals("me")){
								if(!player.hasPermission("parkour.clear.me")){
									player.sendMessage(ChatColor.RED + "You don't have the nessary permission.");
									return true;
								}
								score.set(args[3] + "-" + player.getUniqueId(), null);
								player.sendMessage(prefix + ChatColor.DARK_AQUA + player.getName() + ChatColor.GREEN 
										+ " has been removed from " + ChatColor.RED + args[3] + ChatColor.GREEN + ".");
								reload();
								return true;
							}
							if(!player.hasPermission("parkour.clear.other")){
								player.sendMessage(ChatColor.RED + "You don't have the nessary permission.");
								return true;
							}
							score.set(args[3] + "-" + UUID.fromString(args[2]), null);
							player.sendMessage(prefix + ChatColor.DARK_AQUA + args[2] + ChatColor.GREEN 
									+ " has been removed from " + ChatColor.RED + args[3] + ChatColor.GREEN + ".");
							reload();
							return true;
						}
					}
					if(args.length == 2){
						ArrayList<String> lb = new ArrayList<String>();
						String name = args[1];
						for(String l : leaderboard){if(l.contains(name)){lb.add(l);}}
						player.sendMessage(ChatColor.GOLD+"Leaderboard for "+ChatColor.RED + name + ChatColor.GOLD+":");
						for(int i = 0; i < 10 && i < lb.size() ; i++){
							String player_name = lb.get(i).substring(lb.get(i).indexOf("$")+1);
							player.sendMessage(ChatColor.GREEN + "" + (i+1) + ": " + ChatColor.DARK_AQUA +
												player_name + ChatColor.GREEN + " - " + ChatColor.YELLOW + 
												score.getDouble(args[1] + "$" + 
												getServer().getOfflinePlayer(player_name).getUniqueId().toString()) + 
												ChatColor.GREEN + " secs");
						}
						return true;
					}
					if (args.length == 0) {
						// To be written
					} else if (args[2].equalsIgnoreCase("me")) {
						player.sendMessage(prefix + ChatColor.GREEN + "Your score for " + ChatColor.RED + args[1]
										+ ChatColor.GREEN + " is " + ChatColor.YELLOW + score.getDouble(args[1] + "$"
										+ player.getUniqueId()) + ChatColor.GREEN + " secs.");
					} else if (args[2] != null) {
						player.sendMessage(prefix + ChatColor.GREEN + args[2] + "'s score for " + ChatColor.RED + args[1]
								+ ChatColor.GREEN + " is " + ChatColor.YELLOW + score.getString(args[1] + "$" +args[2]) 
								+ ChatColor.GREEN + " secs.");
					}
					return true;
				}
				
				//parkour help
				if (args[0].equalsIgnoreCase("help")){
					if(!player.hasPermission("parkour.help")){
						player.sendMessage(ChatColor.RED + "You don't have the nessary permission.");
						return true;
					}
					if(player.hasPermission("parkour.play")) player.sendMessage(ChatColor.GOLD + "Commands:\n"
									+ ChatColor.YELLOW + " /parkour: " + ChatColor.WHITE + "Teleport to parkour");
					if(player.hasPermission("parkour.create")) player.sendMessage(ChatColor.YELLOW + " /parkour new: "
																   + ChatColor.WHITE + "Create a Parkour");
					if(player.hasPermission("parkour.edit")) player.sendMessage(ChatColor.YELLOW + " /parkour edit: "
																   + ChatColor.WHITE + "Edit a Parkour");
					if(player.hasPermission("parkour.delete")) player.sendMessage(ChatColor.YELLOW + " /parkour delete: "
																   + ChatColor.WHITE + "Delete a Parkour");
					if(player.hasPermission("parkour.list")) player.sendMessage(ChatColor.YELLOW + " /parkour list: "
																   + ChatColor.WHITE + "List all Parkours");
					if(player.hasPermission("parkour.leave")) player.sendMessage(ChatColor.YELLOW + " /parkour leave: "
																   + ChatColor.WHITE + "Leave Current Parkour");
					if(player.hasPermission("parkour.leaderboard")) player.sendMessage(ChatColor.YELLOW + " /parkour leaderboard: "
																   + ChatColor.WHITE + "Show the leaderboard");
					if(player.hasPermission("parkour.clear.me")) player.sendMessage(ChatColor.YELLOW + " /parkour leaderboard clear: " 
																   + ChatColor.WHITE + "Clear the leaderboard");
					if(player.hasPermission("parkour.help")) player.sendMessage(ChatColor.YELLOW + " /parkour help: " 
																   + ChatColor.WHITE + "Show this menu");
					if(player.hasPermission("parkour.reload")) player.sendMessage(ChatColor.YELLOW + " /parkour reload: "
																   + ChatColor.WHITE + "Reload Plugin");
					if(player.hasPermission("parkour.setspawn")) player.sendMessage(ChatColor.YELLOW + " /parkour setspawn: "
																   + ChatColor.WHITE + "Sets Spawn");
					return true;
				}
				if (args[0].equalsIgnoreCase("list")){
					if(!player.hasPermission("parkour,list")){
						player.sendMessage(ChatColor.RED + "You don't have the nessary permission.");
						return true;
					}
					
					player.sendMessage(list());
					return true;
				}
				if (args[0].equalsIgnoreCase("reload")){
					if(player.hasPermission("parkour.help")){
						reload();
						player.sendMessage(prefix + ChatColor.RED + "Plugin Reloaded");
						return true;
					}
					player.sendMessage(ChatColor.RED + "You don't have the nessary permission.");
					return true;
				}
				if (args[0].equalsIgnoreCase("leave")){
					if(!player.hasPermission("parkour.leave")){
						player.sendMessage(ChatColor.RED + "You don't have the nessary permission.");
						return true;
					}
					for(String p : parkourList){
						end(p, player, false);
					}
					player.sendMessage(prefix + ChatColor.GREEN + "You have left all parkours.");
					return true;
				}
				if (args[0].equalsIgnoreCase("setspawn")){
					if(!player.hasPermission("parkour.setspawn")){
						player.sendMessage(ChatColor.RED + "You don't have the nessary permission.");
						return true;
					}
					Location lo = player.getLocation();
					data.set("spawnX", lo.getBlockX());
					data.set("spawnY", lo.getBlockY());
					data.set("spawnZ", lo.getBlockZ());
					data.set("spawnYw", lo.getYaw());
					data.set("spawnP", lo.getPitch());
					data.set("spawnW", lo.getWorld().getName());
					player.sendMessage(prefix + ChatColor.GREEN + "Parkour Spawn Location has been set.");
					return true;
				}
				player.sendMessage(ChatColor.RED + "Unknown Subcommand, do /parkour help for a list of commands.");
				return true;
			}
		}
	return false;
	}

	public void addPermissions() {
		PluginManager pm = getServer().getPluginManager();
		pm.addPermission(new Permission("parkour.play"));
		pm.addPermission(new Permission("parkour.create"));
		pm.addPermission(new Permission("parkour.edit"));
		pm.addPermission(new Permission("parkour.delete"));
		pm.addPermission(new Permission("parkour.leaderboard"));
		pm.addPermission(new Permission("parkour.help"));
		pm.addPermission(new Permission("parkour.list"));
		pm.addPermission(new Permission("parkour.reload"));
		pm.addPermission(new Permission("parkour.leave"));
		pm.addPermission(new Permission("parkour.clear.me"));
		pm.addPermission(new Permission("parkour.clear.other"));
		pm.addPermission(new Permission("parkour.clear.all"));
		pm.addPermission(new Permission("parkour.setspawn"));
	}

	public void saveFC() {
		try {
			data.save(new File(getDataFolder(), "data.yml"));
			score.save(new File(getDataFolder(), "score.yml"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		makeParkourList();
	}

	public boolean createParkour(Player player, String[] args) {
		if(!(StringUtils.isNumeric(args[2].replaceFirst("-", "")) && 
				StringUtils.isNumeric(args[3].replaceFirst("-", "")) &&
				StringUtils.isNumeric(args[4].replaceFirst("-", "")) && 
				StringUtils.isNumeric(args[5].replaceFirst("-", "")) &&
				StringUtils.isNumeric(args[6].replaceFirst("-", "")) && 
				StringUtils.isNumeric(args[7].replaceFirst("-", "")))){
			player.sendMessage(prefix + ChatColor.RED + "Cords MUST be numeric.");
			return false;
		}
		data.createSection(args[1]);
		data.set(args[1] + ".name", args[1]);
		data.set(args[1] + ".world", player.getWorld().getName());
		data.set(args[1] + ".start-cords.x", args[2]);
		data.set(args[1] + ".start-cords.y", args[3]);
		data.set(args[1] + ".start-cords.z", args[4]);
		data.set(args[1] + ".end-cords.x", args[5]);
		data.set(args[1] + ".end-cords.y", args[6]);
		data.set(args[1] + ".end-cords.z", args[7]);
		player.getWorld().getBlockAt(Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]))
				.setType(Material.getMaterial(getConfig().getString("start-block")));
		player.getWorld().getBlockAt(Integer.parseInt(args[5]), Integer.parseInt(args[6]), Integer.parseInt(args[7]))
				.setType(Material.getMaterial(getConfig().getString("end-block")));
		return true;
	}
	public String list(){
		String msg = "";
		msg += ChatColor.GOLD + "Parkours:\n";
		for( String key : data.getKeys(true)){
			if(!key.contains(".")){
				msg += ChatColor.GREEN + key + ": " + ChatColor.WHITE;
			}
			else if(key.contains("world")){
				msg += "[" + data.getString(key) + "] ";
			}
			else if(key.contains("x") || key.contains("y")){
				msg += data.getString(key) + ", ";
			}
			else if(key.contains("start-cords.z")){
				msg += data.getString(key) + " ; ";
			}
			else if(key.contains("end-cords.z")){
				msg += data.getString(key) + "\n";
			}
		}
		return msg;
	}

	public void makeParkourList() {
		ArrayList<String> blank = new ArrayList<String>();
		parkourList = blank;
		for( String key : data.getKeys(false)){
			if(!key.contains("spawn"))
				parkourList.add(key);
		}
	}
	public void makeLeaderboard(){
		ArrayList<String> blank = new ArrayList<String>();
		for(String key : score.getKeys(false)){
			String name = getServer().getOfflinePlayer(UUID.fromString(key.substring(key.indexOf("$") + 1))).getName();
			blank.add(key.substring(0, key.indexOf("$")) + "$" + name);
		}
		leaderboard = blank;
		for(int i = 0; i < blank.size() - 1; i++){
			if(score.getDouble(blank.get(i)) > score.getDouble(blank.get(i+1))){
				String temp = blank.get(i+1);
				blank.set(i+1, blank.get(i));
				blank.set(i, temp);
			}
		}
		leaderboard = blank;
	}
	public void makeRestartBlocksList(){
		restartBlocks = getConfig().getStringList("restart-blocks");
	}
	public void reload(){
		reloadConfig();
		saveFC();
		makeLeaderboard();
		makeRestartBlocksList();
	}

	@EventHandler
	public void movement(PlayerMoveEvent e){
		Player player = e.getPlayer();
		if(player.getLocation().add(0,-1,0).getBlock().getType() == Material.getMaterial(getConfig().getString("start-block"))){
			for(int i = 0; i < parkourList.size(); i++){
				String name = parkourList.get(i);
				Location l = player.getLocation();
				int x = Integer.parseInt(data.getString(name + ".start-cords.x"));
				int y = Integer.parseInt(data.getString(name + ".start-cords.y"));
				int z = Integer.parseInt(data.getString(name + ".start-cords.z"));		
				if(player.getWorld().getName().equals(data.getString(name + ".world")) &&
						l.getBlockX() == x && l.getBlockY() - 1 == y && l.getBlockZ() == z){start(name, player);}
			}
			return;
		}
		if(player.getLocation().add(0,-1,0).getBlock().getType() == Material.getMaterial(getConfig().getString("end-block"))){
			for(int i = 0; i < parkourList.size(); i++){
				String name = parkourList.get(i);
				Location l = player.getLocation();
				int x = Integer.parseInt(data.getString(name + ".end-cords.x"));
				int y = Integer.parseInt(data.getString(name + ".end-cords.y"));
				int z = Integer.parseInt(data.getString(name + ".end-cords.z"));				
				if(player.getWorld().getName().equals(data.getString(name + ".world")) &&
						l.getBlockX() == x && l.getBlockY() - 1 == y && l.getBlockZ() == z){end(name, player, true);}
			}
			return;
		}
		for(String block : getConfig().getStringList("restart-blocks")){
			if(player.getLocation().add(0,-1,0).getBlock().getType() == Material.getMaterial(block)){
				for(String p : parkourList){
					ParkourPlayer ppt = new ParkourPlayer(p, player);
					for(ParkourPlayer pp : pps){
						if(ppt.equals(pp)){
							player.teleport(new Location(player.getWorld(), 
											Integer.parseInt(data.getString(p + ".start-cords.x")) + 0.5,
											Integer.parseInt(data.getString(p + ".start-cords.y")) + 1,
											Integer.parseInt(data.getString(p + ".start-cords.z")) + 0.5, 
											player.getLocation().getYaw(), player.getLocation().getPitch())
											);
							player.sendMessage(prefix + ChatColor.GOLD + "Your time has been reset.");
							pp.time = 0;
							break;
						}
					}
				}
			}
		}
	}
	//Start parkour, "name", for player, "player"
	public void start(String name, Player player){
		ParkourPlayer ppt = new ParkourPlayer(name, player);
		if(pps.size() == 0){
			player.sendMessage(prefix + ChatColor.GOLD + "Your time starts now!");
			pps.add(ppt);
		}
		for(ParkourPlayer pp : pps){
			if(ppt.equals(pp)){
				if(pp.time >= 50){
					player.sendMessage(prefix + ChatColor.GOLD + "Your time has been reset.");
					pp.time = 0;
				}
				return;
			}
		}
		player.sendMessage(prefix + ChatColor.GOLD + "Your time starts now!");
		pps.add(ppt);
	}
	//Start parkour, "name", for player, "player"
	public void end(String name, Player player, boolean save){
		ParkourPlayer ppt = new ParkourPlayer(name, player);
		for(ParkourPlayer pp : pps){
			if(ppt.equals(pp)){
				double time = pp.time/10.0;
				if(save == true){
					String oldTime = score.getString(pp.name + "-" + pp.player_name);
					if(oldTime == null || Double.parseDouble(oldTime) > time){
						score.set(pp.name + "$" + pp.UUID, time);
					}
					player.teleport(new Location(player.getWorld(), 
							Integer.parseInt(data.getString(pp.name + ".start-cords.x")) -0.5,
							Integer.parseInt(data.getString(pp.name + ".start-cords.y")) + 1,
							Integer.parseInt(data.getString(pp.name + ".start-cords.z")) - 0.5, 
							player.getLocation().getYaw(), player.getLocation().getPitch())
							);
					player.sendMessage(prefix + ChatColor.GOLD + "Your time was " + ChatColor.GREEN 
							+ time + ChatColor.GOLD + " secs!");
				}
				pps.remove(pp);
				makeLeaderboard();
				reload();
				break;
			}
		}
	}
	public void timer(){
		this.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			 
			  public void run() {
			    for(ParkourPlayer pp : pps){
			    	pp.time++;
			    }  
			  }
			},0L, 2L);
	}
	public class ParkourPlayer{
		String name;
		Player player;
		String player_name;
		String UUID;
		int time = 0;
		public ParkourPlayer(String name, Player player){
			this.name = name;
			this.player = player;
			player_name = player.getName();
			UUID = player.getUniqueId().toString();
		}
		public boolean equals(ParkourPlayer other){
			return this.name == other.name && this.player == other.player;
		}
	}
}
