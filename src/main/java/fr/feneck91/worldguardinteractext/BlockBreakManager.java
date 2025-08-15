package fr.feneck91.worldguardinteractext;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Class that implements MaterialManager to allow player to break some blocks with specific tools.
 */
public class BlockBreakManager extends AMaterialManager implements IMaterialManager
{
    /**
     * Material type that this class manage.
     * <p>
     * It manages block break.
     * </p>
     */
    public static final String MATERIAL_TYPE = "__BLOCKBREAK__";

    /**
     * Class that manage all information about block breaking.
     */
    private static class InformationBlockBreakMaterial
    {
        /**
         * List of allowed regions.
         */
        public Set<String> m_lstRegions;

        /**
         * Material to survey (breakable blocks).
         */
        public Set<Material> m_lstMaterials;

        /**
         * Allowed material to use break the block.
         */
        public List<MaterialInformation> m_lstToolsMaterials;
    };

    /**
     * List of map informations.
     */
    private final Map<String, BlockBreakManager.InformationBlockBreakMaterial> m_mapInformationBlockBreakMaterial;

    /**
     * Constructor.
     *
     * @param _plugin Plugin, used to access logger ot other things.
     */
    public BlockBreakManager(WorldGuardInteractExt _plugin)
    {
        super(_plugin);
        m_mapInformationBlockBreakMaterial = new HashMap<String, BlockBreakManager.InformationBlockBreakMaterial>();
    }

    /**
     * Get Material type like __CAULDRON__, __FIELD__, etc.
     *
     * @return tye material type.
     */
    @Override
    public String getMaterialType()
    {
        return MATERIAL_TYPE;
    }

    /**
     * Ask if this material is valid for this type.
     *
     * @param _material Material to test.
     * @return Always false for all not solid material, else true (cannot be able to know if the material is
     *         breakable or not).
     */
    @Override
    public boolean isMaterialValidForType(Material _material)
    {
        return _material.isSolid();
    }

    /**
     * Read a piece of configuration about block break.
     *
     * @param _mapItems Maps items Config to read.
     * @param _logger Wrap class to log to sender if provide from a command, used to write message to info logger.
     * @return true if _mapItems is read without fatal error (but could be ignored), false else.
     */
    @Override
    public boolean readConfig(Map<String, Object> _mapItems, LoggerDispatcher _logger)
    {
        boolean bRet = true;
        List<Material> listMaterial = null;
        List<MaterialInformation> lstToolMaterials = new ArrayList<MaterialInformation>();

        // Get regions list
        Set<String> lstRegions = null;
        if (_mapItems.get("regions") instanceof List<?> listRegions)
        {
            List<String> regions = new ArrayList<>();
            for (Object o : (List<?>) listRegions)
            {
                if (o instanceof String strRegion)
                {
                    regions.add(strRegion);
                }
            }
            lstRegions = findRegions(_logger, regions,
                    (String strRegionName) -> { _logger.sendInfoMessage("Configuration " + getMaterialType() + ": add region '" + strRegionName + "'"); },
                    (String strRegionName) -> { _logger.sendWarningMessage("Configuration " + getMaterialType() + ": found '" + strRegionName + "' more than once, second is ignored"); }
            );
        }
        listMaterial = findMaterials(_logger, false,"names", _mapItems, this::isMaterialValidForType);
        if (listMaterial.isEmpty())
        {
            _logger.sendWarningMessage("Configuration " + getMaterialType() + ": found no item!");
            // bRet = false; No, not a critical error, just ignore __BLOCKBREAK__ configuration
        }
        else
        {   // Read tool material
            Function<MaterialInformation, Boolean> lamdaCheckIsValidTool =
                (MaterialInformation materialInformation) ->
                    {   // Check if MaterialInformation is valid
                        boolean bRetValidMaterialInfo = false;

                        if (materialInformation != null && materialInformation.getMaterial() != null)
                        {
                            // For tools only some extra properties are allowed
                            bRetValidMaterialInfo = materialInformation.getProperties()
                                .keySet()
                                .stream()
                                .allMatch((strPropName) ->
                                      strPropName.equals("name")
                                   || strPropName.equals("lore"));
                        }

                        return bRetValidMaterialInfo;
                    };

            if (    !readMaterial(_logger, false, "tool", _mapItems, lstToolMaterials, true, lamdaCheckIsValidTool))
            {
                bRet = false;
            }
            if (lstToolMaterials.isEmpty())
            {   // Should have at least one tool
                _logger.sendWarningMessage("Configuration " + getMaterialType() + ": no material found for tool, ignored!");
                // bRet = false; No, not a critical error, just ignore __CAULDRON__ configuration
            }
            if (lstRegions == null || lstRegions.isEmpty())
            {
                _logger.sendWarningMessage("Configuration " + getMaterialType() + ": no region found, ignored!");
            }
            else
            {   // All is OK, add it
                BlockBreakManager.InformationBlockBreakMaterial infos = new BlockBreakManager.InformationBlockBreakMaterial();
                infos.m_lstMaterials                = new HashSet<>(listMaterial);
                infos.m_lstToolsMaterials           = lstToolMaterials;
                infos.m_lstRegions                  = lstRegions;
                // To optimize time search, combine world name with material
                for (String strWorldAndRegionName : lstRegions)
                {
                    for (Material material : listMaterial)
                    {
                        String strKey = MakeKey(strWorldAndRegionName, material);
                        if (m_mapInformationBlockBreakMaterial.containsKey(strKey))
                        {
                            _logger.sendErrorMessage("Configuration " + getMaterialType() + " failed to load: more than one material (" + material.name() + ") used for same world / region (" + strWorldAndRegionName + ")!");
                            bRet = false;
                            break;
                        }
                        m_mapInformationBlockBreakMaterial.put(strKey, infos);
                    }
                    if (!bRet)
                    {
                        break;
                    }
                }
            }
        }

        return bRet;
    }

