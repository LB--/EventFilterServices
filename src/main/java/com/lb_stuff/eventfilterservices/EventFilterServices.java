package com.lb_stuff.eventfilterservices;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public enum EventFilterServices
{
	ChatFilter(ChatFilterService.getInst()),
	PotionFilter(PotionFilterService.getInst());

	private final EventFilterService inst;
	private EventFilterServices(EventFilterService efs)
	{
		inst = efs;
	}

	public EventFilterService get()
	{
		return inst;
	}

	private final Map<Plugin, DependencyType> dependees = new HashMap<>();
	public void depend(Plugin plugin, DependencyType type)
	{
		if(type == null)
		{
			type = DependencyType.NONE;
		}
		if(getDependencyType(plugin) == type)
		{
			return;
		}
		Bukkit.getPluginManager().callEvent(new DependencyChangeEvent(plugin, this, type));
		if(type != DependencyType.NONE)
		{
			dependees.put(plugin, type);
		}
		else
		{
			dependees.remove(plugin);
		}
	}
	public Set<Plugin> getDependees(DependencyType type)
	{
		if(type == null)
		{
			return dependees.keySet();
		}
		Set<Plugin> plugins = new HashSet<>();
		if(type == DependencyType.NONE)
		{
			for(Plugin p : Bukkit.getPluginManager().getPlugins())
			{
				if(!dependees.containsKey(p))
				{
					plugins.add(p);
				}
			}
		}
		else for(Map.Entry<Plugin, DependencyType> e : dependees.entrySet())
		{
			if(e.getValue() == type)
			{
				plugins.add(e.getKey());
			}
		}
		return plugins;
	}
	public DependencyType getDependencyType(Plugin plugin)
	{
		if(dependees.containsKey(plugin))
		{
			return dependees.get(plugin);
		}
		return DependencyType.NONE;
	}

	public static enum DependencyType
	{
		NONE,
		OPTIONAL,
		REQUIRED,
		INCOMPATIBLE
	}
	public static final class DependencyChangeEvent extends Event
	{
		private final Plugin plugin;
		private final EventFilterServices service;
		private final DependencyType type;
		private DependencyChangeEvent(Plugin p, EventFilterServices s, DependencyType t)
		{
			plugin = p;
			service = s;
			type = t;
		}

		public Plugin getPlugin()
		{
			return plugin;
		}
		public EventFilterServices getServiceType()
		{
			return service;
		}
		public DependencyType getDependencyType()
		{
			return type;
		}

		private static final HandlerList handlers = new HandlerList();
		@Override
		public HandlerList getHandlers()
		{
			return handlers;
		}
		public static HandlerList getHandlerList()
		{
			return handlers;
		}
	}
}
