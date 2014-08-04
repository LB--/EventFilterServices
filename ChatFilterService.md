ChatFilterService
=================

This is a service you can use for the ability to filter chat messages on a per-target basis, rather than per set of targets. It intercepts Bukkit's `AsyncPlayerChatEvent`s and splits them into multiple `AsyncMessage`s, one for each mapping from source to target. The source is one of the targets, and the server console is also one of the targets. You can customize the message each target receives without affecting the message other targets receive, which is useful for chat filtering (hence the name). This was originally a party of [KataParty](https://github.com/LB--/KataParty) but was separated out because of its otherwise invasive nature with cancelling `AsyncPlayerChatEvent`s at the last second.

## Example Usage
### `plugin.yml`
```yml
# For DependencyType.OPTIONAL:
softdepend: ["EventFilterServices"]
# For DependencyType.REQUIRED:
depend: ["EventFilterServices"]
```
### `MyPlugin.java`
```java
import com.lb_stuff.eventfilterservices.EventFilterServices;
```
```java
	@Override
	public void onEnable()
	{
		try
		{
			// use DependencyType.OPTIONAL or DependencyType.REQUIRED
			EventFilterServices.ChatFilter.depend(this, EventFilterServices.DependencyType.OPTIONAL);
			getServer().getPluginManager().registerEevents(new MyChatFilter(this), this);
		}
		catch(ClassNotFoundException|NoSuchFieldException e)
		{
			getLogger().info("Couldn't find ChatFilterService, will not be able to filter chat");
			// If you used "depend" instead of "softdepend" in your pugin.yml, Bukkit will not even
			// load your plugin in the first place without EventFilterServices, but you still need
			// to catch NoSuchFieldException in case the particular service you need is not provided
			// in the version of EventFilterServices being used.
		}
		//...
	}
```
### `MyChatFilter.java`
```java
import static com.lb_stuff.eventfilterservices.ChatFilterService.AsyncMessage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
```
```java
	// If you used DependencyType.OPTIONAL, be careful to not reference
	// the class which contains this event handler unless you know
	// the service you need is available.
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onMessage(final AsyncMessage m)
	{
		//...
	}
```

## You use Maven? Great!
First, you will want to add this as a dependency in your project. Add this to the `repositories` section of your `pom.xml`:
```xml
		<repository>
			<id>EventFilterServices-repo</id>
			<url>https://raw.github.com/LB--/EventFilterServices/mvn-repo/</url>
		</repository>
```
Then, add this to the `dependencies` section:
```xml
		<dependency>
			<groupId>com.lb_stuff.eventfilterservices</groupId>
			<artifactId>EventFilterServices</artifactId>
			<version>1.0.1</version>
		</dependency>
```
Refresh your project dependencies and it should be downloaded. You can now `import` it into your project and use it as in the examples above.
