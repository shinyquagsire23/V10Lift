package de.V10lator.V10lift;

import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeSet;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitScheduler;

class VLCE implements CommandExecutor
{
  private final V10lift plugin;
  
  VLCE(V10lift plugin)
  {
	this.plugin = plugin;
  }
  
  public boolean onCommand(CommandSender sender, Command cmd, String command,
			String[] args)
  {
	if(!(sender instanceof Player))
	{
	  sender.sendMessage(ChatColor.RED+"You have to use this ingame!");
	  return true;
	}
	if(!plugin.hasPerm(sender, "v10lift.build") &&
			!plugin.hasPerm(sender, "v10lift.admin"))
	{
	  sender.sendMessage(ChatColor.RED+"You are not allowed to do that!");
	  return true;
	}
	if(args.length < 1)
	{
	  sendHelp(sender);
	  return true;
	}
	String player = ((Player)sender).getName();
	if(args[0].equalsIgnoreCase("create"))
	{
	  if(plugin.builds.containsKey(player))
	  {
		if(args.length < 2)
		{
		  sender.sendMessage(ChatColor.RED+"Your lift needs a name!");
		  return true;
		}
		HashSet<LiftBlock> blocks = plugin.builds.get(player);
		if(blocks.isEmpty())
		{
		  sender.sendMessage(ChatColor.RED+"Add blocks first!");
		  return true;
		}
		StringBuilder sb = new StringBuilder(args[1]);
		for(int i = 2; i < args.length; i++)
		{
		  sb.append(" ");
		  sb.append(args[i]);
		}
		int l = sb.length();
		if(l > 15)
		  sb.delete(16, l);
		String lift = sb.toString().trim();
		if(!plugin.api.createNewLift(player, lift))
		  sender.sendMessage(ChatColor.RED+"Can't create two lifts with the same name!");
		TreeSet<LiftBlock> blcks = plugin.lifts.get(lift).blocks;
		for(LiftBlock lb: blocks)
		  plugin.api.addBlockToLift(blcks, lb);
		plugin.api.sortLiftBlocks(lift);
		plugin.builds.remove(player);
		sender.sendMessage(ChatColor.GREEN+"Lift \""+ChatColor.YELLOW+lift+ChatColor.GREEN+"\" created!");
		((Player)sender).performCommand("v10lift edit "+lift);
	  }
	  else
	  {
		plugin.builds.put(player, new HashSet<LiftBlock>());
		sender.sendMessage(ChatColor.GOLD+"Okay, now add all blocks of your lift cabin by right clicking them.");
		sender.sendMessage(ChatColor.GOLD+"Then type: /v10lift create your lift name");
	  }
	}
	else if(args[0].equalsIgnoreCase("delete"))
	{
	  if(args.length < 2)
	  {
		sendHelp(sender, "delete");
		return true;
	  }
	  StringBuilder sb = new StringBuilder(args[1]);
	  for(int i = 2; i < args.length; i++)
		sb.append(" ").append(args[i]);
	  String lift = sb.toString();
	  if(!plugin.lifts.containsKey(lift))
	  {
		sender.sendMessage(ChatColor.RED+"Lift \""+ChatColor.YELLOW+lift+ChatColor.RED+"\" doesn't exist!");
		return true;
	  }
	  if(!plugin.lifts.get(lift).owners.contains(player) && !plugin.hasPerm(sender, "v10lift.admin"))
	  {
		sender.sendMessage(ChatColor.RED+"You're not a owner of this lift!");
		return true;
	  }
	  plugin.api.removeLift(lift);
	  sender.sendMessage(ChatColor.GOLD+"Lift removed.");
	}
	else if(args[0].equalsIgnoreCase("abort"))
	{
	  boolean abort = false;
	  
	  if(plugin.builds.containsKey(player))
	  {
		plugin.builds.remove(player);
		abort = true;
	  }
	  if(plugin.whoisReq.contains(player))
	  {
		plugin.whoisReq.remove(player);
		abort = true;
	  }
	  if(plugin.inputEdits.containsKey(player))
	  {
		plugin.inputEdits.remove(player);
		abort = true;
	  }
	  if(plugin.inputRemoves.contains(player))
	  {
		plugin.inputRemoves.remove(player);
		abort = true;
	  }
	  if(plugin.offlineEdits.contains(player))
	  {
		plugin.offlineEdits.remove(player);
		abort = true;
	  }
	  if(plugin.offlineRemoves.contains(player))
	  {
		plugin.offlineRemoves.remove(player);
		abort = true;
	  }
	  if(plugin.builder.contains(player))
	  {
		plugin.builder.remove(player);
		plugin.api.sortLiftBlocks(plugin.editors.get(player));
		abort = true;
	  }
	  if(plugin.ropeEdits.containsKey(player))
	  {
		plugin.ropeEdits.remove(player);
		abort = true;
	  }
	  if(plugin.ropeRemoves.contains(player))
	  {
		plugin.ropeRemoves.remove(player);
		abort = true;
	  }
	  if(plugin.doorEdits.containsKey(player))
	  {
		plugin.doorEdits.remove(player);
		abort = true;
	  }
	  if(abort)
		sender.sendMessage(ChatColor.GOLD+"Aborted.");
	  else
		sender.sendMessage(ChatColor.RED+"There's nothing to abort.");
	}
	else if(args[0].equalsIgnoreCase("edit"))
	{
	  if(args.length < 2)
	  {
		if(!plugin.editors.containsKey(player))
		{
		  sendHelp(sender, "edit");
		  return true;
		}
		String lift = plugin.editors.get(player);
		if(!plugin.lifts.containsKey(lift))
		{
		  plugin.editors.remove(player);
		  sender.sendMessage(ChatColor.RED+"Lift not found!");
		  return true;
		}
		Lift li = plugin.lifts.get(lift);
		plugin.editors.remove(player);
		plugin.inputEdits.remove(player);
		plugin.inputRemoves.remove(player);
		plugin.offlineEdits.remove(player);
		plugin.offlineRemoves.remove(player);
		if(plugin.builder.contains(player))
		{
		  plugin.builder.remove(player);
		  plugin.api.sortLiftBlocks(plugin.editors.get(player));
		}
		plugin.ropeEdits.remove(player);
		plugin.ropeRemoves.remove(player);
		plugin.doorEdits.remove(player);
		BlockState bs;
		Server s = plugin.getServer();
		Sign sign;
		for(LiftBlock lb: li.blocks)
		{
		  bs = s.getWorld(lb.world).getBlockAt(lb.x, lb.y, lb.z).getState();
		  if(!(bs instanceof Sign))
			continue;
		  sign = (Sign)bs;
		  if(!sign.getLine(0).equalsIgnoreCase(plugin.signText))
			continue;
		  sign.setLine(3, "");
		  sign.update();
		}
		Iterator<LiftSign> liter = li.signs.iterator();
		while(liter.hasNext())
		{
		  LiftSign ls = liter.next();
		  bs = s.getWorld(ls.world).getBlockAt(ls.x, ls.y, ls.z).getState();
		  if(!(bs instanceof Sign))
		  {
			s.getLogger().info("["+plugin.getName()+"] Wrong sign deleted at: "+ls.x+", "+ls.y+", "+ls.z+" in world "+ls.world);
			liter.remove();
			continue;
		  }
		  sign = (Sign)bs;
		  sign.setLine(3, ls.oldText);
		  sign.update();
		  ls.oldText = null;
		}
		sender.sendMessage(ChatColor.GOLD+"Editor disabled!");
		return true;
	  }
	  else if(plugin.editors.containsKey(player))
	  {
		sender.sendMessage(ChatColor.RED+"Still in editing mode!");
		return true;
	  }
	  StringBuilder sb = new StringBuilder(args[1]);
	  for(int i = 2; i < args.length; i++)
	  {
		sb.append(" ");
		sb.append(args[i]);
	  }
	  String lift = sb.toString();
	  if(!plugin.lifts.containsKey(lift))
	  {
		sender.sendMessage(ChatColor.RED+"Lift \""+ChatColor.YELLOW+lift+ChatColor.RED+"\" doesn't exist!");
		return true;
	  }
	  Lift li = plugin.lifts.get(lift);
	  if(!li.owners.contains(player) && !plugin.hasPerm(sender, "v10lift.admin"))
	  {
		sender.sendMessage(ChatColor.RED+"You're not a owner of this lift!");
		return true;
	  }
	  plugin.editors.put(player, lift);
	  BlockState bs;
	  Server s = plugin.getServer();
	  Sign sign;
	  for(LiftBlock lb: li.blocks)
	  {
		bs = s.getWorld(lb.world).getBlockAt(lb.x, lb.y, lb.z).getState();
		if(!(bs instanceof Sign))
		  continue;
		sign = (Sign)bs;
		if(!sign.getLine(0).equalsIgnoreCase(plugin.signText))
		  continue;
		sign.setLine(3, ChatColor.RED+"Maintenance");
		sign.update();
	  }
	  Iterator<LiftSign> liter = li.signs.iterator();
	  while(liter.hasNext())
	  {
		LiftSign ls = liter.next();
		bs = s.getWorld(ls.world).getBlockAt(ls.x, ls.y, ls.z).getState();
		if(!(bs instanceof Sign))
		{
		  s.getLogger().info("["+plugin.getName()+"] Wrong sign deleted at: "+ls.x+", "+ls.y+", "+ls.z+" in world "+ls.world);
		  liter.remove();
		  continue;
		}
		sign = (Sign)bs;
		ls.oldText = sign.getLine(3);
		sign.setLine(3, ChatColor.RED+"Maintenance");
		sign.update();
	  }
	  sender.sendMessage(ChatColor.GREEN+"Editor enabled!");
	}
	else if(args[0].equalsIgnoreCase("floor"))
	{
	  if(!plugin.editors.containsKey(player))
	  {
		sender.sendMessage(ChatColor.RED+"Start editing mode first!");
		return true;
	  }
	  if(args.length < 3)
	  {
		sender.sendMessage(ChatColor.RED+"No floor name given!");
		sendHelp(sender, "floor");
		return true;
	  }
	  String lift = plugin.editors.get(player);
	  if(args[1].equalsIgnoreCase("add"))
	  {
		Block b = ((Player)sender).getLocation().getBlock();
		StringBuilder sb = new StringBuilder(args[2]);
		for(int i = 3; i < args.length; i++)
		{
		  sb.append(" ");
		  sb.append(args[i]);
		}
		String floor = sb.toString();
		
		int ret = plugin.api.addNewFloor(lift, floor, new Floor(b.getY() - 1, b.getWorld().getName()));
		if(ret == 0)
		  sender.sendMessage(ChatColor.GREEN+"Floor \""+ChatColor.YELLOW+floor+ChatColor.GREEN+"\" created!");
		else if(ret == -2)
		  sender.sendMessage(ChatColor.RED+"Can't create two floors with the same name!");
		else if(ret == -3)
		  sender.sendMessage(ChatColor.RED+"Can't create two floors at the same height!");
		else
		  sender.sendMessage(ChatColor.RED+"Internal error!");
	  }
	  else if(args[1].equalsIgnoreCase("del"))
	  {
		StringBuilder sb = new StringBuilder(args[2]);
		for(int i = 3; i < args.length; i++)
		{
		  sb.append(" ");
		  sb.append(args[i]);
		}
		if(plugin.api.removeFloor(lift, sb.toString()))
		  sender.sendMessage(ChatColor.GOLD+"Floor removed!");
		else
		  sender.sendMessage(ChatColor.RED+"Floor \""+ChatColor.YELLOW+sb.toString()+ChatColor.RED+"\" not found!");
	  }
	  else if(args[1].equalsIgnoreCase("rename"))
	  {
		if(args.length < 4)
		{
		  sender.sendMessage(ChatColor.RED+"No floor name given!");
		  sendHelp(sender, "floor");
		  return true;
		}
		StringBuilder sb = new StringBuilder(args[2]);
		String floor = sb.toString();
		int i = 3;
		Lift li = plugin.lifts.get(lift);
		while(!li.floors.containsKey(floor))
		{
		  sb.append(" ").append(args[i]);
		  floor = sb.toString();
		  i++;
		  if(i >= args.length)
		  {
			sender.sendMessage(ChatColor.RED+"Floor \""+ChatColor.YELLOW+floor+ChatColor.RED+"\" not found!");
			return true;
		  }
		}
		if(args.length < i + 1)
		{
		  sender.sendMessage(ChatColor.RED+"No floor name given!");
		  sendHelp(sender, "floor");
		  return true;
		}
		sb = new StringBuilder(args[i]);
		i++;
		for(; i < args.length; i++)
		  sb.append(" ").append(args[i]);
		int ret = plugin.api.renameFloor(lift, floor, sb.toString());
		if(ret == -4)
		  sender.sendMessage(ChatColor.RED+"Can't create two floors with the same name!");
		else if(ret < 0)
		  sender.sendMessage(ChatColor.RED+"Internal error!");
		else
		  sender.sendMessage(ChatColor.GREEN+"Floor renamed!");
	  }
	  else
	  {
		sendHelp(sender, "floor");
		return true;
	  }
	}
	else if(args[0].equalsIgnoreCase("input"))
	{
	  if(!plugin.editors.containsKey(player))
	  {
		sender.sendMessage(ChatColor.RED+"Start editing mode first!");
		return true;
	  }
	  if(args.length < 2)
	  {
		sendHelp(sender, "input");
		return true;
	  }
	  Lift lift = plugin.lifts.get(plugin.editors.get(player));
	  if(args[1].equalsIgnoreCase("add"))
	  {
		String floor = null;
		if(args.length < 3)
		{
		  Block b = ((Player)sender).getLocation().getBlock();
		  Floor f = new Floor(b.getY() - 1, b.getWorld().getName());
		  if(!lift.floors.containsValue(f))
		  {
			sender.sendMessage(ChatColor.RED+"Automatic floor detection failed!");
			return true;
		  }
		  for(Entry<String, Floor> e: lift.floors.entrySet())
		  {
			Floor fl = e.getValue();
			if(fl.equals(f))
			{
			  floor = e.getKey();
			  break;
			}
		  }
		}
		else
		{
		  StringBuilder sb = new StringBuilder(args[2]);
		  for(int i = 3; i < args.length; i++)
		  {
			sb.append(" ");
			sb.append(args[i]);
		  }
		  floor = sb.toString();
		  if(!lift.floors.containsKey(floor))
		  {
			sender.sendMessage(ChatColor.RED+"Floor \""+ChatColor.YELLOW+floor+ChatColor.RED+"\" not found!");
			return true;
		  }
		}
		if(plugin.inputEdits.containsKey(player) ||
				plugin.inputRemoves.contains(player))
		{
		  sender.sendMessage(ChatColor.RED+"You're still editing a input!");
		  return true;
		}
		plugin.inputEdits.put(player, floor);
		sender.sendMessage(ChatColor.GOLD+"Now right-click the input block!");
	  }
	  else if(args[1].equalsIgnoreCase("del"))
	  {
		if(lift.inputs.isEmpty())
		{
		  sender.sendMessage(ChatColor.RED+"No input to remove!");
		  return true;
		}
		if(plugin.inputEdits.containsKey(player) ||
				plugin.inputRemoves.contains(player))
		{
		  sender.sendMessage(ChatColor.RED+"You're still editing a input!");
		  return true;
		}
		plugin.inputRemoves.add(player);
		sender.sendMessage(ChatColor.GOLD+"Now right-click the input block!");
	  }
	  else
	  {
		sendHelp(sender, "input");
		return true;
	  }
	}
	else if(args[0].equalsIgnoreCase("offline"))
	{
	  if(!plugin.editors.containsKey(player))
	  {
		sender.sendMessage(ChatColor.RED+"Start editing mode first!");
		return true;
	  }
	  if(args.length < 2)
	  {
		sendHelp(sender, "offline");
		return true;
	  }
	  Lift lift = plugin.lifts.get(plugin.editors.get(player));
	  if(args[1].equalsIgnoreCase("add"))
	  {
		if(plugin.offlineEdits.contains(player) ||
				plugin.offlineRemoves.contains(player))
		{
		  sender.sendMessage(ChatColor.RED+"You're still editing a offline input!");
		  return true;
		}
		plugin.offlineEdits.add(player);
		sender.sendMessage(ChatColor.GOLD+"Now right-click the offline input block!");
	  }
	  else if(args[1].equalsIgnoreCase("del"))
	  {
		if(lift.offlineInputs.isEmpty())
		{
		  sender.sendMessage(ChatColor.RED+"No offline input to remove!");
		  return true;
		}
		if(plugin.offlineEdits.contains(player) ||
				plugin.offlineRemoves.contains(player))
		{
		  sender.sendMessage(ChatColor.RED+"You're still editing a offline input!");
		  return true;
		}
		plugin.offlineRemoves.add(player);
		sender.sendMessage(ChatColor.GOLD+"Now right-click the offline input block!");
	  }
	  else
	  {
		sendHelp(sender, "offline");
		return true;
	  }
	}
	else if(args[0].equalsIgnoreCase("rename"))
	{
	  if(!plugin.editors.containsKey(player))
	  {
		sender.sendMessage(ChatColor.RED+"Start editing mode first!");
		return true;
	  }
	  if(args.length < 2)
	  {
		sender.sendMessage(ChatColor.RED+"No name given!");
		return true;
	  }
	  StringBuilder sb = new StringBuilder(args[1]);
	  for(int i = 2; i < args.length; i++)
	  {
		sb.append(" ");
		sb.append(args[i]);
	  }
	  int l = sb.length();
	  if(l > 15)
		sb.delete(16, l);
	  String nn = sb.toString().trim();
	  plugin.api.renameLift(plugin.editors.get(player), nn);
	  plugin.editors.put(player, nn);
	  sender.sendMessage(ChatColor.GREEN+"Lift renamed!");
	}
	else if(args[0].equalsIgnoreCase("build"))
	{
	  if(!plugin.editors.containsKey(player))
	  {
		sender.sendMessage(ChatColor.RED+"Start editing mode first!");
		return true;
	  }
	  if(plugin.builder.contains(player))
	  {
		plugin.builder.remove(player);
		plugin.api.sortLiftBlocks(plugin.editors.get(player));
		sender.sendMessage(ChatColor.GREEN+"Builder mode disabled!");
		return true;
	  }
	  plugin.builder.add(player);
	  sender.sendMessage(ChatColor.GOLD+"Now right-click the lift blocks!");
	  sender.sendMessage(ChatColor.GOLD+"Then use /v10lift build again to end the process!");
	}
	else if(args[0].equalsIgnoreCase("rope"))
	{
	  if(!plugin.editors.containsKey(player))
	  {
		sender.sendMessage(ChatColor.RED+"Start editing mode first!");
		return true;
	  }
	  if(args.length < 2)
	  {
		sendHelp(sender, "rope");
		return true;
	  }
	  if(args[1].equalsIgnoreCase("add"))
	  {
		if(plugin.ropeEdits.containsKey(player) || plugin.ropeRemoves.contains(player))
		{
		  sender.sendMessage(ChatColor.RED+"You're still editing ropes.");
		  return true;
		}
		plugin.ropeEdits.put(player, null);
		sender.sendMessage(ChatColor.GOLD+"Now right click the start and the end of your rope.");
	  }
	  else if(args[1].equalsIgnoreCase("del"))
	  {
		if(plugin.ropeEdits.containsKey(player) || plugin.ropeRemoves.contains(player))
		{
		  sender.sendMessage(ChatColor.RED+"You're still editing ropes.");
		  return true;
		}
		plugin.ropeRemoves.add(player);
		sender.sendMessage(ChatColor.GOLD+"Now right click your rope.");
	  }
	}
	else if(args[0].equalsIgnoreCase("door"))
	{
	  if(!plugin.editors.containsKey(player))
	  {
		sender.sendMessage(ChatColor.RED+"Start editing mode first!");
		return true;
	  }
	  if(plugin.doorEdits.containsKey(player))
	  {
		plugin.doorEdits.remove(player);
		sender.sendMessage(ChatColor.RED+"Door editor diabled!");
		return true;
	  }
	  Lift lift = plugin.lifts.get(plugin.editors.get(player));
	  String floor = null;
	  if(args.length < 3)
	  {
		Location loc = ((Player)sender).getLocation();
		Floor f = new Floor(loc.getBlockY() - 1, loc.getWorld().getName());
		if(!lift.floors.containsValue(f))
		{
		  sender.sendMessage(ChatColor.RED+"Automatic floor detection failed!");
		  return true;
		}
		for(Entry<String, Floor> e: lift.floors.entrySet())
		{
		  Floor fl = e.getValue();
		  if(fl.equals(f))
		  {
			floor = e.getKey();
			break;
		  }
		}
	  }
	  else
	  {
		StringBuilder sb = new StringBuilder(args[2]);
		for(int i = 3; i < args.length; i++)
		{
		  sb.append(" ");
		  sb.append(args[i]);
		}
		floor = sb.toString();
		if(!lift.floors.containsKey(floor))
		{
		  sender.sendMessage(ChatColor.RED+"Floor \""+ChatColor.YELLOW+floor+ChatColor.RED+"\" not found!");
		  return true;
		}
	  }
	  plugin.doorEdits.put(player, floor);
	  sender.sendMessage(ChatColor.GOLD+"Now right-click the door blocks!");
	  sender.sendMessage(ChatColor.GOLD+"Then use /v10lift door again to end the process!");
	}
	else if(args[0].equalsIgnoreCase("whitelist"))
	{
	  if(!plugin.editors.containsKey(player))
	  {
		sender.sendMessage(ChatColor.RED+"Start editing mode first!");
		return true;
	  }
	  if(args.length < 3)
	  {
		sendHelp(sender, "whitelist");
		return true;
	  }
	  Lift lift = plugin.lifts.get(plugin.editors.get(player));
	  Player wp = plugin.getServer().getPlayer(args[2]);
	  String wpn;
	  if(wp == null)
		wpn = args[2].toLowerCase();
	  else
		wpn = wp.getName().toLowerCase();
	  String floor = null;
	  if(args.length < 4)
	  {
		Block b = ((Player)sender).getLocation().getBlock();
		Floor f = new Floor(b.getY() - 1, b.getWorld().getName());
		if(!lift.floors.containsValue(f))
		{
		  sender.sendMessage(ChatColor.RED+"Automatic floor detection failed!");
		  return true;
		}
		for(Entry<String, Floor> e: lift.floors.entrySet())
		{
		  Floor fl = e.getValue();
		  if(fl.equals(f))
		  {
			floor = e.getKey();
			break;
		  }
		}
	  }
	  else
	  {
		StringBuilder sb = new StringBuilder(args[3]);
		for(int i = 4; i < args.length; i++)
		{
		  sb.append(" ");
		  sb.append(args[i]);
		}
		floor = sb.toString();
		if(!lift.floors.containsKey(floor))
		{
		  sender.sendMessage(ChatColor.RED+"Floor \""+ChatColor.YELLOW+floor+ChatColor.RED+"\" not found!");
		  return true;
		}
	  }
	  Floor f = lift.floors.get(floor);
	  if(args[1].equalsIgnoreCase("add"))
	  {
		if(f.whitelist.contains(wpn))
		  sender.sendMessage(ChatColor.RED+"Whitelist already contains "+wpn);
		else
		{
		  f.whitelist.add(wpn);
		  sender.sendMessage(ChatColor.GREEN+wpn+" added!");
		}
	  }
	  else if(args[1].equalsIgnoreCase("del"))
	  {
		if(!f.whitelist.contains(wpn))
		  sender.sendMessage(ChatColor.RED+"Whitelist don't contain "+wpn);
		else
		{
		  f.whitelist.remove(wpn);
		  sender.sendMessage(ChatColor.GREEN+wpn+" removed!");
		  if(f.whitelist.isEmpty())
			sender.sendMessage(ChatColor.YELLOW+"Whitelist empty, disabled!");
		}
	  }
	}
	else if(args[0].equalsIgnoreCase("whois"))
	{
	  if(args.length < 2)
	  {
		plugin.whoisReq.add(player);
		sender.sendMessage(ChatColor.GOLD+"Now right-click the block you want to check!");
	  }
	  else
	  {
		String lift;
		if(args.length == 2)
		  lift = args[1];
		else
		{
		  StringBuilder sb = new StringBuilder(args[1]);
		  for(int i = 2; i < args.length; i++)
			sb.append(' ').append(args[i]);
		  lift = sb.toString();
		}
		if(!plugin.api.isLift(lift))
		  sender.sendMessage(ChatColor.RED+"Lift \""+ChatColor.GOLD+lift+ChatColor.RED+"\" not found!");
		else
		  plugin.api.sendLiftInfo(((Player)sender), lift);
	  }
	}
	else if(args[0].equalsIgnoreCase("speed"))
	{
	  if(!plugin.editors.containsKey(player))
	  {
		sender.sendMessage(ChatColor.RED+"Start editing mode first!");
		return true;
	  }
	  if(args.length < 2)
	  {
		sendHelp(sender, "speed");
		return true;
	  }
	  Lift lift = plugin.lifts.get(plugin.editors.get(player));
	  try
	  {
		lift.speed = Integer.parseInt(args[1]);
		if(lift.speed < 1)
		  lift.speed = 1;
		sender.sendMessage(ChatColor.GREEN+"New lift speed: "+lift.speed);
	  }
	  catch(NumberFormatException e)
	  {
		sender.sendMessage(ChatColor.RED+"Invalid speed: "+args[1]);
		sendHelp(sender, "speed");
	  }
	}
	else if(args[0].equalsIgnoreCase("sound"))
	{
	  if(!plugin.editors.containsKey(player))
	  {
		sender.sendMessage(ChatColor.RED+"Start editing mode first!");
		return true;
	  }
	  Lift lift = plugin.lifts.get(plugin.editors.get(player));
	  if(lift.sound)
	  {
		lift.sound = false;
		sender.sendMessage(ChatColor.GOLD+"Sound mode disabled!");
	  }
	  else
	  {
		lift.sound = true;
		sender.sendMessage(ChatColor.GOLD+"Sound mode enabled");
	  }
	}
	else if(args[0].equalsIgnoreCase("realistic"))
	{
	  if(!plugin.editors.containsKey(player))
	  {
		sender.sendMessage(ChatColor.RED+"Start editing mode first!");
		return true;
	  }
	  Lift lift = plugin.lifts.get(plugin.editors.get(player));
	  if(lift.realistic)
	  {
		lift.realistic = false;
		sender.sendMessage(ChatColor.GOLD+"Realistic mode disabled!");
	  }
	  else
	  {
		lift.realistic = true;
		sender.sendMessage(ChatColor.GOLD+"Realistic mode enabled");
	  }
	}
	else if(args[0].equalsIgnoreCase("help"))
	{
	  if(args.length < 2)
		sendHelp(sender);
	  else
		sendHelp(sender, args[1]);
	}
	else if(args[0].equalsIgnoreCase("reset"))
	{
	  if(!plugin.hasPerm(sender, "v10lift.admin"))
	  {
		sender.sendMessage(ChatColor.RED+"You are not allowed to use this command!");
		return true;
	  }
	  System.out.print("DEBUG: Resetting");
	  BukkitScheduler bs = plugin.getServer().getScheduler();
	  for(Entry<String, Lift> e: plugin.lifts.entrySet())
	  {
		String lift = e.getKey();
		System.out.print("DEBUG: Stopping "+lift);
		if(plugin.movingTasks.containsKey(lift))
		  bs.cancelTask(plugin.movingTasks.get(lift));
		System.out.print("DEBUG: Removing queue of "+lift);
		e.getValue().queue = null;
		System.out.print("DEBUG: Re-sorting lift blocks of "+lift);
		plugin.api.sortLiftBlocks(lift);
	  }
	  System.out.print("DEBUG: Clearing movingTasks");
	  plugin.movingTasks.clear();
	  System.out.print("DEBUG: Reloading config");
	  plugin.reloadConfig();
	  System.out.print("DEBUG: Loading...");
	  plugin.load();
	  if(plugin.au != null)
		try
	  	{
		  System.out.print("DEBUG: Resetting AutoUpdate...");
		  plugin.au.resetConfig();
		}
	  	catch(FileNotFoundException e1)
	  	{
		  e1.printStackTrace();
		}
	  System.out.print("DEBUG: Saving config...");
	  plugin.saveConfig();
	  sender.sendMessage(ChatColor.YELLOW+"Resetted!");
	}
	else if(args[0].equalsIgnoreCase("repair"))
	{
	  if(!plugin.hasPerm(sender, "v10lift.repair.master"))
	  {
		sender.sendMessage(ChatColor.RED+"You are not allowed to use this command!");
		return true;
	  }
	  if(args.length < 2)
	  {
		sendHelp(sender, "repair");
		return true;
	  }
	  StringBuilder sb = new StringBuilder(args[1]);
	  for(int i = 2; i < args.length; i++)
		sb.append(" ").append(args[i]);
	  String lift = sb.toString();
	  if(!plugin.lifts.containsKey(lift))
	  {
		sender.sendMessage(ChatColor.RED+"Lift \""+ChatColor.YELLOW+lift+ChatColor.RED+"\" doesn't exist!");
		return true;
	  }
	  if(!plugin.api.isDefective(lift))
	  {
		sender.sendMessage("The lift doesn't seem to be defective!");
		return true;
	  }
	  Player p = (Player)sender;
	  if(p.getGameMode() == GameMode.SURVIVAL && plugin.masterAmount > 0)
	  {
		PlayerInventory pi = p.getInventory();
		if(!pi.contains(plugin.masterItem, plugin.masterAmount))
		{
		  sender.sendMessage(ChatColor.RED+"You need "+plugin.masterAmount+"x "+Material.getMaterial(plugin.masterItem).toString().replace('_', ' ').toLowerCase());
		  return true;
		}
		pi.remove(new ItemStack(plugin.masterItem, plugin.masterAmount));
	  }
	  plugin.api.setDefective(lift, false);
	  sender.sendMessage(ChatColor.GREEN+"Lift repaired!");
	}
	else
	  sendHelp(sender);
	return true;
  }
  
