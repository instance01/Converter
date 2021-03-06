package com.comze_instancelabs.convert;

import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.plugin.java.JavaPlugin;


public class Main extends JavaPlugin implements Listener{
	
	//TODO: saving credits into mysql
	
	// setup:
	/*
	 * once at beginning:
	 * cm setmainlobby
	 * 
	 * for each arena:
	 * cm createarena name
	 * cm setlobby name
	 * cm setspawn name 1
	 * cm setspawn name 2
	 * 
	 * set the sign:
	 * 1st line: converter
	 * 3rd line: name
	 * 
	 */
	
	public int cpw = 0;
	public int cpk = 0;
	public int minplayers = 0;
	
	public static HashMap<String, String> arenap_ = new HashMap<String, String>(); // INGAME player -> arena
	public static HashMap<String, String> arenap = new HashMap<String, String>(); // LOBBY player -> arena
	public static HashMap<String, String> pteam = new HashMap<String, String>(); // player -> team
	public static HashMap<String, String> lastteam = new HashMap<String, String>(); // INGAME player -> arena
	public static HashMap<String, Boolean> canthrow = new HashMap<String, Boolean>();
	
	public static ArrayList<String> lost = new ArrayList<String>();
	
	@Override
	public void onEnable(){
		getServer().getPluginManager().registerEvents(this, this);
		
		getConfig().addDefault("config.players", 4);
		getConfig().addDefault("config.credits_per_win", 30);
		getConfig().addDefault("config.credits_per_kill", 30);
		getConfig().addDefault("mysql.host", "localhost");
		getConfig().addDefault("mysql.db", "minecraft");
		getConfig().addDefault("mysql.user", "root");
		getConfig().addDefault("mysql.pw", "password");
		getConfig().options().copyDefaults(true);
		this.saveConfig();
		
		cpw = getConfig().getInt("config.credits_per_win");
		cpk = getConfig().getInt("config.credits_per_kill");
		minplayers = getConfig().getInt("config.players");
	}
	
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if(cmd.getName().equalsIgnoreCase("converter") || cmd.getName().equalsIgnoreCase("conv")){
			if(args.length > 0){
				String action = args[0];
				if(action.equalsIgnoreCase("setmainlobby")){
					if(sender.hasPermission("converter.setup")){
						if(sender instanceof Player){
							Location l = ((Player) sender).getLocation();
							getConfig().set("mainlobby.world", l.getWorld().getName());
							getConfig().set("mainlobby.loc.x", l.getBlockX());
							getConfig().set("mainlobby.loc.y", l.getBlockY());
							getConfig().set("mainlobby.loc.z", l.getBlockZ());
							this.saveConfig();
							sender.sendMessage(ChatColor.GREEN + "Successfully set component.");
						}
					}
				}else if(action.equalsIgnoreCase("createarena")){
					if(sender.hasPermission("converter.setup")){
						if(args.length > 1){
							String arena = args[1];
							
							getConfig().set(arena + ".name", arena);
							sender.sendMessage(ChatColor.GREEN + "Successfully saved arena.");
						}else{
							sender.sendMessage("�cInvalid argument count! See /conv help for more info.");
						}
					}
				}else if(action.equalsIgnoreCase("setlobby")){
					if(sender.hasPermission("converter.setup")){
						if(args.length > 1){
							String arena = args[1];
							
							if(sender instanceof Player){
								Location l = ((Player) sender).getLocation();
								getConfig().set(arena + ".lobby.world", l.getWorld().getName());
								getConfig().set(arena + ".lobby.loc.x", l.getBlockX());
								getConfig().set(arena + ".lobby.loc.y", l.getBlockY());
								getConfig().set(arena + ".lobby.loc.z", l.getBlockZ());
								this.saveConfig();
								sender.sendMessage(ChatColor.GREEN + "Successfully set component.");
							}
						}else{
							sender.sendMessage("�cInvalid argument count! See /conv help for more info.");
						}
					}
				}else if(action.equalsIgnoreCase("setspawn")){
					if(sender.hasPermission("converter.setup")){
						if(args.length > 2){
							String arena = args[1];
							String count = args[2];
							
							if(isNumeric(count)){
								if(count.equalsIgnoreCase("1") || count.equalsIgnoreCase("2")){
									if(sender instanceof Player){
										Location l = ((Player) sender).getLocation();
										getConfig().set(arena + ".spawn" + count + ".world", l.getWorld().getName());
										getConfig().set(arena + ".spawn" + count + ".loc.x", l.getBlockX());
										getConfig().set(arena + ".spawn" + count + ".loc.y", l.getBlockY());
										getConfig().set(arena + ".spawn" + count + ".loc.z", l.getBlockZ());
										this.saveConfig();
										sender.sendMessage(ChatColor.GREEN + "Successfully set component.");
									}
								}else{
									sender.sendMessage("�cCount needs to be 1 or 2!");
								}
							}
						}else{
							sender.sendMessage("�cInvalid argument count! See /conv help for more info.");
						}
					}
				}else if(action.equalsIgnoreCase("join")){
					if(args.length > 1){
						String arena = args[1];
						
						if(sender instanceof Player){
							if(isValidArena(arena)){
								Player p = (Player)sender;
								Sign s = this.getSignFromArena(arena);
								if(s != null){
									if(s.getLine(1).equalsIgnoreCase("�2[join]")){
										joinLobby(p, arena);
									}
								}
							}
						}
					}else{
						sender.sendMessage("�cInvalid argument count! See /conv help for more info.");
					}
				}else if(action.equalsIgnoreCase("leave")){
					if(sender instanceof Player){
						Player p = (Player)sender;
						if(arenap.containsKey(p.getName())){
							leave(p, true);
						}
					}
				}else if(action.equalsIgnoreCase("list")){
					if(sender.hasPermission("converter.list")){
						sender.sendMessage("");
						//TODO list all arenas
					}
				}else if(action.equalsIgnoreCase("start")){
					if(sender.hasPermission("converter.setup")){
						if(args.length > 1){
							final String arena = args[1];
							if(isValidArena(arena)){
								for (final String p_ : arenap.keySet()) {
									final Player p__ = Bukkit.getPlayerExact(p_);
									if (arenap.get(p_).equalsIgnoreCase(arena)) {
										Bukkit.getScheduler().runTaskLater(this, new Runnable() {
											public void run() {
												try{
													p__.teleport(getSpawn(arena, pteam.get(p_)));
													setTeam(p__, pteam.get(p_));
												}catch(Exception e){
													e.printStackTrace();
												}
											}
										}, 5);
									}
								}
								Bukkit.getScheduler().runTaskLater(this, new Runnable() {
									public void run() {
										try{
											start(arena);
										}catch(Exception e){
											e.printStackTrace();
										}
									}
								}, 10);
							}
						}else{
							sender.sendMessage("�cInvalid argument count! See /conv help for more info.");
						}
					}
				}else if(action.equalsIgnoreCase("help")){
					sendHelp(sender);
				}
			}else{
				sendHelp(sender);
			}
			return true;
		}
		return false;
	}
	
	public void sendHelp(CommandSender sender){
		sender.sendMessage("�6-= Converter �2help: �6=-");
		sender.sendMessage("�2To �6setup the main lobby �2, type in �c/conv setmainlobby");
		sender.sendMessage("�2To �6setup �2a new arena, type in the following commands:");
		sender.sendMessage("�2/conv createarena [name]");
		sender.sendMessage("�2/conv setspawn [name] 1");
		sender.sendMessage("�2/conv setspawn [name] 2");
		sender.sendMessage("�2/conv setlobby [name] �6- for the waiting lobby");
		sender.sendMessage("");
		sender.sendMessage("�2You can join with �c/conv join [name] �2and leave with �c/conv leave�2.");
		sender.sendMessage("�2You can force an arena to start with �c/conv start [name]�2.");
	}

	
	public void onPlayerDropItem(PlayerDropItemEvent event){
		if(arenap_.containsKey(event.getPlayer().getName())){
			event.setCancelled(true);
		}
    }
	
	@EventHandler
    public void onHunger(FoodLevelChangeEvent event){
    	if(event.getEntity() instanceof Player){
    		Player p = (Player)event.getEntity();
    		if(arenap_.containsKey(p.getName())){
    			event.setCancelled(true);
    		}
    	}
    }
	
	
	@EventHandler
    public void onSignUse(PlayerInteractEvent event){
    	if (event.hasBlock())
	    {
	        if (event.getClickedBlock().getType() == Material.SIGN_POST || event.getClickedBlock().getType() == Material.WALL_SIGN)
	        {
	            final Sign s = (Sign) event.getClickedBlock().getState();
	            if(s.getLine(0).toLowerCase().contains("converter")){
	            	if(s.getLine(1).equalsIgnoreCase("�2[join]")){
	            		joinLobby(event.getPlayer(), s.getLine(2));
	            	}
	            }
	        }
	    }
	}
    
    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        Player p = event.getPlayer();
        if(event.getLine(0).toLowerCase().equalsIgnoreCase("converter")){
        	if(event.getPlayer().hasPermission("converter.sign")){
	        	event.setLine(0, "�6�lConverter");
	        	if(!event.getLine(2).equalsIgnoreCase("")){
	        		String arena = event.getLine(2);
	        		if(isValidArena(arena)){
	        			getConfig().set(arena + ".sign.world", p.getWorld().getName());
	        			getConfig().set(arena + ".sign.loc.x", event.getBlock().getLocation().getBlockX());
						getConfig().set(arena + ".sign.loc.y", event.getBlock().getLocation().getBlockY());
						getConfig().set(arena + ".sign.loc.z", event.getBlock().getLocation().getBlockZ());
						this.saveConfig();
						p.sendMessage("�2Successfully created arena sign.");
	        		}else{
	        			p.sendMessage("�2The arena appears to be invalid (missing components or misstyped arena)!");
	        			event.getBlock().breakNaturally();
	        		}
	        		event.setLine(1, "�2[Join]");
	        		event.setLine(2, arena);
	        		event.setLine(3, "0/" + Integer.toString(this.minplayers));
	        	}
        	}
        }
	}

    public Sign getSignFromArena(String arena){
		Location b_ = new Location(Bukkit.getWorld(getConfig().getString(arena + ".sign.world")), getConfig().getInt(arena + ".sign.loc.x"), getConfig().getInt(arena + ".sign.loc.y"), getConfig().getInt(arena + ".sign.loc.z"));
    	BlockState bs = b_.getBlock().getState();
    	Sign s_ = null;
    	if(bs instanceof Sign){
    		s_ = (Sign)bs;
    	}else{
    		getLogger().info("Could not find sign: " + bs.getBlock().toString());
    	}
		return s_;
	}
    
	public Location getLobby(String arena) {
		Location ret = null;
		if (isValidArena(arena)) {
			ret = new Location(Bukkit.getWorld(getConfig().getString(
					arena + ".lobby.world")), getConfig().getInt(
					arena + ".lobby.loc.x"),
					getConfig().getInt(arena + ".lobby.loc.y"), getConfig().getInt(
							arena + ".lobby.loc.z"));
		}
		return ret;
	}

	public Location getMainLobby() {
		Location ret = new Location(Bukkit.getWorld(getConfig().getString(
				"mainlobby.world")), getConfig().getInt("mainlobby.loc.x"),
				getConfig().getInt("mainlobby.loc.y"), getConfig().getInt(
						"mainlobby.loc.z"));
		return ret;
	}

	public Location getSpawn(String arena, String count) {
		Location ret = null;
		if (isValidArena(arena)) {
			ret = new Location(Bukkit.getWorld(getConfig().getString(
					arena + ".spawn" + count + ".world")), getConfig().getInt(
					arena + ".spawn" + count + ".loc.x"),
					getConfig().getInt(arena + ".spawn" + count + ".loc.y"), getConfig().getInt(
							arena + ".spawn" + count + ".loc.z"));
		}
		return ret;
	}
	
	public boolean isValidArena(String arena) {
		if (getConfig().isSet(arena + ".spawn1") && getConfig().isSet(arena + ".spawn2") && getConfig().isSet("mainlobby") && getConfig().isSet(arena + ".lobby")) {
			return true;
		}
		return false;
	}
	
	public static boolean isNumeric(String str) {
		try {
			double d = Double.parseDouble(str);
		} catch (NumberFormatException nfe) {
			return false;
		}
		return true;
	}
	
	
	
	
	public void joinLobby(final Player p, final String arena) {
		arenap.put(p.getName(), arena);

		p.sendMessage(ChatColor.GREEN + "You joined arena " + ChatColor.GOLD + arena + ChatColor.GREEN + ".");
		
		String next = "1";
		if(lastteam.containsKey(arena)){
			String c = lastteam.get(arena);
			if(c.equalsIgnoreCase("1")){
				next = "2";
			}else{
				next = "1";
			}
		}
		lastteam.put(arena, next);
		setTeam(p, next);
		
		p.setGameMode(GameMode.SURVIVAL);
		Bukkit.getScheduler().runTaskLater(this, new Runnable() {
			public void run() {
				p.teleport(getLobby(arena));
				p.getInventory().clear();
				p.updateInventory();
			}
		}, 5);

		int count = 0;
		for (String p_ : arenap.keySet()) {
			if (arenap.get(p_).equalsIgnoreCase(arena)) {
				count++;
			}
		}
		if (count > minplayers - 1) {
			for (final String p_ : arenap.keySet()) {
				final Player p__ = Bukkit.getPlayerExact(p_);
				if (arenap.get(p_).equalsIgnoreCase(arena)) {
					Bukkit.getScheduler().runTaskLater(this, new Runnable() {
						public void run() {
							p__.teleport(getSpawn(arena, pteam.get(p_)));
							setTeam(p__, pteam.get(p_));
						}
					}, 5);
				}
			}
			Bukkit.getScheduler().runTaskLater(this, new Runnable() {
				public void run() {
					start(arena);
				}
			}, 10);
		}
		
		try{
			Sign s = this.getSignFromArena(arena);
			if(s != null){
				s.setLine(3, Integer.toString(count) + "/" + Integer.toString(this.minplayers));
				s.update();
			}
		}catch(Exception e){
			getLogger().warning("You forgot to set a sign for arena " + arena + "! This may lead to errors.");
		}
		
	}
	
	
	public void leave(final Player p, boolean flag){
		p.getInventory().clear();
		p.updateInventory();
		p.getInventory().setBoots(null);
		p.getInventory().setHelmet(null);
		p.getInventory().setLeggings(null);
		p.getInventory().setChestplate(null);
		p.updateInventory();
		Bukkit.getScheduler().runTaskLater(this, new Runnable() {
			public void run() {
				p.teleport(getMainLobby());
			}
		}, 5);

		String arena = arenap.get(p.getName());

		if (arenap.containsKey(p.getName())) {
			arenap.remove(p.getName());
		}
		if (arenap_.containsKey(p.getName())) {
			arenap_.remove(p.getName());
		}

		
		int count = 0;
		for (String p_ : arenap.keySet()) {
			if (arenap.get(p_).equalsIgnoreCase(arena)) {
				count++;
			}
		}
		
		if(flag){
			if(count < 2){ // only one player left, stop it then
				stop(arena);
			}
		}
	}
	
	public void start(String arena){
		Sign s = this.getSignFromArena(arena);
		if(s != null){
			s.setLine(1, "�4[Ingame]");
			s.update();
		}
		
		for (String p_ : arenap.keySet()) {
			Player p = Bukkit.getPlayerExact(p_);
			if (arenap.get(p.getName()).equalsIgnoreCase(arena)) {
				arenap_.put(p.getName(), arena);
				// set inventory and exp bar
				p.getInventory().clear();
				p.updateInventory();
				p.getInventory().addItem(new ItemStack(Material.SNOW_BALL, 1));
				p.updateInventory();
			}
		}
	}
	
	public void stop(String arena){
		ArrayList<Player> rem = new ArrayList<Player>();
		for(String p_ : arenap.keySet()){
			if(arenap.get(p_).equalsIgnoreCase(arena)){ 
				Player p = Bukkit.getPlayerExact(p_);
				rem.add(p);
			}
		}
		
		for(Player p : rem){
			leave(p, false);
		}

		Sign s = this.getSignFromArena(arena);
		if(s != null){
			s.setLine(1, "�2[Join]");
			s.setLine(3, "0/" + Integer.toString(minplayers));
			s.update();
		}

	}
	
	
	
	
	
	@EventHandler
	public void onProjectileThrownEvent(ProjectileLaunchEvent event) {
		if (event.getEntity() instanceof Snowball) {
			if(event.getEntity().getShooter() instanceof Player){
				final Player p = (Player)event.getEntity().getShooter();
				if(p != null){
					if(arenap_.containsKey(p.getName())){
						if(canthrow.containsKey(p.getName())){
							if(canthrow.get(p.getName())){
								// player can throw
								canthrow.put(p.getName(), false);
								p.getInventory().addItem(new ItemStack(Material.SNOW_BALL, 1));
								p.updateInventory();
								Bukkit.getScheduler().runTaskLater(this, new Runnable(){
									public void run(){
										canthrow.put(p.getName(), true);
									}
								}, 39); // 2 seconds
							}else{
								event.setCancelled(true);
								p.getInventory().addItem(new ItemStack(Material.SNOW_BALL, 1));
								p.updateInventory();
							}
						}else{
							canthrow.put(p.getName(), false);
							p.getInventory().addItem(new ItemStack(Material.SNOW_BALL, 1));
							p.updateInventory();
							Bukkit.getScheduler().runTaskLater(this, new Runnable(){
								public void run(){
									canthrow.put(p.getName(), true);
								}
							}, 39); // 2 seconds
						}
					}
				}	
			}
		}
	}
	
	@EventHandler
	public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
		try{
			if (event.getDamager() instanceof Snowball) {
				Snowball snowball = (Snowball) event.getDamager();
				Entity hitBySnowball = event.getEntity();
				LivingEntity shooter = snowball.getShooter();
				if (hitBySnowball instanceof Player && shooter instanceof Player) {
					// set the new team
					Player player = (Player) hitBySnowball;
					Player o = (Player) shooter;
					if(arenap_.containsKey(player.getName()) && arenap_.containsKey(o.getName())){
						String team = pteam.get(o.getName());
						String otherteam = pteam.get(player.getName());
						String arena = arenap_.get(player.getName());
						setTeam(player, team);
						lost.add(player.getName());
						int count = 0;
						for(String p : pteam.keySet()){
							if(arenap_.get(p).equalsIgnoreCase(arena)){
								if(pteam.get(p).equalsIgnoreCase(otherteam)){
									count ++;
								}
							}
						}
						if(count < 1){
							// noones in the other team anymore
							teamwin(arena, team);
							stop(arena);
						}
					}
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}
	
	public void teamwin(String arena, String team){
		for(String p : pteam.keySet()){
			if(arenap_.get(p).equalsIgnoreCase(arena)){
				if(pteam.get(p).equalsIgnoreCase(team)){
					Player p_ = Bukkit.getPlayerExact(p);
					if(lost.contains(p)){
						p_.sendMessage(ChatColor.RED + "Your team lost this game.");
						//TODO save lost stats
						lost.remove(p);
					}else{
						p_.sendMessage(ChatColor.GREEN + "Your team won this game!");
						//TODO give credits via mysql and save won stats
					}
				}else{
					Player p_ = Bukkit.getPlayerExact(p);
					p_.sendMessage(ChatColor.RED + "Your team lost this game.");
					//TODO save lost stats
				}
			}
		}
	}
	
	
	public void setTeam(Player p, String team){
		pteam.put(p.getName(), team);
		
		ItemStack lhelmet = new ItemStack(Material.LEATHER_HELMET, 1);
	    LeatherArmorMeta lam = (LeatherArmorMeta)lhelmet.getItemMeta();
	    
	    ItemStack lboots = new ItemStack(Material.LEATHER_BOOTS, 1);
	    LeatherArmorMeta lam1 = (LeatherArmorMeta)lboots.getItemMeta();
	    
	    ItemStack lchestplate = new ItemStack(Material.LEATHER_CHESTPLATE, 1);
	    LeatherArmorMeta lam2 = (LeatherArmorMeta)lchestplate.getItemMeta();
	    
	    ItemStack lleggings = new ItemStack(Material.LEATHER_LEGGINGS, 1);
	    LeatherArmorMeta lam3 = (LeatherArmorMeta)lleggings.getItemMeta();

	    Color c;
	    if(team.equalsIgnoreCase("1")){
	    	c = Color.RED;
	    }else{
	    	c = Color.BLUE;
	    }
	    lam3.setColor(c);
	    lam2.setColor(c);
	    lam1.setColor(c);
	    lam.setColor(c);
	   
	    lhelmet.setItemMeta(lam);
	    lboots.setItemMeta(lam1);
	    lchestplate.setItemMeta(lam2);
	    lleggings.setItemMeta(lam3);
		
	    p.getInventory().setBoots(lboots);
	    p.getInventory().setHelmet(lhelmet);
	    p.getInventory().setChestplate(lchestplate);
	    p.getInventory().setLeggings(lleggings);
	    
		p.sendMessage("�aYou are in Team �6" + team + " �anow!");
	}
}
