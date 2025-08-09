package fr.feneck91.worldguardinteractext;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Base class for all MaterialManagers class.
 */
public abstract class AMaterialManager  implements IMaterialManager
{
    /**
     * Class used from derived class to record material information with more properties.
     */
    protected static class MaterialInformation
    {
        /**
         * Material.
         */
        private final Material m_material;

        /**
         * Contains properties for this material like color, text, author, depending on material type.
         */
        private final Map<String, String> m_dicProperties;

        /**
         * Constructor.
         *
         * @param _material Material.
         * @param _dicProperties Extra properties, can be null.
         */
        public MaterialInformation(Material _material, Map<String, String> _dicProperties)
        {
            m_material = _material;
            m_dicProperties = _dicProperties == null ? new HashMap<String, String>() : _dicProperties;
        }

        /**
         * Get the material.
         *
         * @return The material.
         */
        public Material getMaterial()
        {
            return m_material;
        }

        /**
         * Get a map with extended properties.
         *
         * @return All the properties.
         */
        public Map<String, String> getProperties()
        {
            return m_dicProperties;
        }

        /**
         * Convert to string.
         *
         * @return A string that represent the class content.
         */
        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder();
            sb.append(m_material.name());
            if (!getProperties().isEmpty())
            {
                sb.append(" (");
                boolean bAddComma = false;
                for (Map.Entry<String, String> entryProperties : getProperties().entrySet())
                {
                    sb.append(bAddComma ? ", " : "")
                      .append(entryProperties.getKey())
                      .append(" : \"")
                      .append(entryProperties.getValue())
                      .append("\"");
                    bAddComma = true;
                }
                sb.append(")");
            }

