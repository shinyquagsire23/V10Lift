package de.V10lator.V10lift;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import net.minecraft.server.WorldServer;

import org.apache.commons.lang.Validate;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

class MoveLift implements Runnable
{
  private final V10lift plugin;
  
  private final HashSet<Integer> antiCopy = new HashSet<Integer>();
  private final int cm = Material.CHEST.getId();
  private final long speed;
  private final int ft;
  private final String ln;
  
  MoveLift(V10lift plugin, String ln, long speed)
  {
	this.plugin = plugin;
	this.ln = ln;
	this.speed = speed;
	
	if(speed > 32L)
	  ft = 1;
	else if(speed > 16L)
	  ft = 2;
	else if(speed > 8L)
	  ft = 4;
	else if(speed > 4L)
	  ft = 8;
	else if(speed > 2L)
	  ft = 16;
	else if(speed > 1L)
	  ft = 32;
	else
	  ft = 64;
	
	antiCopy.add(Material.REDSTONE_TORCH_OFF.getId());
	antiCopy.add(Material.REDSTONE_TORCH_ON.getId());
	antiCopy.add(Material.DIODE_BLOCK_OFF.getId());
	antiCopy.add(Material.DIODE_BLOCK_ON.getId());
	antiCopy.add(Material.REDSTONE_WIRE.getId());
	antiCopy.add(Material.STONE_BUTTON.getId());
	antiCopy.add(Material.TORCH.getId());
	antiCopy.add(Material.TRAP_DOOR.getId());
	antiCopy.add(Material.WOOD_PLATE.getId());
	antiCopy.add(Material.STONE_PLATE.getId());
	antiCopy.add(Material.SIGN_POST.getId());
	antiCopy.add(Material.WALL_SIGN.getId());
	antiCopy.add(Material.RAILS.getId());
	antiCopy.add(Material.POWERED_RAIL.getId());
	antiCopy.add(Material.LADDER.getId());
	antiCopy.add(Material.DETECTOR_RAIL.getId());
  }
  
  public FloatingBlock spawnFallingBlock(Location location, org.bukkit.Material material, byte data) throws IllegalArgumentException {
      Validate.notNull(location, "Location cannot be null");
      Validate.notNull(material, "Material cannot be null");
      Validate.isTrue(material.isBlock(), "Material must be a block");

      
      
      double x = location.getBlockX() + 0.5;
      double y = location.getBlockY() + 1.5;
      double z = location.getBlockZ() + 0.5;
      WorldServer world = ((CraftWorld)location.getWorld()).getHandle();

      FloatingBlock entity = new FloatingBlock(world, x, y, z, material.getId(), data);
      entity.c = 1; // ticksLived

      world.addEntity(entity, SpawnReason.CUSTOM);
      return entity;
  }
  
