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
     *
     * The key is the super type (__FIRE__, __FIELD__, etc)
     */
    private HashMap<String, IMaterialManager> m_mapMaterialManagers;

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
                if (m_plugin.isVerboseLogEnabled())
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
                            interactEventInfos = materialManager.managePlayerInteraction(_event, block, world, strCurrentPlayerRegionName);
                            if (interactEventInfos != null)
                            {
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
