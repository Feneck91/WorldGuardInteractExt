package fr.feneck91.worldguardinteractext;

import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.session.Session;
import com.sk89q.worldguard.session.SessionManager;
import com.sk89q.worldguard.session.handler.Handler;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
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
     * Unique instance.
     */
    static private WorldGuardInteractExt s_instance = null;

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
     * Get the instance.
     *
     * @return The unique instance of this plugin.
     */
    public static WorldGuardInteractExt getInstance()
    {
        return s_instance;
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
     * Indicate if next PlaceEvent should be canceled or not.
     *
     * @param _player Info for this player.
     * @return true if flag was previously set to true, false else.
     */
    public boolean isNextPlaceEventShouldBeCanceled(Player _player)
    {
        return m_materialConfig.isNextPlaceEventShouldBeCanceled(_player);
    }

    /**
     * Called when plugin is activated.
     * <p>
     * Used to read the current configuration.
     */
    @Override
    public void onEnable()
    {
        s_instance = this;
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
        s_instance = null;
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

    /**
     * Used when user run a commanbd.
     *
     * @param _sender Sender.
     * @param _command Command.
     * @param _strLabel Label.
     * @param _args Argument.
     * @return true if the command is executed, false else.
     */
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
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onBlockPlaceEventLowest(BlockPlaceEvent _event)
    {
        if (m_materialConfig.manageBlockPlaceEvent(_event))
        {
            _event.setCancelled(true);
            if (IsVerboseLogEnabled())
            {
                getLogger().info("Block place event canceled - WorldGuard will not called");
            }
        }
    }

    /**
     * When block change, verify it it should be reactivated.
     *
     * @param _event The event.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockPlaceEventHighest(BlockPlaceEvent _event)
    {
        if (m_materialConfig.manageBlockPlaceEvent(_event))
        {
            m_materialConfig.clearNextPlaceEventInfos(_event.getPlayer());
            _event.setCancelled(false);
            if (IsVerboseLogEnabled())
            {
                getLogger().info("Block place event canceled is reactivated!");
            }
        }
    }

    /**
     * When block is ignite event.
     *
     * Used when block ignite, even the player make event to put fire, it is this event that is called, check if it must be uncanceled.
     *
     * @param _event The event
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onBlockIgniteLowest(BlockIgniteEvent _event)
    {
        if (m_materialConfig.isNextPlaceEventShouldBeCanceled(_event.getPlayer()))
        {
            _event.setCancelled(true); // Block  event
            if (IsVerboseLogEnabled())
            {
                getLogger().info("Block ignite interaction canceled - WorldGuard will not called!");
            }
        }
    }

    /**
     * When block is ignite event.
     *
     * Used when block ignite, even the player make event to put fire, it is this event that is called, check if it must be uncanceled.
     *
     * @param _event The event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockIgnite(BlockIgniteEvent _event)
    {
        if (_event.isCancelled() && m_materialConfig.isNextPlaceEventShouldBeCanceled(_event.getPlayer()))
        {   // Only if WorldGuard has canceled the interaction, else do nothing
            _event.setCancelled(false); // Reactivated event
            if (IsVerboseLogEnabled())
            {
                getLogger().info("Block ignite interaction reactivated!");
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
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPlayerInteractLowest(PlayerInteractEvent _event)
    {
        m_materialConfig.clearNextPlaceEventInfos(_event.getPlayer());
        // Only if WorldGuard has canceled the interaction, else do nothing
        Block block = _event.getClickedBlock();
        if (block != null)
        {
            if (_event.getHand() == EquipmentSlot.HAND)
            {   // Remove 2 call with OFF_HAND
                if (m_materialConfig.manageEvent(_event))
                {
                    _event.setCancelled(true); // Ignore WorldGuard message and rules
                    if (IsVerboseLogEnabled())
                    {
                        getLogger().info("Player interaction canceled - WorldGuard not called!");
                    }
                }
            }
        }
    }

    /**
     * When player make event.
     *
     * Check if it must be uncanceled.
     *
     * @param _event The event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerInteractHighest(PlayerInteractEvent _event)
    {
        if (_event.useItemInHand() == Event.Result.DENY || _event.useInteractedBlock() == Event.Result.DENY)
        {
            if (m_materialConfig.isNextPlaceEventShouldBeCanceled(_event.getPlayer()))
            {
                _event.setCancelled(false); // Do the event
                if (IsVerboseLogEnabled())
                {
                    getLogger().info("Player interaction reactivated");
                }
            }
        }
    }
}