  @SuppressWarnings("unchecked")
  public void run()
  {
	Server s = plugin.getServer();
	World tw;
	
	Iterator<LiftBlock> iter;
	ArrayList<LiftBlock> tb = new ArrayList<LiftBlock>();
	Block block;
	World world;
	World world2 = null;
	Location loc;
	BlockState bs;
	boolean by;
	int y;
	Chest c;
	V10Entity v10ent;
	Iterator<V10Entity> veiter;
	Sign sign;
	LiftBlock lb;
	Lift lift;
	LiftSign ls;
	Inventory inv;
	ItemStack is;
	ItemStack[] isa;
	
	lift = plugin.lifts.get(ln);
	if(lift == null)
	{
	  stopMe();
	  return;
	}
	if(lift.queue.isEmpty() || lift.offline)
	{
	  lift.queue = null;
	  stopMe();
	  return;
	}
	
	if(plugin.editors.containsValue(ln) || lift.defective)
	  return;
	
	//TODO: EXPERIMENTAL: Lift freeze in unloaded chunk...
	if(lift.counter > 0)
	{
	  lift.counter--;
	  return;
	}
	
	lb = lift.blocks.first();
	world = s.getWorld(lb.world);
	if(world == null)
	{
	  lift.counter = ft;
	  return;
	}
	loc = new Location(world, lb.x, lb.y, lb.z);
	if(!loc.getChunk().isLoaded())
	{
	  lift.counter = ft;
	  return;
	}
	lb = lift.blocks.last();
	world = s.getWorld(lb.world);
	if(world == null)
	{
	  lift.counter = ft;
	  return;
	}
	loc = new Location(world, lb.x, lb.y, lb.z);
	if(!loc.getChunk().isLoaded())
	{
	  lift.counter = ft;
	  return;
	}
	
	if(plugin.chanceOfDefect > 0.0D)
	{
	  y = plugin.rand.nextInt(100);
	  double chance;
	  if(y < 100)
	  {
		long co = plugin.rand.nextLong();
		if(co < 0)
		  co = -co;
		chance = Double.parseDouble(y+"."+co);
	  }
	  else
		chance = y;
	  if(chance < plugin.chanceOfDefect)
	  {
		plugin.api.setDefective(ln, true);
		return;
	  }
	}
	
	Iterator<Entry<String, Floor>> quiter = lift.queue.entrySet().iterator();
	Entry<String, Floor> floor = quiter.next();
	Floor to = floor.getValue();
	String fl = floor.getKey();
	boolean up = false;
	boolean down = false;
	if(plugin.v10vAPI == null)
	{
	  if(lift.y < to.y)
		up = true;
	  else if(lift.y > to.y)
		down = true;
	}
	else
	{
	  String w = lift.world;
	  if(w.equals(to.world))
	  {
		if(lift.y < to.y)
		  up = true;
		else if(lift.y > to.y)
		  down = true;
	  }
	  else
	  {
		while(true)
		{
		  tw = plugin.v10vAPI.getUpperWorld(w);
		  if(tw == null)
			break;
		  w = tw.getName();
		  if(w.equals(to.world))
		  {
			up = true;
			break;
		  }
		}
		if(!up)
		{
		  w = lift.world;
		  while(true)
		  {
			tw = plugin.v10vAPI.getLowerWorld(w);
			if(tw == null)
			  break;
			w = tw.getName();
			if(w.equals(to.world))
			{
			  down = true;
			  break;
			}
		  }
		}
	  }
	}
	
	String tmpw = lift.world;
	if(up)
	{
	  if(!plugin.api.closeDoor(lift))
		return;
	  //Ropes:
	  for(V10Rope rope: lift.ropes)
	  {
		if(rope.currentWorld.equals(rope.startWorld) && rope.currently > rope.maxY)
		{
		  plugin.getServer().getLogger().info("["+plugin.getName()+"] Lift \""+ln+"\" reaches the upper rope end but won't stop!");
		  plugin.api.setDefective(ln, true);
		  lift.toMove.clear();
		  quiter.remove();
		  return;
		}
		world = s.getWorld(rope.currentWorld);
		block = world.getBlockAt(rope.x, rope.currently, rope.z);
		block.setType(Material.AIR);
		if(plugin.v10vAPI != null && rope.currently > plugin.v10vAPI.getMaxY(world))
		{
		  tw = plugin.v10vAPI.getUpperWorld(world);
		  rope.currentWorld = tw.getName();
		  rope.currently = plugin.v10vAPI.getMinY(tw);
		}
		else
		  rope.currently++;
	  }
	  iter = lift.blocks.iterator();
	  //TODO: TB handling... tb = new ArrayList<LiftBlock>();
	  while(iter.hasNext())
	  {
		lb = iter.next();
		if(antiCopy.contains(lb.type))
		{
		  tb.add(lb);
		  iter.remove();
		  block = s.getWorld(lb.world).getBlockAt(lb.x, lb.y, lb.z);
		  //TODO lb.data = block.getData();
		  block.setType(Material.AIR);
		  lb.y++;
		}
	  }
	  boolean wc = false;
	  boolean hasFloatingBlocks = false;
	  try
	  {
		  hasFloatingBlocks = lift.movingBlocks.length > 0;
	  }
	  catch(Exception e)
	  {
		  lift.movingBlocks = new FloatingBlock[lift.blocks.size()];
	  }
	  int currentIndex = -1;
	  for(LiftBlock lib: lift.blocks.descendingSet())
	  {
		  currentIndex++;
		world = s.getWorld(lib.world);
		block = world.getBlockAt(lib.x, lib.y, lib.z);
		//TODO lb.data = block.getData();
		if(lib.type == cm && lib.serializedItemStacks == null)
		{
		  c = (Chest)block.getState();
		  inv = c.getInventory();
		  isa = inv.getContents();
		  by = false;
		  lib.serializedItemStacks = new Map[isa.length];
		  for(int i = 0; i < isa.length; i++)
		  {
			is = isa[i];
			if(is != null)
			{
			  by = true;
			  lib.serializedItemStacks[i] = is.serialize();
			}
		  }
		  if(by)
		  {
			inv.clear();
			c.update();
		  }
		  else
			lib.serializedItemStacks = null;
		}
		block.setType(Material.AIR); //remove block to start lifting
		FloatingBlock f;
		if(!hasFloatingBlocks)
		{
			try
			{
				f = spawnFallingBlock(block.getLocation(), Material.getMaterial(lib.type), lib.data);
				lift.movingBlocks[currentIndex] = f;
			}
			catch(Exception e)
			{
				lift.movingBlocks = new FloatingBlock[lift.blocks.size()];
				f = spawnFallingBlock(block.getLocation(), Material.getMaterial(lib.type), lib.data);
				lift.movingBlocks[currentIndex] = f;
			}
		}
		else
		{
			f = lift.movingBlocks[currentIndex];
			
		}
		//if(f.getVelocity().getY() < 0)
		f.setVelocity(new Vector(0,0.01,0));
		if(f.getLocation().getBlockY() >= lib.y + 1 && f.getLocation().getBlockY() <= to.y)
			//lib.y = f.getLocation().getBlockY();
			lib.y++;
		
		//lib.y = f.getLocation().getBlockY();
		//if(f.getLocation().getY() > to.y - 1)
			//f.setLocation(new Location(f.getWorld(), f.getLocation().getX(),(double)to.y - 1,f.getLocation().getZ()));
		if(plugin.v10vAPI != null)
		{
		  tw = plugin.v10vAPI.getUpperWorld(world);
		  if(tw != null)
		  {
			if(lib.y > plugin.v10vAPI.getMaxY(world))
			{
			  world2 = world;
			  world = tw;
			  lib.world = world.getName();
			  tmpw = lib.world;
			  lib.y = plugin.v10vAPI.getMinY(tmpw);
			  wc = true;
			}
		  }
		}
		block = world.getBlockAt(lib.x, lib.y, lib.z);
		//if(lib.y == to.y )//|| f.getLocation().getY() >= to.y - 0.5)
		System.out.println(f.getLocation().getY() + ", " + lib.y + ", " + to.y);
		if(f.getLocation().getY() >= to.y * 2 - 0.5 && lib.y == to.y * 2)// - 0.5)
		{
			lib.y = to.y;
			block.setTypeIdAndData(lib.type, lib.data, true); //done lifting that block
			int nullCount = 0;
			f.die();
			
			lift.movingBlocks[currentIndex] = null;
			for(FloatingBlock b : lift.movingBlocks)
				if(b == null)
					nullCount++;
			if(nullCount == lift.blocks.size())
			{
				for(FloatingBlock b : lift.movingBlocks)
					if(b != null)
						b.die();
				lift.movingBlocks = null;
			}
		}
		
		if(world2 != null)
		  world = world2;
		
		lb = lift.blocks.first();
		for(Entity ent: world.getBlockAt(lib.x, lib.y, lib.z).getChunk().getEntities())
		{
		  v10ent = new V10Entity(ent, null, 0);
		  if(lift.toMove.contains(v10ent))
			continue;
		  loc = ent.getLocation();
		  y = loc.getBlockY();
		  if(y == lib.y)
			by = true;
		  else if(y + 1 == lib.y)
		  {
			by = true;
			y++;
		  }
		  else
			by = false;
		  if(by &&
				  loc.getBlockX() == lib.x &&
				  loc.getBlockZ() == lib.z)
		  {
			loc.setY(loc.getY() + 1);
			if(plugin.v10vAPI != null)
			{
			  if(!plugin.v10vAPI.hasCooldown(ent))
				plugin.v10vAPI.addCooldown(ent, speed);
			  tw = plugin.v10vAPI.getUpperWorld(world);
			  if(tw != null && y + 1 > plugin.v10vAPI.getMaxY(world))
			  {
				loc.setWorld(tw);
				y = plugin.v10vAPI.getMinY(tw);
				loc.setY(y);
				plugin.v10vAPI.teleport(ent, loc);
				lift.toMove.add(new V10Entity(ent, loc, y));
			  }
			  else
				ent.teleport(loc);
			}
			else
			  ent.teleport(loc);
		  }
		}
	  }//End of block foreach
	  veiter = lift.toMove.iterator();
	  while(veiter.hasNext())
	  {
		v10ent = veiter.next();
		if(v10ent.step > 0)
		{
		  v10ent.moveUp();
		  if(v10ent.step > 16)
			veiter.remove();
		}
		v10ent.step++;
	  }
	  for(LiftBlock lib: tb)
	  {
		if(plugin.v10vAPI != null)
		{
		  tw = plugin.v10vAPI.getUpperWorld(lib.world);
		  if(tw != null)
		  {
			if(lib.y > plugin.v10vAPI.getMaxY(lib.world))
			{
			  lib.world = tw.getName();
			  tmpw = lib.world;
			  lib.y = plugin.v10vAPI.getMinY(tmpw);
			  wc = true;
			}
		  }
		}
		if(lib.y == to.y)
		{
			block = s.getWorld(lib.world).getBlockAt(lib.x, lib.y, lib.z);
			block.setTypeIdAndData(lib.type, lib.data, true);
			lift.blocks.add(lib);
			if(lib.lines != null)
			{
				bs = block.getState();
				if(bs instanceof Sign)
				{
					sign = (Sign)bs;
					for(int i = 0; i < 3; i++)
					{
						sign.setLine(i, lib.lines[i]);
						if(i == 0 && lib.lines[i].equalsIgnoreCase(plugin.signText) &&
								lib.lines[1].equals(ln))
						{
							sign.setLine(1, ln);
							sign.setLine(3, ChatColor.GOLD+""+fl);
							break;
						}
					}
					sign.update(true);
				}
			}
		}
	  }
	  if(plugin.v10vAPI != null && !lift.world.equals(tmpw))
	  {
		lift.world = tmpw;
		lift.y = plugin.v10vAPI.getMinY(lift.world);
	  }
	  else if(!wc)
		lift.y++;
	  Iterator<LiftSign> liter = lift.signs.iterator();
	  while(liter.hasNext())
	  {
		ls = liter.next();
		if(ls.state == 1)
		  continue;
		bs = s.getWorld(ls.world).getBlockAt(ls.x, ls.y, ls.z).getState();
		if(!(bs instanceof Sign))
		{
		  s.getLogger().info("["+plugin.getName()+"] Wrong sign deleted at: "+ls.x+", "+ls.y+", "+ls.z+" in world "+ls.world);
		  liter.remove();
		  continue;
		}
		sign = (Sign)bs;
		if(ls.type == 0)
		  sign.setLine(3, ChatColor.GREEN+"up");
		else
		  sign.setLine(3, ChatColor.GRAY+ChatColor.stripColor(sign.getLine(3)));
		sign.update();
		ls.state = 1;
	  }
	}
	else if(down)
	{
	  if(!plugin.api.closeDoor(lift))
		return;
	  iter = lift.blocks.iterator();
	  while(iter.hasNext())
	  {
		lb = iter.next();
		if(antiCopy.contains(lb.type))
		{
		  tb.add(lb);
		  iter.remove();
		  block = s.getWorld(lb.world).getBlockAt(lb.x, lb.y, lb.z);
		  //TODO: lb.data = block.getData();
		  block.setType(Material.AIR);
		  lb.y--;
		}
	  }
	  for(LiftBlock lib: lift.blocks)
	  {
		world = s.getWorld(lib.world);
		block = world.getBlockAt(lib.x, lib.y, lib.z);
		//TODO lb.data = block.getData();
		if(lib.type == cm && lib.serializedItemStacks == null)
		{
		  c = (Chest)block.getState();
		  inv = c.getInventory();
		  isa = inv.getContents();
		  by = false;
		  lib.serializedItemStacks = new Map[isa.length];
		  for(int i = 0; i < isa.length; i++)
		  {
			is = isa[i];
			if(is != null)
			{
			  by = true;
			  lib.serializedItemStacks[i] = is.serialize();
			}
		  }
		  if(by)
		  {
			inv.clear();
			c.update();
		  }
		  else
			lib.serializedItemStacks = null;
		}
		block.setType(Material.AIR);
		lib.y--;
		y = lib.y;
		if(plugin.v10vAPI != null)
		{
		  tw = plugin.v10vAPI.getLowerWorld(world);
		  if(tw != null)
		  {
			if(lib.y < plugin.v10vAPI.getMinY(world))
			{
			  world2 = world;
			  world = tw;
			  lib.world = world.getName();
			  tmpw = lib.world;
			  lib.y = plugin.v10vAPI.getMaxY(tmpw);
			}
		  }
		}
		block = world.getBlockAt(lib.x, lib.y, lib.z);
		block.setTypeIdAndData(lib.type, lib.data, true);
		if(plugin.v10vAPI != null)
		{
		  if(world2 != null)
		  {
			for(Entity ent: world2.getBlockAt(lib.x, lib.y, lib.z).getChunk().getEntities())
			{
			  v10ent = new V10Entity(ent, null, 0);
			  if(lift.toMove.contains(v10ent))
				continue;
			  loc = ent.getLocation();
			  if(y == loc.getBlockY() - 1 || y == loc.getBlockY() - 2 &&
					  loc.getBlockX() == lib.x &&
					  loc.getBlockZ() == lib.z)
			  {
				if(!plugin.v10vAPI.hasCooldown(ent))
				  plugin.v10vAPI.addCooldown(ent, speed);
				tw = plugin.v10vAPI.getLowerWorld(world2);
				if(tw != null && y < plugin.v10vAPI.getMinY(world2))
				{
				  y = plugin.v10vAPI.getMaxY(tw);
				  loc.setY(y + 1);
				  loc.setWorld(tw);
				  plugin.v10vAPI.teleport(ent, loc);
				  lift.toMove.add(new V10Entity(ent, loc, y + 1));
				}
			  }
			}
		  }
		}
	  }
	  veiter = lift.toMove.iterator();
	  while(veiter.hasNext())
	  {
		v10ent = veiter.next();
		if(v10ent.step> 0)
		{
		  v10ent.moveDown();
		  if(v10ent.step > 16)
			veiter.remove();
		}
		v10ent.step++;
	  }
	  for(LiftBlock lib: tb)
	  {
		if(plugin.v10vAPI != null)
		{
		  tw = plugin.v10vAPI.getLowerWorld(lib.world);
		  if(tw != null)
		  {
			if(lib.y < plugin.v10vAPI.getMinY(lib.world))
			{
			  lib.world = tw.getName();
			  tmpw = lib.world;
			  lib.y = plugin.v10vAPI.getMaxY(tmpw);
			}
		  }
		}
		block = s.getWorld(lib.world).getBlockAt(lib.x, lib.y, lib.z);
		block.setTypeIdAndData(lib.type, lib.data, true);
		lift.blocks.add(lib);
		if(lib.lines != null)
		{
		  bs = block.getState();
		  if(bs instanceof Sign)
		  {
			sign = (Sign)bs;
			for(int i = 0; i < 3; i++)
			  sign.setLine(i, lib.lines[i]);
			sign.update(true);
		  }
		}
		if(lib.lines != null)
		{
		  bs = block.getState();
		  if(bs instanceof Sign)
		  {
			sign = (Sign)bs;
			for(int i = 0; i < 3; i++)
			{
			  sign.setLine(i, lib.lines[i]);
			  if(i == 0 && lib.lines[i].equalsIgnoreCase(plugin.signText) &&
					  lib.lines[1].equals(ln))
			  {
				sign.setLine(1, ln);
				sign.setLine(3, ChatColor.GOLD+""+fl);
				break;
			  }
			}
			sign.update(true);
		  }
		}
	  }
	  if(plugin.v10vAPI != null && !lift.world.equals(tmpw))
	  {
		lift.world = tmpw;
		lift.y = plugin.v10vAPI.getMaxY(lift.world);
	  }
	  else
		lift.y--;
	  Iterator<LiftSign> liter = lift.signs.iterator();
	  while(liter.hasNext())
	  {
		ls = liter.next();
		if(ls.state == 2)
		  continue;
		bs = s.getWorld(ls.world).getBlockAt(ls.x, ls.y, ls.z).getState();
		if(!(bs instanceof Sign))
		{
		  s.getLogger().info("["+plugin.getName()+"] Wrong sign deleted at: "+ls.x+", "+ls.y+", "+ls.z+" in world "+ls.world);
		  liter.remove();
		  continue;
		}
		sign = (Sign)bs;
		if(ls.type == 0)
		  sign.setLine(3, ChatColor.GREEN+"down");
		else
		  sign.setLine(3, ChatColor.GRAY+ChatColor.stripColor(sign.getLine(3)));
		sign.update();
		  ls.state = 2;
	  }
	  //Ropes:
	  for(V10Rope rope: lift.ropes)
	  {
		if(rope.currentWorld.equals(rope.startWorld) && rope.currently < rope.minY)
		{
		  plugin.getServer().getLogger().info("["+plugin.getName()+"] Lift \""+ln+"\" reaches the upper rope end but won't stop!");
		  plugin.api.setDefective(ln, true);
		  lift.toMove.clear();
		  quiter.remove();
		  rope.currently--;
		  block = world.getBlockAt(rope.x, rope.currently, rope.z);
		  block.setTypeId(rope.id);
		  return;
		}
		world = s.getWorld(rope.currentWorld);
		if(plugin.v10vAPI != null && rope.currently < plugin.v10vAPI.getMinY(world))
		{
		  tw = plugin.v10vAPI.getLowerWorld(world);
		  rope.currentWorld = tw.getName();
		  rope.currently = plugin.v10vAPI.getMaxY(tw);
		  world = tw;
		}
		else
		  rope.currently--;
		block = world.getBlockAt(rope.x, rope.currently, rope.z);
		block.setTypeId(rope.id);
	  }
	}
	else
	{
	  lift.toMove.clear();
	  quiter.remove();
	  bs = null;
	  for(LiftBlock lib: lift.blocks)
	  {
		bs = s.getWorld(lib.world).getBlockAt(lib.x, lib.y, lib.z).getState();
		if(!(bs instanceof Sign))
		{
		  if(bs instanceof Chest && lib.serializedItemStacks != null)
		  {
			isa = new ItemStack[lib.serializedItemStacks.length];
			by = false;
			for(int i = 0; i < lib.serializedItemStacks.length; i++)
			  if(lib.serializedItemStacks[i] != null)
			  {
				isa[i] = ItemStack.deserialize(lib.serializedItemStacks[i]);
				by = true;
			  }
			if(by)
			{
			  c = (Chest)bs;
			  c.getInventory().setContents(isa);
			  c.update();
			}
			lib.serializedItemStacks = null;
		  }
		  continue;
		}
		sign = (Sign)bs;
		if(!sign.getLine(0).equalsIgnoreCase(plugin.signText))
		  continue;
		sign.setLine(1, ln);
		sign.setLine(3, ChatColor.GREEN+fl);
		sign.update();
	  }
	  Iterator<LiftSign> liter = lift.signs.iterator();
	  while(liter.hasNext())
	  {
		ls = liter.next();
		if(ls.state == 0)
		  continue;
		bs = s.getWorld(ls.world).getBlockAt(ls.x, ls.y, ls.z).getState();
		if(!(bs instanceof Sign))
		{
		  s.getLogger().info("["+plugin.getName()+"] Wrong sign deleted at: "+ls.x+", "+ls.y+", "+ls.z+" in world "+ls.world);
		  liter.remove();
		  continue;
		}
		sign = (Sign)bs;
		if(ls.type == 0)
		  sign.setLine(3, ChatColor.GREEN+fl);
		else
		{
		  String l3 = ChatColor.stripColor(sign.getLine(3));
		  if(!fl.equals(l3))
			sign.setLine(3, ChatColor.GRAY+l3);
		  else
			sign.setLine(3, ChatColor.GREEN+l3);
		}
		sign.update();
		ls.state = 0;
	  }
	  plugin.api.openDoor(lift, ln, to);
	  if(lift.realistic)
		lift.counter = ft;
	  if(lift.sound)
	  {
		loc = bs.getLocation();
		world.playSound(loc, Sound.ORB_PICKUP, 2.0F, 63.0F);
	  }
	}
  }
  
  private void stopMe()
  {
	plugin.getServer().getScheduler().cancelTask(plugin.movingTasks.get(ln));
	plugin.movingTasks.remove(ln);
  }
}