    /**
     * Display material available for this material type.
     *
     * @param _logger Wrap class to log to sender if provide from a command, used to write message to info logger.
     */
    @Override
    public void displayMaterials(LoggerDispatcher _logger)
    {
        _logger.sendMessage("Display material for " + getMaterialType());
        for (Material material : Material.values())
        {
            if (material.isSolid())
            {
                _logger.sendMessage(material.name());
            }
        }
        _logger.sendMessage("No specific material, all breakable material are allowed (only solid)");
    }

    /**
     * Manage player interaction
     *
     * @param _event Generic event.
     * @param _block Block that the user clic.
     * @param _world Current player world.
     * @param _strCurrentPlayerRegionName Current region name where player is located actually.
     * @return InteractEventsInfos if something is done, null else.
     */
    @Override
    public InteractEventManager.InteractEventsInfos managePlayerInteraction(Event _event, Block _block, World _world, String _strCurrentPlayerRegionName)
    {
        InteractEventManager.InteractEventsInfos interactEventsInfos = null;
        ItemStack itemHand;
        Player player = null;

        if (_event instanceof PlayerInteractEvent playerInteractEvent)
        {
            player = playerInteractEvent.getPlayer();
            itemHand = player.getInventory().getItemInMainHand();
        }
        else
        {
            itemHand = null;
        }

        if (itemHand != null)
        {   // If nothing in hand, nothing to do
            String strKey = MakeKey(_world, _strCurrentPlayerRegionName, _block.getType());
            if (m_mapInformationBlockBreakMaterial.containsKey(strKey))
            {
                BlockBreakManager.InformationBlockBreakMaterial infosBlockBreak = m_mapInformationBlockBreakMaterial.get(strKey);
                final Material toolMaterial;   // Can be handItem material or other if handItem is null
                toolMaterial = itemHand.getType();
                // Create lambda function
                Predicate<? super MaterialInformation> lambaIsAllowedMaterial = (MaterialInformation item) ->
                {
                    boolean bRet = false;
                    if (toolMaterial.equals(item.getMaterial()))
                    {   // Check if has properties
                        if (item.getProperties().isEmpty())
                        {
                            bRet = true; // Ok this material is allowed to be used
                        }
                        else
                        {   // Check properties
                            bRet = true;
                            if (itemHand.hasItemMeta())
                            {   // Only if has META
                                final ItemMeta itemMeta = Objects.requireNonNull(itemHand.getItemMeta()); // Cannot be null here
                                bRet = item.getProperties().entrySet().stream().allMatch(prop ->
                                       (prop.getKey().equals("name") && itemMeta.hasDisplayName() && itemMeta.getDisplayName().equals(prop.getValue()))
                                    || (prop.getKey().equals("lore") && itemMeta.hasLore() && !itemMeta.getLore().isEmpty() && itemMeta.getLore().get(0).equals(prop.getValue()))
                                );
                            }
                        }
                    }
                    return bRet;
                };

                if (infosBlockBreak.m_lstToolsMaterials.stream().anyMatch(lambaIsAllowedMaterial))
                {
                    interactEventsInfos = new InteractEventManager.InteractEventsInfos(player, _block);
                    interactEventsInfos.addEventInfos(new InteractEventManager.InteractEventsInfos.EventInfos(PlayerInteractEvent.class, EventPriority.LOWEST, InteractEventManager.InteractEventsInfos.EventInfos.eCancelType.eCancelTypeIgnore, null));
                    interactEventsInfos.addEventInfos(new InteractEventManager.InteractEventsInfos.EventInfos(PlayerInteractEvent.class, EventPriority.HIGHEST, InteractEventManager.InteractEventsInfos.EventInfos.eCancelType.eCancelTypeIgnore,null));
                    interactEventsInfos.addEventInfos(new InteractEventManager.InteractEventsInfos.EventInfos(BlockBreakEvent.class, EventPriority.LOWEST, InteractEventManager.InteractEventsInfos.EventInfos.eCancelType.eCancelTypeCancel, null));
                    interactEventsInfos.addEventInfos(new InteractEventManager.InteractEventsInfos.EventInfos(BlockBreakEvent.class, EventPriority.HIGHEST, InteractEventManager.InteractEventsInfos.EventInfos.eCancelType.eCancelTypeUncancel,null));
                }
            }
        }

        return interactEventsInfos;
    }
}
