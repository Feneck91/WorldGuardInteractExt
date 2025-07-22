package fr.feneck91.worldguardinteractext;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.block.Block;

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
            if (IsVerboseLogEnabled())
            {
                getLogger().info("WorldGuardInteractExt activated!");
            }
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
        if (IsVerboseLogEnabled())
        {
            getLogger().info("WorldGuardInteractExt deactivated!");
        }
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

    /**
     * When block change, verify it it should be reactivated.
     *
     * @param _event The event.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockPlaceEvent(BlockPlaceEvent _event)
    {
        if (m_materialConfig.manageBlockPlaceEvent(_event) && IsVerboseLogEnabled())
        {
            getLogger().info("Block place event canceled by WorldGuard is reactivated!");
        }
    }

    /**
     * When block is igoite event.
     *
     * Used when block ignite, even the player make event to put fire, it is this event that is called, check if it must be uncanceled.
     *
     * @param _event The event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockIgnite(BlockIgniteEvent _event)
    {
        m_materialConfig.clearNextPlaceEventInfos();
        if (_event.isCancelled())
        {   // Only if WorldGuard has canceled the interaction, else do nothing
            if (   m_materialConfig.manageEvent(_event)
                && IsVerboseLogEnabled()
                && !_event.isCancelled()
               )
            {
                getLogger().info("Block ignite interaction canceled by WorldGuard is reactivated!");
            }
        }
    }

    /**
     * When player make event.
     *
     * Check if it must be uncanceled.
     *
     *
     * @param _event The event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerInteract(PlayerInteractEvent _event)
    {
        m_materialConfig.clearNextPlaceEventInfos();
        if (_event.useItemInHand() == Event.Result.DENY || _event.useInteractedBlock() == Event.Result.DENY)
        {   // Only if WorldGuard has canceled the interaction, else do nothing
            Block block = _event.getClickedBlock();
            if (block != null)
            {
                if (_event.getHand() == EquipmentSlot.HAND)
                {   // Remove 2 call with OFF_HAND
                    if (   m_materialConfig.manageEvent(_event)
                        && IsVerboseLogEnabled()
                        && (   _event.useItemInHand() != Event.Result.DENY
                            && _event.useInteractedBlock() != Event.Result.DENY
                           )
                       )
                    {
                        getLogger().info("Player interaction canceled by WorldGuard is reactivated!");
                    }
                }
            }
        }
    }
}