  private void sendHelp(CommandSender sender)
  {
	sendHelp(sender, null);
  }
  
  private void sendHelp(CommandSender sender, String section)
  {
	if(section == null)
	  section = "main";
	
	if(section.equals("create"))
	{
	  sender.sendMessage("Use /v10lift create");
	  sender.sendMessage("to create a new lift.");
	}
	else if(section.equals("delete"))
	{
	  sender.sendMessage("Use /v10lift delete your lift");
	  sender.sendMessage("to delete \"your lift\".");
	}
	else if(section.equals("edit"))
	{
	  sender.sendMessage("Use /v10lift edit \"my lift\"");
	  sender.sendMessage("to remove/delete floors and input blocks from the lift \"my lift\".");
	}
	else if(section.equals("floor"))
	{
	  sender.sendMessage("Use /v10lift floor add floor name");
	  sender.sendMessage("to create a new floor called \"floor name\".");
	  sender.sendMessage("Use /v10lift floor del floor name");
	  sender.sendMessage("to delete the floor called \"floor name\".");
	  sender.sendMessage("Use /v10lift floor rename floor name new name");
	  sender.sendMessage("to rename the floor \"floor name\" to \"new name\".");
	}
	else if(section.equalsIgnoreCase("input"))
	{
	  sender.sendMessage("Use /v10lift input add <floor name>");
	  sender.sendMessage("to create a new input for the floor \"floor name\".");
	  sender.sendMessage("If no floor name is given it will try to use the floor you're standing at.");
	  sender.sendMessage("Use /v10lift input del");
	  sender.sendMessage("to delete an input block.");
	}
	else if(section.equalsIgnoreCase("offline"))
	{
	  sender.sendMessage("Use /v10lift offline add");
	  sender.sendMessage("to create a new offline input.");
	  sender.sendMessage("Use /v10lift offline del");
	  sender.sendMessage("to delete a offline input block.");
	}
	else if(section.equals("build"))
	{
	  sender.sendMessage("Use /v10lift build");
	  sender.sendMessage("to start or stop the building mode.");
	  sender.sendMessage("In that mode you can change the blocks of a still builded lift.");
	}
	else if(section.equals("rename"))
	{
	  sender.sendMessage("Use /v10lift rename <new name>");
	  sender.sendMessage("to rename the lift to \"new name\"");
	}
	else if(section.equals("rope"))
	{
	  sender.sendMessage("Use /v10lift rope add");
	  sender.sendMessage("to add a rope to your lift.");
	  sender.sendMessage("Use /v10lift rope del");
	  sender.sendMessage("to remove a rope from your lift.");
	}
	else if(section.equals("door"))
	{
	  sender.sendMessage("Use /v10lift door floor name");
	  sender.sendMessage("to edit a door for the floor\"floor name\".");
	  sender.sendMessage("Defaults to the floor you are standing at if no floor name given.");
	  sender.sendMessage("Use /v10lift door");
	  sender.sendMessage("to stop the door edit mode.");
	}
	else if(section.equalsIgnoreCase("abort"))
	{
	  sender.sendMessage("Use /v10lift abort");
	  sender.sendMessage("to cancel your actuall lift creation, (offline) input or floor edit.");
	}
	else if(section.equalsIgnoreCase("whitelist"))
	{
	  sender.sendMessage("Use /v10lift whitelist add player <floor name>");
	  sender.sendMessage("to add a player to the whitelist of floor \"floor name\".");
	  sender.sendMessage("Use /v10lift del player <floor name>");
	  sender.sendMessage("to remove a player from the whitelist of floor \"floor name\".");
	  sender.sendMessage("If no floor name is given it will try to use the floor you're standing at.");
	}
	else if(section.equalsIgnoreCase("speed"))
	{
	  sender.sendMessage("Use /v10lift sound");
	  sender.sendMessage("to toggle sound mode.");
	}
	else if(section.equalsIgnoreCase("speed"))
	{
	  sender.sendMessage("Use /v10lift speed X");
	  sender.sendMessage("to set the lift speed to X ticks (1 second = 20 ticks).");
	}
	else if(section.equalsIgnoreCase("realistic"))
	{
	  sender.sendMessage("Use /v10lift realistic");
	  sender.sendMessage("to toggle realistic mode.");
	}
	else if(section.equalsIgnoreCase("repair"))
	{
	  sender.sendMessage("Use /v10lift repair lift name");
	  sender.sendMessage("to repair the lift \"lift name\"");
	}
	else
	{
	  sender.sendMessage("Use /v10lift create");
	  sender.sendMessage("/v10lift delete");
	  sender.sendMessage("/v10lift edit");
	  sender.sendMessage("/v10lift floor");
	  sender.sendMessage("/v10lift input");
	  sender.sendMessage("/v10lift offline");
	  sender.sendMessage("/v10lift build");
	  sender.sendMessage("/v10lift rope");
	  sender.sendMessage("/v10lift door");
	  sender.sendMessage("/v10lift abort");
	  sender.sendMessage("/v10lift whois");
	  sender.sendMessage("/v10lift sound");
	  sender.sendMessage("/v10lift speed");
	  sender.sendMessage("/v10lift realistic");
	  sender.sendMessage("/v10lift whitelist");
	  if(plugin.hasPerm(sender, "v10lift.admin"))
	    sender.sendMessage("/v10lift reset");
	  if(plugin.hasPerm(sender, "v10lift.repair.master"))
		sender.sendMessage("/v10lift repair");
	  sender.sendMessage("For more help try /v10lift help command");
	}
  }
}
