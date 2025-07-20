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
import org.bukkit.event.player.PlayerInteractEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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
    private static HashMap<String, IMaterialManager> s_mapMaterialManagers;

    /**
     * Constructor.
     *
     * @param _plugin Plugin, used to access logger ot other things.
     */
    public MaterialConfig(WorldGuardInteractExt _plugin)
    {
        m_plugin = _plugin;
        if (s_mapMaterialManagers == null)
        {   // Initialize all available materials
            s_mapMaterialManagers = new HashMap<String, IMaterialManager>();
            s_mapMaterialManagers.put(FireMaterialManager.MATERIAL_TYPE, new FireMaterialManager(_plugin));
        }
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
     * Clear the content of the class.
     */
    public void clearAll()
    {
        s_mapMaterialManagers = null;
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

            if (s_mapMaterialManagers.containsKey(strMaterialType))
            {
                if (m_plugin.IsVerboseLogEnabled())
                {
                    m_plugin.getLogger().info("Reading material = " + strMaterialType);
                }
                bRet = ((IMaterialManager) s_mapMaterialManagers.get(strMaterialType)).readConfig(mapItems);
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
     * @param _event Player event. event.getClickedBlock() is not null else we don't go there.
     * @return true if one manage interaction, false else.
     */
    public boolean managePlayerInteraction(PlayerInteractEvent _event)
    {
        boolean bRet = false;
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        World world = _event.getPlayer().getWorld();
        Block block = _event.getClickedBlock();
        RegionManager manager = container.get(BukkitAdapter.adapt(world));
        if (manager != null)
        {
            ApplicableRegionSet regionSet = manager.getApplicableRegions(BukkitAdapter.asBlockVector(_event.getPlayer().getLocation()));
            Set<String> currentRegions = regionSet.getRegions().stream().map(ProtectedRegion::getId).collect(Collectors.toSet());
            for (IMaterialManager materialManager : s_mapMaterialManagers.values())
            {
                if (materialManager.managePlayerInteraction(_event, block, world, currentRegions))
                {   // Ok, done. Should I continue?
                    bRet = true;
                    break;
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
        if (s_mapMaterialManagers.containsKey(_strMaterialType))
        {   // If exists, display
            ((IMaterialManager) s_mapMaterialManagers.get(_strMaterialType)).displayMaterials();
        }
        else
        {   // If not exists, warn the user
            m_plugin.getLogger().warning("Unknown material type = " + _strMaterialType);
        }
    }
}