            return sb.toString();
        }
    };

    /**
     * Instance of plugin.
     */
    private final WorldGuardInteractExt m_plugin;

    /**
     * Constructor.
     *
     * @param _plugin Plugin, used to access logger ot other things.
     */
    public AMaterialManager(WorldGuardInteractExt _plugin)
    {
        m_plugin = _plugin;
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
     * Get a list of materials from a name.
     *
     * @param _strKeyName Key name to retrieve the list of materials informations (name / regex and extra informations).
     * @param _mapItems Map where to find informations.
     * @param _lambdaIsMaterialInformationValid Lambda to check if the material information is valid for this search.
     * @param _lambdaInfoAdd Lambda to display log that this material is added.
     * @param _lambdaInfoInvalid Lambda to display log that this material is not added (but should be).
     * @return An allowed material informations list.
     */
    protected List<MaterialInformation> findMaterialsX(String _strKeyName, Map<String, Object> _mapItems, Function<MaterialInformation, Boolean> _lambdaIsMaterialInformationValid, Consumer<MaterialInformation> _lambdaInfoAdd, Consumer<MaterialInformation> _lambdaInfoInvalid)
    {
        if (_lambdaIsMaterialInformationValid == null)
        {
            throw new NullPointerException("AMaterialManager::findMaterials : _lambdaIsMaterialInformationValid argument is null");
        }
        ArrayList<Object> listMaterialInformations = (ArrayList<Object>) _mapItems.get(_strKeyName);
        List<MaterialInformation> listMaterialsInformations = new ArrayList<MaterialInformation>();
        Map<String, String> dicProperties = new HashMap<String, String>();

        // Read all materials
        if (listMaterialInformations.isEmpty())
        {   // Empty list, take all material that are valid
            for (Material itemMaterial : Material.values())
            {
                MaterialInformation materialInformation = new MaterialInformation(itemMaterial, null);
                if (_lambdaIsMaterialInformationValid.apply(materialInformation))
                {
                    listMaterialsInformations.add(materialInformation);
                    if (getPlugin().isVerboseLogEnabled() && _lambdaInfoAdd != null)
                    {
                        _lambdaInfoAdd.accept(materialInformation);
                    }
                }
            }
        }
        else
        {
            Material itemMaterialFound = null;
            String strItemName = null; // Name or regex

            for (Object oItem : listMaterialInformations)
            {
                if (oItem instanceof String strItemNameToFind)
                {   // Only a material name
                    itemMaterialFound = Material.getMaterial(strItemNameToFind);
                    strItemName = strItemNameToFind;
                }
                else if (oItem instanceof HashMap<?, ?> dicItemsToCheck)
                {   // Material name with others values
                    if (   dicItemsToCheck.keySet().stream().allMatch(k -> k instanceof String)
                        && dicItemsToCheck.values().stream().allMatch(v -> v instanceof String))
                    {   // All keys / values are String
                        HashMap<String, String> dicItems = (HashMap<String, String>) dicItemsToCheck;
                        if (dicItems.containsKey("material"))
                        {
                            strItemName = dicItems.get("material");
                            itemMaterialFound = Material.getMaterial(strItemName);

                            for (Map.Entry<String, String> entryProperties : dicItems.entrySet())
                            {
                                if (!entryProperties.getKey().equals("material"))
                                {
                                    if (!dicProperties.containsKey(entryProperties.getKey()))
                                    {
                                        dicProperties.put(entryProperties.getKey(), entryProperties.getValue());
                                    }
                                    else
                                    {
                                        getPlugin().getLogger().warning("Configuration " + getMaterialType() + ": more than one key '" + entryProperties.getKey() +"' is filled, only first is keep!");
                                    }
                                }
                            }
                        }
                        else
                        {
                            getPlugin().getLogger().warning("Configuration " + getMaterialType() + ": bad configuration for key '" +  _strKeyName + "', subkey 'material' is mandatory for dictionary informations, configuration is ignored!");
                            listMaterialsInformations = null;
                            break;
                        }
                    }
                    else
                    {
                        getPlugin().getLogger().warning("Configuration " + getMaterialType() + ": bad configuration for key '" +  _strKeyName + "', configuration ignored!");
                        listMaterialsInformations = null;
                        break;
                    }
                }
                else
                {
                    getPlugin().getLogger().warning("Configuration " + getMaterialType() + ": bad configuration for key '" +  _strKeyName + "', configuration ignored!");
                    listMaterialsInformations = null;
                    break;
                }

                if (itemMaterialFound == null)
                {   // Not found, try to find with regex
                    Pattern pattern = Pattern.compile(strItemName); // CASE_SENSITIVE

                    for (Material itemMaterial : Material.values())
                    {
                        if (pattern.matcher(itemMaterial.name()).find())
                        {
                            MaterialInformation materialInformation = new MaterialInformation(itemMaterial, dicProperties);
                            if (_lambdaIsMaterialInformationValid.apply(materialInformation))
                            {
                                listMaterialsInformations.add(materialInformation);
                                if (getPlugin().isVerboseLogEnabled())
                                {
                                    _lambdaInfoAdd.accept(materialInformation);
                                }
                            }
                            else
                            {   // Not valid, ignore it
                                _lambdaInfoInvalid.accept(new MaterialInformation(itemMaterial, null));
                            }
                        }
                        // else : ignore it
                    }
                }
                else
                {
                    MaterialInformation materialInformation = new MaterialInformation(itemMaterialFound, dicProperties);
                    if (_lambdaIsMaterialInformationValid.apply(materialInformation))
                    {
                        listMaterialsInformations.add(materialInformation);
                        if (getPlugin().isVerboseLogEnabled())
                        {
                            _lambdaInfoAdd.accept(materialInformation);
                        }
                    }
                    else
                    {   // Not valid, ignore it
                        _lambdaInfoInvalid.accept(materialInformation);
                    }
                }
            }
        }

        return listMaterialsInformations;
    }

    /**
     * Get a list of materials from a _listMaterialNames.
     *
     * @param _listMaterialNames List of materials names or regex.If empty, take all possible.
     * @param _lambdaIsValid Lambda to check if the material is valid for this search.
     * @param _lambdaInfoAdd Lambda to display log that this material is added.
     * @param _lambdaInfoInvalid Lambda to display log that this material is not added (but should be).
     * @return An allowed material list.
     */
    protected List<Material> findMaterials(ArrayList<String> _listMaterialNames, Function<Material, Boolean> _lambdaIsValid, Consumer<Material> _lambdaInfoAdd, Consumer<Material> _lambdaInfoInvalid)
    {
        if (_lambdaIsValid == null)
        {
            throw new NullPointerException("AMaterialManager::findMaterials : _lambdaIsValid argument is null");
        }
        List<Material> listMaterials = new ArrayList<Material>();

        // Read all materials
        if (_listMaterialNames.isEmpty())
        {   // Empty list, take all material that are valid
            for (Material itemMaterial : Material.values())
            {
                if (_lambdaIsValid.apply(itemMaterial))
                {
                    if (getPlugin().isVerboseLogEnabled() && _lambdaInfoAdd != null)
                    {
                        _lambdaInfoAdd.accept(itemMaterial);
                    }
                    listMaterials.add(itemMaterial);
                }
            }
        }
        else
        {
            for (String strItemName : _listMaterialNames)
            {
                Material itemMaterialFound = Material.getMaterial(strItemName);
                if (itemMaterialFound == null)
                {   // Not found, try to find with regex
                    Pattern pattern = Pattern.compile(strItemName); // CASE_SENSITIVE

                    for (Material itemMaterial : Material.values())
                    {
                        if (pattern.matcher(itemMaterial.name()).find())
                        {
                            if (_lambdaIsValid.apply(itemMaterial))
                            {
                                if (getPlugin().isVerboseLogEnabled())
                                {
                                    _lambdaInfoAdd.accept(itemMaterial);
                                }
                                listMaterials.add(itemMaterial);
                            }
                            else
                            {   // Not valid, ignore it
                                _lambdaInfoInvalid.accept(itemMaterial);
                            }
                        }
                        // else : ignore it
                    }
                }
                else if (_lambdaIsValid.apply(itemMaterialFound))
                {
                    if (getPlugin().isVerboseLogEnabled())
                    {
                        _lambdaInfoAdd.accept(itemMaterialFound);
                    }
                    listMaterials.add(itemMaterialFound);
                }
                else
                {   // Not valid, ignore it
                    _lambdaInfoInvalid.accept(itemMaterialFound);
                }
            }
        }

        return listMaterials;
    }

    /**
     * Get a list of regions names from a _listRegionsNames.
     *
     * @param _listRegionsNames List of regions names or regex. If empty, take all regions.
     * @param _lambdaInfoAdd Lambda to display log that this region is added.
     * @param _lambdaInfoInvalid Lambda to display log that this region is not added (but should be because it is into the list).
     * @return A regions list names.
     */
    protected Set<String> findRegions(ArrayList<String> _listRegionsNames, Consumer<String> _lambdaInfoAdd, Consumer<String> _lambdaInfoInvalid)
    {
        HashSet<String> listRegions = new HashSet<String>();

        // Read all WorldGuard regions
        Map<String, ProtectedRegion> wgRegions = getAllWorldGuardRegions();

        // Read all regions
        if (_listRegionsNames.isEmpty())
        {   // Empty list, take all regions
            for (String strRegionNameItem : wgRegions.keySet())
            {
                if (listRegions.contains(strRegionNameItem))
                {   // Warning
                    _lambdaInfoInvalid.accept(strRegionNameItem);
                }
                else
                {   // Add it
                    if (getPlugin().isVerboseLogEnabled())
                    {
                        _lambdaInfoAdd.accept(strRegionNameItem);
                    }
                    listRegions.add(strRegionNameItem);
                }
            }
        }
        else
        {   // Find all regions give by user
            for (String strRegionName : _listRegionsNames)
            {
                if (wgRegions.containsKey(strRegionName))
                {
                    if (listRegions.contains(strRegionName))
                    {   // Warning
                        _lambdaInfoInvalid.accept(strRegionName);
                    }
                    else
                    {   // Add it
                        if (getPlugin().isVerboseLogEnabled())
                        {
                            _lambdaInfoAdd.accept(strRegionName);
                        }
                        listRegions.add(strRegionName);
                    }
                }
                else
                {   // Not found, try to find with regex
                    Pattern pattern = Pattern.compile(strRegionName); // CASE_SENSITIVE

                    for (String strRegionNameItem : wgRegions.keySet())
                    {
                        if (pattern.matcher(strRegionNameItem).find())
                        {
                            if (listRegions.contains(strRegionNameItem))
                            {   // Warning
                                _lambdaInfoInvalid.accept(strRegionNameItem);
                            }
                            else
                            {   // Add it
                                if (getPlugin().isVerboseLogEnabled())
                                {
                                    _lambdaInfoAdd.accept(strRegionNameItem);
                                }
                                listRegions.add(strRegionNameItem);
                            }
                        }
                        // else : ignore it
                    }
                }
            }
        }

        return listRegions;
    }

    /**
     * Get all regions defines by WorldGuard.
     *
     * @return A map that contains a key like <world_name>.<region_name>n and a value that is the  region informations.
     */
    Map<String, ProtectedRegion> getAllWorldGuardRegions()
    {
        Map<String, ProtectedRegion> mapRegions = new HashMap<String, ProtectedRegion>();

        RegionContainer containerRegion = WorldGuard.getInstance().getPlatform().getRegionContainer();
        // Fusion all worlds
        for (World world : Bukkit.getWorlds())
        {
            RegionManager managerRegion = containerRegion.get(BukkitAdapter.adapt(world));
            if (managerRegion != null)
            {
                Map<String, ProtectedRegion> lstRegions = managerRegion.getRegions();
                if (lstRegions != null)
                {
                    for (Map.Entry<String, ProtectedRegion> entryRegionName : lstRegions.entrySet())
                    {
                        mapRegions.put(world.getName() + "." + entryRegionName.getKey(), entryRegionName.getValue());
                    }
                }
            }
        }

        return mapRegions;
    }
}
