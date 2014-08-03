package com.lb_stuff.eventfilterservices;

import com.lb_stuff.eventfilterservices.EventFilterServices.DependencyChangeEvent;
import com.lb_stuff.eventfilterservices.EventFilterServices.DependencyType;
import com.lb_stuff.eventfilterservices.config.MainConfig;

import static com.lb_stuff.eventfilterservices.EventFilterServices.DependencyType.*;

import net.gravitydevelopment.updater.Updater;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import static org.bukkit.ChatColor.*;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import java.lang.reflect.Method;

public final class EventFilterServicesPlugin extends JavaPlugin implements Listener, CommandExecutor
{
	private final File configFile = new File(getDataFolder(), "config.yml");
	private MainConfig config;
	private Updater updater;
	@Override
	public void onEnable()
	{
		getServer().getPluginManager().registerEvents(this, this);

		for(EventFilterServices s : EventFilterServices.values())
		{
			s.get().register(ServicePriority.High);
		}

		boolean firstrun = !configFile.exists();

		try
		{
			getDataFolder().mkdirs();
			config = new MainConfig(configFile);
		}
		catch(IOException|InvalidConfigurationException e)
		{
			throw new RuntimeException(e);
		}
		if(!firstrun)
		{
			if(!config.getBoolean("auto-updater"))
			{
				getLogger().info("Automatic update downloading disabled in config");
			}
			else
			{
				updater = new Updater(this, 0, getFile(), Updater.UpdateType.DEFAULT, false)
				{
					@Override
					public boolean shouldUpdate(String current, String potential)
					{
						String[] c = current.split("\\.");
						String[] p = potential.split("\\.");
						if(c.length != 3 || p.length != 3)
						{
							return true;
						}
						for(int i = 0; i < 3; ++i)
						{
							int ci = Integer.parseInt(c[i]);
							int pi = Integer.parseInt(p[i]);
							if(ci < pi)
							{
								return true;
							}
							else if(ci > pi)
							{
								return false;
							}
						}
						return false;
					}
				};
				Bukkit.getScheduler().runTaskAsynchronously(this, new Runnable(){@Override public void run()
				{
					switch(updater.getResult())
					{
						case NO_UPDATE:
						{
							getLogger().info("Up to date.");
						} break;
						case SUCCESS:
						{
							getLogger().info("Update installed and ready: "+updater.getLatestName());
						} break;
						case UPDATE_AVAILABLE:
						{
							getLogger().warning("Out of date: new version is "+updater.getLatestName());
						} break;
						default: break;
					}
				}});
			}
		}
		else
		{
			getLogger().warning("This plugin supports auto-updating - you may disable it in the config.");
		}
	}
	@Override
	public void onDisable()
	{
		for(EventFilterServices s : EventFilterServices.values())
		{
			s.get().unregister();
		}
	}
	@Override
	public FileConfiguration getConfig()
	{
		return config;
	}
	@Override
	public void reloadConfig()
	{
		try
		{
			config.reload(configFile);
		}
		catch(IOException|InvalidConfigurationException e)
		{
			throw new RuntimeException(e);
		}
	}
	@Override
	public void saveDefaultConfig()
	{
		reloadConfig();
	}

	private void tell(CommandSender cs, String msg)
	{
		cs.sendMessage(""+DARK_PURPLE+'['+LIGHT_PURPLE+"EventFilterServices"+DARK_PURPLE+']'+RESET+' '+msg);
	}
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
	{
		if(args.length == 0)
		{
			for(EventFilterServices s : EventFilterServices.values())
			{
				Set<Plugin> plugins = s.getDependees(null);
				if(plugins.size() == 0)
				{
					tell(sender, "No plugins registered for "+AQUA+s.name()+RESET+".");
					continue;
				}
				tell(sender, "Plugins registered for "+AQUA+s.name()+RESET+":");
				for(Plugin p : plugins)
				{
					PluginDescriptionFile pd = p.getDescription();
					String msg = "- "+AQUA+p.getName()+RESET+" v"+pd.getVersion()+" (";
					DependencyType dt = s.getDependencyType(p);
					if(dt == INCOMPATIBLE)
					{
						msg = msg + RED+"incompatible"+RESET;
					}
					else if(dt == OPTIONAL)
					{
						msg = msg + GREEN+"optional"+RESET;
					}
					else if(dt == REQUIRED)
					{
						msg = msg + AQUA+"required"+RESET;
					}
ListenerLoop:
					for(RegisteredListener rl : HandlerList.getRegisteredListeners(p))
					{
						Class<? extends Listener> l = rl.getListener().getClass();
						for(Method m : l.getDeclaredMethods())
						{
							if(m.getAnnotation(EventHandler.class) != null && m.getParameterCount() == 1)
							{
								for(Class<? extends Event> clazz : s.get().getEvents())
								{
									if(m.getParameterTypes()[0].equals(clazz))
									{
										msg += ", listening";
										break ListenerLoop;
									}
								}
							}
						}
					}
					tell(sender, msg+")");
				}
			}
			return true;
		}
		return false;
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onDependencyChange(final DependencyChangeEvent e)
	{
		final EventFilterServices s = e.getServiceType();
		if(e.getDependencyType() == INCOMPATIBLE)
		{
			getLogger().warning(e.getPlugin().getName()+" is incompatible with "+s.get().getClass().getSimpleName());
		}
		Bukkit.getScheduler().runTask(this, new Runnable(){@Override public void run()
		{
			if(s.getDependees(REQUIRED).size() > 0 || s.getDependees(OPTIONAL).size() > 0)
			{
				s.get().start();
			}
			else
			{
				s.get().stop();
			}
		}});
	}
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPluginDisable(PluginDisableEvent e)
	{
		for(EventFilterServices s : EventFilterServices.values())
		{
			s.depend(e.getPlugin(), null);
		}
	}
}
