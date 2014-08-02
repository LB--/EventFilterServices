PotionFilterService.java
========================

This is a single-file dependency you can shade into your plugins for the ability to filter splash potions on a per-target basis, rather than per set of targets. It intercepts Bukkit's `PotionSplashEvent`s and splits them into multiple `Splash`es, one for each mapping from source to target. You can customize the effects each target receives without affecting the effects other targets receive, which is useful for potion effect filtering (hence the name). This was originally a party of [KataParty](https://github.com/LB--/KataParty) but was separated out because of its otherwise invasive nature with cancelling `PotionSplashEvent`s at the last second.

## Example Usage
```java
import com.lb_stuff.service.PotionFilterService;
import org.bukkit.plugin.ServicePriority;
```
```java
	@Override
	public void onEnable()
	{
		new PotionFilterService(ServicePriority.Normal).start();
		//...
	}
```
```java
import static com.lb_stuff.service.PotionFilterService.Splash;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
```
```java
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onSplash(final Splash s)
	{
		//...
	}
```
Don't forget to register your listeners with the Bukkit `PluginManager`.

To use in your project, you have two options. The first (preferred) option is to use maven to shade the dependency into your project, and the second is just to copy the file into your project and edit the `package` declaration.

## Shading in the dependency with Maven
First, you will want to add this as a dependency in your project. Add this to the `repositories` section of your `pom.xml`:
```xml
		<repository>
			<id>PotionFilterService.java-mvn-repo</id>
			<url>https://raw.github.com/LB--/PotionFilterService.java/mvn-repo/</url>
		</repository>
```
Then, add this to the `dependencies` section:
```xml
		<dependency>
			<groupId>com.lb_stuff.service</groupId>
			<artifactId>PotionFilterService</artifactId>
			<version>1.0.0</version>
		</dependency>
```
Refresh your project dependencies and it should be downloaded. You can now `import` it into your porject and use it as in the example.

The next important step is to use the maven shade plugin to relocate the class into your own project packages so that it will not conflict with any other plugins that use this too. Add this to the `plugins` section of the `build` section in your `pom.xml`:
```xml
			<plugin>
				<groupId>org.apache.maven.plugins</groupId>
				<artifactId>maven-shade-plugin</artifactId>
				<version>2.3</version>
				<executions>
					<execution>
						<phase>package</phase>
						<goals>
							<goal>shade</goal>
						</goals>
						<configuration>
							<artifactSet>
								<includes>
									<include>com.lb_stuff.service:PotionFilterService</include>
								</includes>
							</artifactSet>
							<relocations>
								<relocation>
									<pattern>com.lb_stuff.service</pattern>
									<shadedPattern>YOUR.PLUGIN.PACKAGE.HERE</shadedPattern>
								</relocation>
							</relocations>
						</configuration>
					</execution>
				</executions>
			</plugin>
```
Those of you that have used Gravity's `Updater` will notice that this is familiar. You can do both at the same time if you merge the XML properly.

Now when you build your project and investigate the jar, you'll see that the `PotionFilterService` class is in your desired package and will not conflict with other plugins that also utilize it.

## Option 2: Just doing it by hand
You could just download the java source file, place it into your project, and edit the `package` statement on the first line like some of you may have done with Gravity's `Updater`. If you prefer that, that's fine - I don't ;)
