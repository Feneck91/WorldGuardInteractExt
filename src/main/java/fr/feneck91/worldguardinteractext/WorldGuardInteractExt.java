package fr.feneck91.worldguardinteractext;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.data.type.Campfire;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
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

import java.util.Collection;
import java.util.Map;
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
     * Is verbose log enabled?
     */
    private boolean         m_bIsVerboseLogEnabled;

    /**
     * Configuration
     */
    private MaterialConfig  m_materialConfig;

    /**
     * Constructor.
     */
    public WorldGuardInteractExt()
    {
        m_bIsVerboseLogEnabled = false;
        // Default config with nothing into it
        m_materialConfig = new MaterialConfig(this);
    }

    /**
     * Is verbose log enabled?
     *
     * @return true if enables, false else.
     */
    public boolean IsVerboseLogEnabled()
    {
        return m_bIsVerboseLogEnabled;
    }

    /**
     * Called when plugin is activated.
     * <p>
     * Used to read the current configuration.
     */
    @Override
    public void onEnable()
    {
        getServer().getPluginManager().registerEvents(this, this);
        if (readConfiguration())
        {
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
        HandlerList.unregisterAll();
        m_materialConfig.clearAll();
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

        // Will save only if the file doesn't exists
        // If readConfiguration() is called because operator make a reload command (wgiextreload), he may
        // have deleted this file to get new one.
        saveDefaultConfig();

        try
        {
            FileConfiguration config = getConfig();
            // Reading config
            m_bIsVerboseLogEnabled = config.getBoolean("enable_verbose_logs");
            if (IsVerboseLogEnabled())
            {
                getLogger().info("Reading configuration");
            }
            MaterialConfig materialConfig = new MaterialConfig(this);
            if (materialConfig.RaadConfig(config))
            {
                m_materialConfig = materialConfig;
                bRet = true;
            }
        }
        catch(Exception _ex)
        {
            getLogger().severe("WorldGuardInteractExt::readConfiguration(), exception: " + _ex.getMessage());
            getLogger().severe("Previous configuration is keep.");
        }

        return bRet;
    }

    @Override
    public boolean onCommand(CommandSender _sender, Command _command, String _strLabel, String[] _args)
    {
        boolean bRet = false;

        if (_command.getName().equalsIgnoreCase("wgiextmaterials"))
        {
            if (!_sender.hasPermission("wgiext.materials"))
            {
                _sender.sendMessage(ChatColor.RED + "You don't have permission to execute this command!");
            }
            else if (_args.length != 1)
            {
                _sender.sendMessage(ChatColor.RED + "One an only one argument is needed for this command!");
            }
            else
            {
                m_materialConfig.displayMaterials(_args[0]);
            }
        }
        else if (_command.getName().equalsIgnoreCase("wgiextreload"))
        {
            if (!_sender.hasPermission("wgiext.reload"))
            {
                _sender.sendMessage(ChatColor.RED + "You don't have permission to execute this command!");
            }
            else if (_args.length != 0)
            {
                _sender.sendMessage(ChatColor.RED + "No argument needed for this command!");
            }
            else
            {
                // Reload configuration here
                if (readConfiguration())
                {
                    _sender.sendMessage(ChatColor.GREEN + "WorldGuardInteractExt configuration reloaded successfully.");
                    bRet = true;
                }
                else
                {
                    _sender.sendMessage(ChatColor.RED + "Error while reloading WorldGuardInteractExt configuration!");
                }
            }
        }
        return bRet;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent _event)
    {
        Block block = _event.getClickedBlock();
        if (block != null)
        {
            m_materialConfig.managePlayerInteraction(_event);
        }
    }
/*
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
*/
}