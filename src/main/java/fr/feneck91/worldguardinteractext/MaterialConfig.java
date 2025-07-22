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
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.*;
import java.util.stream.Collectors;

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
     *
     * The key is the super type (__FIRE__, __FIELD__, etc)
     */
    private HashMap<String, IMaterialManager> m_mapMaterialManagers;

    /**
     * Next PlaceEvent block.
     *
     * Used to quickly check if PlaceEvent will use this block, to reactivate the cancel event.
     * Key is the player name.
     */
    private Map<UUID, Block> m_mapNextPlaceEventBlock;

    /**
     * Constructor.
     *
     * @param _plugin Plugin, used to access logger ot other things.
     */
    public MaterialConfig(WorldGuardInteractExt _plugin)
    {
        m_plugin = _plugin;
        m_mapNextPlaceEventBlock = new HashMap<UUID, Block>();

        // Initialize all available materials
        m_mapMaterialManagers = new HashMap<String, IMaterialManager>();
        m_mapMaterialManagers.put(FireMaterialManager.MATERIAL_TYPE, new FireMaterialManager(_plugin));
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
     * Clear flag that indicate next PlaceEvent could be re-activated.
     *
     * @param _player Infos for this player.
     */
    public void clearNextPlaceEventInfos(Player _player)
    {
        if (_player != null)
        {
            m_mapNextPlaceEventBlock.remove(_player.getUniqueId());
        }
    }

    /**
     * Indicate if next PlaceEvent should be canceled or not.
     *
     * @param _player Info for this player.
     * @return true if flag was previously set to true, false else.
     */
    public boolean isNextPlaceEventShouldBeCanceled(Player _player)
    {
        return _player != null && m_mapNextPlaceEventBlock.containsKey(_player.getUniqueId());
    }

    /**
     * Constructor.
     *
     * @param _config Configuration to read.
     */
    public boolean RaadConfig(FileConfiguration _config)
    {
        boolean bRet = true;

        for (Map<?, ?> itemlist : _config.getMapList("items"))
        {   // Iterate across the List of Maps in the config
            Map<String, Object> mapItems = (Map<String, Object>) itemlist;
            String strMaterialType = (String) mapItems.get("type"); // If not exist or bad type, exception is raised

            if (m_mapMaterialManagers.containsKey(strMaterialType))
            {
                if (m_plugin.IsVerboseLogEnabled())
                {
                    m_plugin.getLogger().info("Reading material = " + strMaterialType);
                }
                bRet = ((IMaterialManager) m_mapMaterialManagers.get(strMaterialType)).readConfig(mapItems);
                if (!bRet)
                {   // Configuration is not good
                    break;
                }
            }
            else
            {
                m_plugin.getLogger().severe("Unknown material type: " + strMaterialType);
                bRet = false;
                break;
            }
        }

        return bRet;
    }

    /**
     * Manage PlaceBlock event.
     *
     * When managePlayerInteraction function detect that a canceled action was done by WorldGuard, it re-activate
     * the event and record block material and location.
     * Then, just after the PlayerInteractEvent, if BlockPlaceEvent is called by Minecraft framework with these
     * informations, this plugin must re-activate event too (example, put fire on camp fire will modify block too).
     *
     * @param _event Player event. event.getClickedBlock() is not null else we don't go there.
     * @return true if the event is re-activated.
     */
    public boolean manageBlockPlaceEvent(BlockPlaceEvent _event)
    {
        boolean bRet = false;
        if (m_mapNextPlaceEventBlock.containsKey(_event.getPlayer().getUniqueId()))
        {
            if (   _event.isCancelled()
                // Here, not sure the block is same, often it is not the same ! So just verify plugin is waiting block change at this location
                && (   m_mapNextPlaceEventBlock.containsKey(_event.getPlayer().getUniqueId())
                    && m_mapNextPlaceEventBlock.get(_event.getPlayer().getUniqueId()).getLocation().equals(_event.getBlock().getLocation())
                   )
               )
            {
                bRet = true;
                _event.setCancelled(false);
            }
        }

        return bRet;
    }

    /**
     * Manage user interaction.
     *
     * @param _event Generic event.
     * @return true if one manage interaction, false else.
     */
    public boolean manageEvent(Event _event)
    {
        boolean bRet = false;

        if (_event instanceof Cancellable eventCancellable)
        {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            Player player;
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
            else
            {
                player = null;
            }
            if (player != null)
            {
                World world = player.getWorld();
                RegionManager manager = container.get(BukkitAdapter.adapt(world));
                if (manager != null)
                {
                    ApplicableRegionSet regionSet = manager.getApplicableRegions(BukkitAdapter.asBlockVector(player.getLocation()));
                    // Take region with highter priority
                    String strCurrentPlayerRegionName = regionSet.getRegions().stream()
                                                        .max(Comparator.comparingInt(ProtectedRegion::getPriority))
                                                        .map(ProtectedRegion::getId)
                                                        .orElse(null);
                    if (strCurrentPlayerRegionName != null)
                    {
                        for (IMaterialManager materialManager : m_mapMaterialManagers.values())
                        {
                            if (materialManager.managePlayerInteraction(_event, block, world, strCurrentPlayerRegionName, (Block _block) ->
                            {   // Re-actiuate the event
                                m_mapNextPlaceEventBlock.put(player.getUniqueId(), _block);
                                eventCancellable.setCancelled(false);
                            }))
                            {   // Ok, done. Should I continue?
                                bRet = true;
                                break;
                            }
                        }
                    }
                }
            }
        }
        return bRet;
    }

    /**
     * Display informations about material.
     *
     * @param _strMaterialType Type, like __FIRE__, __FIELD__, etc.
     */
    public void displayMaterials(String _strMaterialType)
    {
        if (m_mapMaterialManagers.containsKey(_strMaterialType))
        {   // If exists, display
            ((IMaterialManager) m_mapMaterialManagers.get(_strMaterialType)).displayMaterials();
        }
        else
        {   // If not exists, warn the user
            m_plugin.getLogger().warning("Unknown material type = " + _strMaterialType);
        }
    }
}
