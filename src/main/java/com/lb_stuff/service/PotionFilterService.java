package com.lb_stuff.service;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.event.Listener;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.Cancellable;
import org.bukkit.Bukkit;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.potion.PotionEffect;
import org.bukkit.entity.Projectile;
import org.bukkit.util.Vector;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;

import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

/**
 * This service listens to {@link org.bukkit.event.player.PotionSplashEvent}
 * and cancels the event after other plugins have processed it, instead splitting
 * it into multiple events ({@link PotionFilterService.Splash}) where each
 * event is from the source to one of the targets. This allows each target have a
 * different effect applied (or no effect at all) based on their context. This class
 * listens to its own events that have not been cancelled with {@link org.bukkit.event.EventPriority#MONITOR}
 * priority and manually sends the message to each target based on the event.
 */
public final class PotionFilterService implements Listener
{
	/**
	 * Doesn't do anything - allows you to register the service as you wish.
	 */
	public PotionFilterService()
	{
	}
	/**
	 * Registers the service with the given {@link ServicePriority}, but does not start it.
	 * @param priority The desired {@link ServicePriority}.
	 */
	public PotionFilterService(ServicePriority priority)
	{
		JavaPlugin plugin = JavaPlugin.getProvidingPlugin(PotionFilterService.class);
		Bukkit.getServicesManager().register(PotionFilterService.class, this, plugin, priority);
	}

	/**
	 * Start intercepting {@link org.bukkit.event.player.PotionSplashEvent}.
	 */
	public void start()
	{
		JavaPlugin plugin = JavaPlugin.getProvidingPlugin(PotionFilterService.class);
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}
	/**
	 * Stop intercepting {@link org.bukkit.event.player.PotionSplashEvent}.
	 */
	public void stop()
	{
		HandlerList.unregisterAll(this);
	}

	/**
	 * Convenience method for {@link org.bukkit.plugin.ServicesManager#getRegistration(java.lang.Class)}.
	 * @return The {@link RegisteredServiceProvider} for this class.
	 */
	public static RegisteredServiceProvider<PotionFilterService> getService()
	{
		return Bukkit.getServicesManager().getRegistration(PotionFilterService.class);
	}
	/**
	 * Convenience method for {@link org.bukkit.plugin.ServicesManager#getRegistrations(java.lang.Class)}.
	 * @return The {@link RegisteredServiceProvider} for this class.
	 */
	public static Collection<RegisteredServiceProvider<PotionFilterService>> getServices()
	{
		return Bukkit.getServicesManager().getRegistrations(PotionFilterService.class);
	}

	/**
	 * See class description for more information.
	 * @param e The {@link org.bukkit.event.player.PotionSplashEvent}.
	 */
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
	public void onSplash(PotionSplashEvent e)
	{
		ThrownPotion potion = e.getPotion();
		ProjectileSource source = potion.getShooter();
		if(!(source instanceof Rethrower))
		{
			if(!e.isCancelled())
			{
				for(LivingEntity le : e.getAffectedEntities())
				{
					final Splash s = new Splash(source, le, potion, e.getIntensity(le));
					Bukkit.getPluginManager().callEvent(s);
				}
				e.setCancelled(true);
			}
		}
		else
		{
			Rethrower r = (Rethrower)source;
			LivingEntity target = r.target;
			for(LivingEntity le : e.getAffectedEntities())
			{
				if(le == target)
				{
					e.setIntensity(le, r.intensity);
				}
				else
				{
					e.setIntensity(le, 0.0);
				}
			}
			e.setCancelled(false);
		}
	}
	/**
	 * See class description for more information.
	 * @param s The {@link Splash}.
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onSplash(Splash s)
	{
		for(PotionEffect pe : s.getEffects())
		{
			Rethrower r = new Rethrower(s.getSource(), s.getTarget(), pe, s.getIntensity(pe));
			ThrownPotion rethrown = r.launchProjectile(ThrownPotion.class, new Vector());
			rethrown.teleport(s.getPotion());
			ItemStack is = new ItemStack(s.item);
			PotionMeta m = (PotionMeta)is.getItemMeta();
			m.clearCustomEffects();
			m.addCustomEffect(pe, true);
			m.setMainEffect(pe.getType());
			is.setItemMeta(m);
			rethrown.setItem(is);
		}
	}

	/**
	 * Listen to this event to customize splash potion effects for different
	 * players, or even cancel splash potions from affecting certain
	 * players. See {@link PotionFilterService} for more information.
	 */
	public static final class Splash extends Event implements Cancellable
	{
		private final ProjectileSource source;
		private final LivingEntity target;
		private final ThrownPotion potion;
		private final ItemStack item;
		private final Map<PotionEffect, Double> intensities = new HashMap<>();
		private Splash(ProjectileSource src, LivingEntity dest, ThrownPotion pot, double intensity)
		{
			source = src;
			target = dest;
			potion = pot;
			item = new ItemStack(pot.getItem());
			PotionMeta m = (PotionMeta)item.getItemMeta();
			for(PotionEffect pe : m.getCustomEffects())
			{
				intensities.put(pe, intensity);
			}
			if(intensities.isEmpty())
			{
				for(PotionEffect pe : potion.getEffects())
				{
					intensities.put(pe, intensity);
				}
			}
		}

