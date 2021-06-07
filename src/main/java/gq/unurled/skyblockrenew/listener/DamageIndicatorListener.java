package gq.unurled.skyblockrenew.listener;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import gq.unurled.skyblockrenew.SkyblockRenew;
import gq.unurled.skyblockrenew.listener.entity.EntityHider;
import gq.unurled.skyblockrenew.utils.CalcStats;
import gq.unurled.skyblockrenew.utils.Utility;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class DamageIndicatorListener implements Listener {
  private JavaPlugin plugin;
  private EntityHider entityHider;
  FileConfiguration config;
  private List<ArmorStand> toBeRemovedArmorstands;
  private CalcStats stats = new CalcStats();

  public DamageIndicatorListener(JavaPlugin plugin, EntityHider entityHider, FileConfiguration config,
                                 List<ArmorStand> toBeRemovedArmorstands) {
    this.plugin = plugin;
    this.entityHider = entityHider;
    this.config = config;
    this.toBeRemovedArmorstands = toBeRemovedArmorstands;
  }

  @EventHandler
  public void entityDamageByEntity(EntityDamageByEntityEvent event) {
    if (event.isCancelled())
      return;
    // Don't show indicator if the damagee is an armor stand
    if (event.getEntity().getType() == EntityType.ARMOR_STAND)
      return;

    // Only show indicator if the damager was a player or an arrow
    if (!(event.getDamager().getType() == EntityType.PLAYER || event.getDamager().getType() == EntityType.ARROW))
      return;

    Player damager;
    Location spawnLocation;
    Random random = new Random();
    DecimalFormat damageFormat = new DecimalFormat(
        ChatColor.translateAlternateColorCodes('&', config.getString("IndicatorFormat")));

    // Tries random positions until it finds one that is not inside a block
    int tries = 0;
    do {
      tries++;
      spawnLocation = event.getEntity().getLocation().add(random.nextDouble() * (1.0 + 1.0) - 1.0, 1,
          random.nextDouble() * (1.0 + 1.0) - 1.0);
      if (tries > 20) {
        spawnLocation = event.getEntity().getLocation();
        break;
      }
    } while (!spawnLocation.getBlock().isEmpty() && !spawnLocation.getBlock().isLiquid()); // In previous
                                                                                           // versions of
    // this plugin I used
    // .isPassable() but that's
    // not compatible with older
    // versions of Minecraft.

    // Check if the damager is an arrow. If it is use arrow.isCritical().
    // If it isn't use the custom isCritical() for direct damage.
    if (event.getDamager().getType() == EntityType.ARROW) {
      Arrow arrow = (Arrow) event.getDamager();

      // Don't show indicator if the arrow doesn't belong to a player
      if (!(arrow.getShooter() instanceof Player)) {
        return;
      }

      damager = (Player) arrow.getShooter();

      if (arrow.isCritical())
        damageFormat = new DecimalFormat(
            ChatColor.translateAlternateColorCodes('&', config.getString("CriticalIndicatorFormat")));
    } else {
      damager = (Player) event.getDamager();
      if (Utility.isCritical(damager))
        damageFormat = new DecimalFormat(
            ChatColor.translateAlternateColorCodes('&', config.getString("CriticalIndicatorFormat")));
    }

    // Spawn an invisible armor stand
    final ArmorStand armorStand = (ArmorStand) spawnLocation.getWorld().spawn(spawnLocation, ArmorStand.class,
        new InvisibleArmorStand(plugin, damager, entityHider, config.getBoolean("ShowToDamagerOnly")));

    // Set visible name
    armorStand.setCustomName(String.valueOf(damageFormat.format(stats.getFinalDamage((Player) event.getDamager()))));
    armorStand.setCustomNameVisible(true);

    toBeRemovedArmorstands = new ArrayList<>();
    // Destroy the armor stand after 3 sec
    toBeRemovedArmorstands.add(armorStand);
    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
      armorStand.remove();
      toBeRemovedArmorstands.remove(armorStand);
    }, 30);
  }
}