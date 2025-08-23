package fr.feneck91.worldguardinteractext;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTakeLecternBookEvent;

import java.util.*;

/**
 * Class used to load and manage configuration.
 */
public class MaterialConfig
{
    /**
     * Instance of plugin.
     */
    private final WorldGuardInteractExt m_plugin;

    /**
     * Materials managers map.
     * <p>
     * The key is the super type (__FIRE__, __FIELD__, etc).
     * </p>
     */
    private final HashMap<String, IMaterialManager> m_mapMaterialManagers;

    /**
     * Constructor.
     *
     * @param _plugin Plugin, used to access logger ot other things.
     */
    public MaterialConfig(WorldGuardInteractExt _plugin)
    {
        m_plugin = _plugin;
        // Initialize all available materials
        m_mapMaterialManagers = new HashMap<String, IMaterialManager>();
        m_mapMaterialManagers.put(CampFireMaterialManager.MATERIAL_TYPE, new CampFireMaterialManager(_plugin));
        m_mapMaterialManagers.put(LecternMaterialManager.MATERIAL_TYPE, new LecternMaterialManager(_plugin));
        m_mapMaterialManagers.put(CauldronMaterialManager.MATERIAL_TYPE, new CauldronMaterialManager(_plugin));
        m_mapMaterialManagers.put(BlockBreakManager.MATERIAL_TYPE, new BlockBreakManager(_plugin));
    }

    /**
     * Get the plugin.
     *
     * @return The plugin instance.
     */
    public WorldGuardInteractExt getPlugin()
    {
        return m_plugin;
    }

    /**
     * Get the list of all materials types separate with comma.
     *
     * @return A list of all materials managed by this plugin.
     */
    public String getAllMaterialsTypes()
    {
        return String.join(", ", m_mapMaterialManagers.keySet());
    }

    /**
     * Read the configuration.
     *
     * @param _config Configuration to read.
     * @param _logger Wrap class to log to sender if provide from a command, used to write message to info logger.
     * @return true if no critical error has been encounter, false else.
     * <p>
     *     When return true, warning can be encountered, some part of the configuration file may be ignored.
     * </p>
     */
    public boolean ReadConfig(FileConfiguration _config, LoggerDispatcher _logger)
    {
        boolean bRet = true;

        for (Map<?, ?> itemlist : _config.getMapList("items"))
        {   // Iterate across the List of Maps in the config
            Map<String, Object> mapItems = (Map<String, Object>) itemlist;
            String strMaterialType = (String) mapItems.get("type"); // If not exist or bad type, exception is raised

            if (m_mapMaterialManagers.containsKey(strMaterialType))
            {
                _logger.sendInfoMessage("Reading material = " + strMaterialType);
                bRet = ((IMaterialManager) m_mapMaterialManagers.get(strMaterialType)).readConfig(mapItems, _logger);
                if (!bRet)
                {   // Configuration is not good
                    break;
                }
            }
            else
            {
                _logger.sendErrorMessage("Unknown material type: " + strMaterialType);
                bRet = false;
                break;
            }
        }

        return bRet;
    }

    /**
     * Manage user interaction.
     *
     * @param _event Generic event.
     * @return Informations about how to manage this interaction, null to ignore all.
     */
    public InteractEventManager.InteractEventsInfos managePlayerInteraction(Event _event)
    {
        InteractEventManager.InteractEventsInfos interactEventInfos = null;

        if (_event instanceof Cancellable eventCancellable)
        {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            Player player = null;
            Block block = null;

            if (_event instanceof PlayerInteractEvent playerInteractEvent)
            {
                player = playerInteractEvent.getPlayer();
                block = playerInteractEvent.getClickedBlock();
            }
            else if (_event instanceof BlockIgniteEvent blockIgniteEvent)
            {
                player = blockIgniteEvent.getPlayer();
                block = blockIgniteEvent.getBlock();
            }
            else if (_event instanceof PlayerTakeLecternBookEvent playerTakeLecternBookEvent)
            {
                player = playerTakeLecternBookEvent.getPlayer();
                block = playerTakeLecternBookEvent.getLectern().getBlock();
            }

            if (block != null && AMaterialManager.isPluginActivatedForPlayerMode(player))
            {
                World world = player.getWorld();
                RegionManager manager = container.get(BukkitAdapter.adapt(world));
                if (manager != null)
                {
                    // Get region from block location, not from player location
                    ApplicableRegionSet regionSet = manager.getApplicableRegions(BukkitAdapter.asBlockVector(block.getLocation()));
                    // Take region with highter priority
                    String strBlocRegionName = regionSet.getRegions().stream()
                                                        .max(Comparator.comparingInt(ProtectedRegion::getPriority))
                                                        .map(ProtectedRegion::getId)
                                                        .orElse(null);
                    if (strBlocRegionName != null)
                    {
                        for (IMaterialManager materialManager : m_mapMaterialManagers.values())
                        {
                            interactEventInfos = materialManager.managePlayerInteraction(_event, block, world, strBlocRegionName);
                            if (interactEventInfos != null)
                            {
                                if (interactEventInfos.isEventsEmpty())
                                {   // If here, the event is processed and nothing should be done
                                    interactEventInfos = null;
                                }
                                else if (getPlugin().isVerboseLogEnabled())
                                {
                                    getPlugin().getLogger().info("Accepted event for player '" + player.getName() + "' => " + materialManager.getMaterialType() + " / Event = " + _event.getClass().getSimpleName());
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }
        return interactEventInfos;
    }

    /**
     * Display informations about material.
     *
     * @param _strMaterialType Type, like __CAMPFIRE__, __FIELD__, etc.
     * @param _logger Wrap class to log to sender if provide from a command, used to write message to info logger.
     */
    public void displayMaterials(String _strMaterialType, LoggerDispatcher _logger)
    {
        if (m_mapMaterialManagers.containsKey(_strMaterialType))
        {   // If exists, display
            ((IMaterialManager) m_mapMaterialManagers.get(_strMaterialType)).displayMaterials(_logger);
        }
        else
        {   // If not exists, warn the user
            m_plugin.getLogger().warning("Unknown material type = " + _strMaterialType);
        }
    }
}