		/**
		 * Returns the {@link PotionFilterService} instance.
		 * @return The {@link PotionFilterService} instance.
		 */
		public PotionFilterService getService()
		{
			return Bukkit.getServicesManager().getRegistration(PotionFilterService.class).getProvider();
		}

		public ProjectileSource getSource()
		{
			return source;
		}
		public LivingEntity getTarget()
		{
			return target;
		}
		public ThrownPotion getPotion()
		{
			return potion;
		}
		public Set<PotionEffect> getEffects()
		{
			return intensities.keySet();
		}
		public double getIntensity(PotionEffect effect)
		{
			if(intensities.containsKey(effect))
			{
				return intensities.get(effect);
			}
			return 0.0;
		}
		public void setIntensity(PotionEffect effect, Double intensity)
		{
			if(intensity != null && intensity > 0.0)
			{
				intensities.put(effect, intensity);
			}
			else
			{
				intensities.remove(effect);
			}
		}

		private boolean cancelled = false;
		/**
		 * Returns whether this potion will not be applied.
		 * @return Whether this potion will not be applied.
		 */
		@Override
		public boolean isCancelled()
		{
			return cancelled;
		}
		/**
		 * Cancel or uncancel the potion from splashing the target.
		 * @param c Whether the potion will splash the target.
		 */
		@Override
		public void setCancelled(boolean c)
		{
			cancelled = c;
		}

		private static final HandlerList handlers = new HandlerList();
		/**
		 * See {@link org.bukkit.event.Event#getHandlers()}.
		 * @return The {@link org.bukkit.event.HandlerList}.
		 */
		@Override
		public HandlerList getHandlers()
		{
			return handlers;
		}
		/**
		 * See {@link org.bukkit.event.Event#getHandlers()}.
		 * @return The {@link org.bukkit.event.HandlerList}.
		 */
		public static HandlerList getHandlerList()
		{
			return handlers;
		}
	}

	private final class Rethrower implements ProjectileSource
	{
		private final ProjectileSource original;
		private final LivingEntity target;
		private final PotionEffect effect;
		private final double intensity;
		private Rethrower(ProjectileSource ps, LivingEntity le, PotionEffect pe, double i)
		{
			original = ps;
			target = le;
			effect = pe;
			intensity = i;
		}

		@Override
		public <T extends Projectile> T launchProjectile(Class<? extends T> projectile)
		{
			T t = original.launchProjectile(projectile);
			t.setShooter(this);
			return t;
		}
		@Override
		public <T extends Projectile> T launchProjectile(Class<? extends T> projectile, Vector velocity)
		{
			T t = original.launchProjectile(projectile, velocity);
			t.setShooter(this);
			return t;
		}
	}
}
