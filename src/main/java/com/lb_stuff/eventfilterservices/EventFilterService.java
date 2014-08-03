package com.lb_stuff.eventfilterservices;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class EventFilterService implements Listener
{
	protected final EventFilterServicesPlugin inst;
	/*default*/ EventFilterService()
	{
		JavaPlugin p = JavaPlugin.getProvidingPlugin(getClass());
		if(p instanceof EventFilterServicesPlugin)
		{
			inst = (EventFilterServicesPlugin)p;
		}
		else
		{
			throw new UnsupportedOperationException();
		}
	}

	/*default*/ abstract void register(ServicePriority priority);
	/*default*/ void unregister()
	{
		Bukkit.getServicesManager().unregister(this);
	}

	/*default*/ void start()
	{
		Bukkit.getPluginManager().registerEvents(this, inst);
	}
	/*default*/ void stop()
	{
		HandlerList.unregisterAll(this);
	}

	/*default*/ abstract Class<? extends Event>[] getEvents();
}
