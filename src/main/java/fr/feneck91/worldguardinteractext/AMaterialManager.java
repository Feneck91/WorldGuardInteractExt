package fr.feneck91.worldguardinteractext;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.ComponentBuilder;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.format.TextColor;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
     * Read material with extra informations.
     *
     * @param _strKeyName Key name to retrieve the list of materials informations (name / regex and extra informations).
     * @param _mapItems Map where to find informations.
     * @param _lstMaterialsInformation Materials information list to fill.
     * @param _bIsEmptyAllowed If empty is not allowed, return false if the key doesn't exists, else just
     *                         not fill _lstMaterialsInformation.
     * @param _lambdaIsMaterialInformationValid Lambda to check if the material information is valid for this search.
     *                                          If null all is allowed.
     * @return true if no error, false else.
     */
    protected boolean readMaterial(String _strKeyName, Map<String, Object> _mapItems, List<MaterialInformation> _lstMaterialsInformation, boolean _bIsEmptyAllowed, Function<MaterialInformation, Boolean> _lambdaIsMaterialInformationValid)
    {
        boolean bRet = true;
        if (_mapItems.containsKey(_strKeyName))
        {
            List<MaterialInformation> lstMaterialsInformation = findMaterialsWithProperties(_strKeyName, _mapItems, _lambdaIsMaterialInformationValid,
                (MaterialInformation materialInformation) ->
                    {
                        getPlugin().getLogger().info("Configuration " + getMaterialType() + ": add " + _strKeyName + ": " + materialInformation.toString());
                    },
                (MaterialInformation materialInformation) ->
                    {
                        getPlugin().getLogger().warning("Configuration " + getMaterialType() + ": found '" + materialInformation.toString() + "' that is not valid to " + _strKeyName + "material ignored. ");
                    }
            );
            if (lstMaterialsInformation != null)
            {
                _lstMaterialsInformation.addAll(lstMaterialsInformation);
            }
            else
            {
                bRet = false;
            }
        }
        else if (!_bIsEmptyAllowed)
        {
            bRet = false;
        }

        return bRet;
    }

    /**
     * Get a list of materials from a _listMaterialNames.
     *
     * @param _strKeyName Key name to retrieve the list of materials informations (name / regex and extra informations).
     * @param _mapItems Map where to find informations.
     * @param _lambdaIsMaterialValid Lambda to check if the material is valid for this search.
     * @return An allowed material list.
     */
    protected List<Material> findMaterials(String _strKeyName, Map<String, Object> _mapItems, Function<Material, Boolean> _lambdaIsMaterialValid)
    {
        List<Material> listMaterials = new ArrayList<Material>();
        List<MaterialInformation> lstMaterialsInformation = new ArrayList<MaterialInformation>();
        if (readMaterial(_strKeyName, _mapItems, lstMaterialsInformation, false, (MaterialInformation itemMaterialInformation) ->
            {
                return    itemMaterialInformation != null
                       && itemMaterialInformation.getMaterial() != null
                       &&  itemMaterialInformation.getProperties().isEmpty()
                       && _lambdaIsMaterialValid.apply(itemMaterialInformation.getMaterial());
            }))
        {
            listMaterials.addAll(lstMaterialsInformation.stream()   // Use stream
                .map(MaterialInformation::getMaterial)              // Transfor each MaterialInformation to Material
                .toList());                                         // Collect to List<Material>
        }

        return listMaterials;
    }

    /**
     * Get a list of materials from a name.
     *
     * @param _strKeyName Key name to retrieve the list of materials information (name / regex and extra information).
     * @param _mapItems Map where to find information.
     * @param _lambdaIsMaterialInformationValid Lambda to check if the material information is valid for this search.
     *                                          If null all is allowed.
     * @param _lambdaInfoAdd Lambda to display log that this material is added.
     * @param _lambdaInfoInvalid Lambda to display log that this material is not added (but should be).
     * @return An allowed material information list.
     */
    private List<MaterialInformation> findMaterialsWithProperties(String _strKeyName, Map<String, Object> _mapItems, Function<MaterialInformation, Boolean> _lambdaIsMaterialInformationValid, Consumer<MaterialInformation> _lambdaInfoAdd, Consumer<MaterialInformation> _lambdaInfoInvalid)
    {
        ArrayList<Object> listMaterialInformations = (ArrayList<Object>) _mapItems.get(_strKeyName);
        List<MaterialInformation> listMaterialsInformations = new ArrayList<MaterialInformation>();
        Map<String, String> dicProperties = new HashMap<String, String>();

        // Read all materials
        if (listMaterialInformations.isEmpty())
        {   // Empty list, take all material that are valid
            for (Material itemMaterial : Material.values())
            {
                MaterialInformation materialInformation = new MaterialInformation(itemMaterial, null);
                if (_lambdaIsMaterialInformationValid == null || _lambdaIsMaterialInformationValid.apply(materialInformation))
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
     * Get a list of regions names from a _listRegionsNames.
     *
     * @param _listRegionsNames List of regions names or regex. If empty, take all regions.
     * @param _lambdaInfoAdd Lambda to display log that this region is added.
     * @param _lambdaInfoInvalid Lambda to display log that this region is not added (but should be because it is into the list).
     * @return A regions list names.
     */
    protected Set<String> findRegions(List<String> _listRegionsNames, Consumer<String> _lambdaInfoAdd, Consumer<String> _lambdaInfoInvalid)
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

    /**
     * Make the keys for all material managers with world name.
     *
     * @param _world WorldGuard World name.
     * @param _strRegionName WorldGuard Region name.
     * @param _material Used material, must be not null.
     * @return The formatted key.
     */
    protected static String MakeKey(World _world, String _strRegionName, Material _material)
    {
        return MakeKey(_world.getName() + "." + _strRegionName, _material);
    }

    /**
     * Make the keys for all material managers with world name and region.
     *
     * @param _strWorldAndRegionName WorldGuard World name with region name.
     * @param _material Used material, must be not null.
     * @return The formatted key.
     */
    protected static String MakeKey(String _strWorldAndRegionName, Material _material)
    {
        return _strWorldAndRegionName + ".__." + _material.name();
    }

    /**
     * Transform a string with placeholders to WorldEdit Component.
     * <p>
     * Replace each placeholder to a Component passed into parameter and apply a color based on legacy codes (§).
     * Used to be able to crete message with translated strings.
     * </p>
     *
     * @param _strTemplateText Text template with colors, ex: "§cFill the &lt;cauldron&gt; with &lt;material&gt; is forbidden."
     * @param _mapPlaceholders Placeholders map, ex: "cauldron" -&gt; TranslatableComponent.of(_block.getTranslationKey()).
     * @return Component WorldEdit message ready to send.
     */
    public static Component formatPlaceHolders(String _strTemplateText, Map<String, Component> _mapPlaceholders)
    {
        List<Component> lstComponents = new ArrayList<>();

        TextColor currentColor = null;

        StringBuilder sb = new StringBuilder();
        int iPos = 0;
        int iLength = _strTemplateText.length();

        while (iPos < iLength)
        {
            char c = _strTemplateText.charAt(iPos);

            if (c == '§' && iPos + 1 < iLength)
            {
                if (!sb.isEmpty())
                {
                    lstComponents.add(colorizeText(sb.toString(), currentColor));
                    sb = new StringBuilder();
                }
                currentColor = fromLegacyColorCode(_strTemplateText.charAt(iPos + 1));
                iPos += 2;
                continue;
            }
            else if (c == '<')
            {
                if (!sb.isEmpty())
                {
                    lstComponents.add(colorizeText(sb.toString(), currentColor));
                    sb = new StringBuilder();
                }

                int iEndPlaceholder = _strTemplateText.indexOf('>', iPos);
                if (iEndPlaceholder == -1)
                {
                    sb.append(_strTemplateText.substring(iPos));
                    break;
                }

                String strKey = _strTemplateText.substring(iPos + 1, iEndPlaceholder);
                Component compReplacement = _mapPlaceholders.get(strKey);

                if (compReplacement != null)
                {
                    if (currentColor != null)
                    {
                        compReplacement = compReplacement.color(currentColor);
                    }
                    lstComponents.add(compReplacement);
                }
                else
                {
                    lstComponents.add(colorizeText("<" + strKey + ">", currentColor));
                }

                iPos = iEndPlaceholder + 1;
                continue;
            }
            else
            {
                sb.append(c);
                iPos++;
            }
        }

        if (!sb.isEmpty())
        {
            lstComponents.add(colorizeText(sb.toString(), currentColor));
        }

        Component result = TextComponent.of("");

        for (Component comp : lstComponents)
        {
            result = result.append(comp);
        }

        return result;
    }

    /**
     * Make a colorized component string from a string.
     *
     * @param _strText Input string to colorized.
     * @param _colorText Color to use.
     * @return A colorized Component.
     */
    private static Component colorizeText(String _strText, TextColor _colorText)
    {
        TextComponent retComponent;
        if (_strText.isEmpty())
        {
            retComponent = TextComponent.of("");
        }
        else
        {
            retComponent = TextComponent.of(_strText);
            if (_colorText != null)
            {
                retComponent = retComponent.color(_colorText);
            }
        }

        return retComponent;
    }

    /**
     * Get text color from char.
     *
     * @param _cCode Char code.
     * @return A text color that ce input char represent.
     */
    private static TextColor fromLegacyColorCode(char _cCode)
    {
        return switch (Character.toLowerCase(_cCode))
        {
            case '0' -> TextColor.BLACK;
            case '1' -> TextColor.DARK_BLUE;
            case '2' -> TextColor.DARK_GREEN;
            case '3' -> TextColor.DARK_AQUA;
            case '4' -> TextColor.DARK_RED;
            case '5' -> TextColor.DARK_PURPLE;
            case '6' -> TextColor.GOLD;
            case '7' -> TextColor.GRAY;
            case '8' -> TextColor.DARK_GRAY;
            case '9' -> TextColor.BLUE;
            case 'a' -> TextColor.GREEN;
            case 'b' -> TextColor.AQUA;
            case 'c' -> TextColor.RED;
            case 'd' -> TextColor.LIGHT_PURPLE;
            case 'e' -> TextColor.YELLOW;
            case 'f' -> TextColor.WHITE;
            default -> TextColor.WHITE;
        };
    }

    /**
     * Pad a string to the right with space.
     *
     * @param _strText Input texte.
     * @param iLength Full string size wanted.
     * @return A padded string.
     */
    public static String padRight(String _strText, int iLength)
    {
        return (_strText.length() >= iLength)
            ?  _strText
            : _strText + " ".repeat(iLength - _strText.length());
    }
}