package fr.feneck91.worldguardinteractext;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.data.type.Campfire;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import java.util.Set;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Base class to manage plugin.
 *
 * Allow to catch event and check if some block interaction forbidden by WorldGuard can be allowed or not by this plugin.
 */
public class WorldGuardInteractExt extends JavaPlugin implements Listener
{
    /**
     * Class to manage material than can be survey.
     */
    public static class MaterialSurvey
    {
        /**
         * Material that can be manage.
         */
        private Material        m_material;

        /**
         * Region allowed to manage this material.
         */
        private Set<String>     m_lstRegionsNames;
    };
    
    /**
     * Is log enabled?
     */
    private boolean         m_bIsLogEnabled;
    //private Map<String, >   m_allowedRegionsNames;
    //private Material extinguishItem;

    /**
     * Called when plugin is activated.
     * <p>
     * Used to read the current configuration.
     */
    @Override
    public void onEnable()
    {
        if (readConfiguration())
        {
            getServer().getPluginManager().registerEvents(this, this);
            getLogger().info("WorldGuardInteractExt activated!");
        }
    }

    /**
     * Called when plugin is activated.
     * <p>
     * Used to read the current configuration.
     */
    @Override
    public void onDisable()
    {
        getLogger().info("WorldGuardInteractExt deactivated!");
    }

    /**
     * Read the plugin configuration.
     *
     * @return true if configuration is OK.
     */
    private boolean readConfiguration()
    {
        boolean bRet = false;
        saveDefaultConfig();
        FileConfiguration config = getConfig();
        // Reading config
        m_bIsLogEnabled = config.getBoolean("enable_logs");
        List<?> listItems = config.getList("items");
        bRet = true;
        /*
        Material.CAMPFIRE.name();
        Material.SOUL_CAMPFIRE.name();
        Material.CAMPFIRE.name()
        static Material
        getMaterial(String name)


        Material.SOUL_CAMPFIRE
        Map<>
        m_allowedRegionsNames = config.getStringList("enable_log");
        extinguishItem = Material.getMaterial(config.getString("extinguish-item", "STICK"));
        */
        return bRet;
    }
/*
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event)
    {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
        {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.CAMPFIRE)
        {
            return;
        }
        
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() != extinguishItem) return;

        if (!isInAllowedRegion(player)) return;

        block.setType(Material.SOUL_CAMPFIRE);
        player.sendMessage("§bFeu éteint !");
    }
*/
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCampfireClick(PlayerInteractEvent _event)
    {
        if (_event.getHand() != EquipmentSlot.HAND)
        {
            return; // To not do more than once with OFF_HAND
        }

        if (_event.getClickedBlock() == null)
        {
            return;
        }

        Block block = _event.getClickedBlock();
        if (block.getType() != Material.CAMPFIRE && block.getType() != Material.SOUL_CAMPFIRE)
        {
            return;
        }

        if (!(block.getBlockData() instanceof Campfire))
        {
            return;
        }
        
        Campfire campfire = (Campfire) block.getBlockData();
        Player player = _event.getPlayer();
        ItemStack item = _event.getItem();
        Material tool = item != null ? item.getType() : Material.AIR;

        if (campfire.isLit())
        {   // 🔥 Stop fire with hand or shovel
            if (tool == Material.AIR || tool.name().endsWith("_SHOVEL"))
            {
                campfire.setLit(false);
                block.setBlockData(campfire);
                block.getWorld().playSound(block.getLocation(), "block.fire.extinguish", 1.0f, 1.0f);
                _event.setCancelled(true);
            }
        }
        else
        {   // 🔥 Start fire with fire charge or flint and steel
            if (tool == Material.FLINT_AND_STEEL || tool == Material.FIRE_CHARGE)
            {
                campfire.setLit(true);
                block.setBlockData(campfire);
                block.getWorld().playSound(block.getLocation(), "item.flintandsteel.use", 1.0f, 1.0f);
                _event.setCancelled(true);

                // Reduce durability or consume item (if not creative mode)
                if (player.getGameMode() != GameMode.CREATIVE)
                {
                    if (tool == Material.FLINT_AND_STEEL)
                    {
                        item.setDurability((short)(item.getDurability() + 1));
                        if (item.getDurability() >= item.getType().getMaxDurability())
                        {
                            player.getInventory().getItem(_event.getHand()).setAmount(0);
                        }
                    }
                    else if (tool == Material.FIRE_CHARGE)
                    {
                        item.setAmount(item.getAmount() - 1);
                    }
                }
            }
        }
    }

    private boolean isInAllowedRegion(Player player)
    {
        boolean bRet = false;
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager manager = container.get(BukkitAdapter.adapt(player.getWorld()));
        if (manager != null)
        {
            ApplicableRegionSet regionSet = manager.getApplicableRegions(BukkitAdapter.asBlockVector(player.getLocation()));
            Set<String> currentRegions = regionSet.getRegions().stream().map(ProtectedRegion::getId).collect(Collectors.toSet());
            /*
            for (String region : m_allowedRegionsNames)
            {
                if (currentRegions.contains(region)) 
                {
                    bRet = true;
                    break;
                }
            }*/
        }
        return bRet;
    }
}