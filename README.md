EventFilterServices
===================

This is a Bukkit Plugin that offers services to other Bukkit Plugins - specifically, it intercepts some of Bukkit's events which are not very modular, and converts or splits them into a more modular API for other plugins to take advantage of. By itself, it does nothing - other plugins have to enable its functionality. Most services have side effects in order to offer proper functionality while remaining withing the constraints of the Bukkit API. There is no dependency on CraftBukkit or any other Bukkit implementation.

For information each specific service offered by this plugin, see the other `*Service.md` files.
