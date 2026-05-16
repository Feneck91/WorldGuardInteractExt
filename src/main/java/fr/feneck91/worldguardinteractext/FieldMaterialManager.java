package fr.feneck91.worldguardinteractext;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Class that implements MaterialManager for fields management (plant / till).
 */
public class FieldMaterialManager extends AMaterialManager implements IMaterialManager
{
    /**
     * Material type that this class manage.
     * <p>
     * It handles the field management like plant, till. There is no way to filter allowed blocks and tools (and be sure); it is up to the server
     * administrator to choose the appropriate blocks and tools.
     * </p>
     */
    public static final String MATERIAL_TYPE = "__FIELD__";

    /**
     * Class that manage all information about fields: block allowed for plant (dirt / seeds) and tools (hoe), etc.
     */
    private static class InformationFieldMaterial
    {
        /**
         * List of allowed regions.
         */
        public Set<String> m_lstRegions;

        /**
         * Blocks materials allowed to be changed (target) for tillable.
         */
        public Set<Material> m_lstTillableMaterials;

        /**
         * Seeds materials allowed to plant (item in hand).
         */
        public Set<Material> m_lstPlantMaterials;

        /**
         * Materials allowed to be harvested.
         */
        public Set<Material> m_lstHarvestedMaterials;

        /**
         * Allowed material to use to harvesting (hoe).
         */
        public List<MaterialInformation> m_lstHarvestToolsMaterials;
    };

    /**
     * List of map information.
     */
    private final Map<String, InformationFieldMaterial> m_mapInformationFieldMaterial;

    /**
     * Static list of blocks that can be plowed with a hoe.
     */
    private static final Set<Material> TILLABLE_MATERIALS = Set.of(
        Material.DIRT,
        Material.GRASS_BLOCK,
        Material.ROOTED_DIRT,
        Material.COARSE_DIRT // Optional : depending on rules
    );

    /**
     * List of agricultural crops that can be planted on cultivated land.
     */
    private static final Set<Material> FARMLAND_PLANTABLE = Set.of(
        Material.WHEAT_SEEDS,
        Material.BEETROOT_SEEDS,
        Material.CARROT,
        Material.POTATO,
        Material.MELON_SEEDS,
        Material.PUMPKIN_SEEDS,
        Material.TORCHFLOWER_SEEDS,
        Material.PITCHER_POD
    );

    /**
     * List of blocks that can be planted on land (dirt) or cultivated land (hydrated farmland).
     */
    private static final Set<Material> DIRT_PLANTABLE = Set.of(
        // Saplings
        Material.ACACIA_SAPLING,
        Material.BAMBOO_SAPLING,
        Material.BIRCH_SAPLING,
        Material.CHERRY_SAPLING,
        Material.DARK_OAK_SAPLING,
        Material.JUNGLE_SAPLING,
        Material.OAK_SAPLING,
        Material.SPRUCE_SAPLING,
        Material.MANGROVE_PROPAGULE,

        // Plant and flower
        Material.FERN,
        Material.LARGE_FERN,
        Material.DEAD_BUSH,
        Material.AZALEA,
        Material.FLOWERING_AZALEA,

        // Mushroom (only brown and red can grow on dirt)
        Material.BROWN_MUSHROOM,
        Material.RED_MUSHROOM,

        // Other
        Material.SUGAR_CANE     // hydrated farmland or near water
    );

    /**
     * List of blocks that can be planted on specific blocks (not dirt or farmland):
     * - COCOA_BEANS: On wood (LOG, STRIPPED_LOG, etc.)
     * - CRIMSON_FUNGUS / WARPED_FUNGUS: On NETHERRACK or NYLIUM
     * - GLOW_LICHEN: On any solid block
     * - NETHER_WART: On soul sand
     * - VINE: On solid block with support above
     */
    private static final Set<Material> SPECIAL_PLANTABLE = Set.of(
        Material.COCOA_BEANS,   // On wood
        Material.CRIMSON_FUNGUS,// On netherrack or nylium
        Material.GLOW_LICHEN,   // On all solid blocks
        Material.NETHER_WART,   // On soul sand
        Material.VINE,          // On solid block, not directly on dirt / farmland block
        Material.WARPED_FUNGUS  // On netherrack or nylium
    );

    /**
     * Static list of blocks that can be harvested with a hoe (right-click).
     * These blocks must be mature to drop their products.
     */
    private static final Set<Material> HOE_HARVESTABLE = Set.of(
        Material.WHEAT,          // Drops WHEAT_SEEDS + WHEAT
        Material.CARROTS,        // Drops CARROT
        Material.POTATOES,       // Drops POTATO
        Material.BEETROOTS,      // Drops BEETROOT + BEETROOT_SEEDS
        Material.MELON,          // Drops MELON_SLICE (but hoe is not optimal)
        Material.PUMPKIN,        // Drops PUMPKIN (but hoe is not optimal)
        Material.NETHER_WART,    // Drops NETHER_WART
        Material.TORCHFLOWER,    // Drops TORCHFLOWER
        Material.PITCHER_PLANT   // Drops PITCHER_POD
    );

    /**
     * Constructor.
     *
     * @param _plugin Plugin, used to access logger ot other things.
     */
    public FieldMaterialManager(WorldGuardInteractExt _plugin)
    {
        super(_plugin);
        m_mapInformationFieldMaterial = new HashMap<String, InformationFieldMaterial>();
    }

    /**
     * Get Material type like __CAMPFIRE__, __FIELD__, etc.
     *
     * @return Material type.
     */
    @Override
    public String getMaterialType()
    {
        return MATERIAL_TYPE;
    }

    /**
     * Ask if this material is valid for this type.
     * Tha valid type is all blocks that can be tillable.
     *
     * @param _material Material to test.
     * @return true if this material is valid, false else.
     */
    @Override
    public boolean isMaterialValidForType(Material _material)
    {
        return _material != null && (TILLABLE_MATERIALS.contains(_material));
    }

    /**
     * Ask if this material information is valid for plant.
     *
     * @param _material Material  to test.
     * @return true if this material information is valid, false else.
     */
    private boolean isMaterialValidForPlantable(Material _material)
    {
        return    _material != null
               && (   FieldMaterialManager.FARMLAND_PLANTABLE.contains(_material)
                   || FieldMaterialManager.DIRT_PLANTABLE.contains(_material)
                   || FieldMaterialManager.SPECIAL_PLANTABLE.contains(_material)
                  );
    }

    /**
     * Ask if this material information is valid for harvest with hoe.
     *
     * @param _material Material to test.
     * @return true if this material information is valid, false else.
     */
    private boolean isMaterialValidForHarvestableWithHoe(Material _material)
    {
        return    _material != null
               && FieldMaterialManager.HOE_HARVESTABLE.contains(_material);
    }

    /**
     * Read a piece of configuration about camp fire.
     *
     * @param _mapItems Maps items Config to read.
     * @param _logger Wrap class to log to sender if provide from a command, used to write message to info logger.
     * @return true if _mapItems is read without fatal error (but could be ignored), false else.
     */
    @Override
    public boolean readConfig(Map<String, Object> _mapItems, LoggerDispatcher _logger)
    {
        boolean bRet = true;
        List<Material> lstTillableMaterials = null;
        List<Material> lstPlantMaterials = new ArrayList<Material>();
        List<Material> lstHarvestedMaterials = new ArrayList<Material>();
        List<MaterialInformation> lstHarvestToolsMaterials = new ArrayList<MaterialInformation>();

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
                    (String strRegionName) -> { _logger.sendMessage("Configuration " + getMaterialType() + ": add region '" + strRegionName + "'"); },
                    (String strRegionName) -> { _logger.sendWarningMessage("Configuration " + getMaterialType() + ": found '" + strRegionName + "' more than once, second is ignored"); }
            );

            lstTillableMaterials = findMaterials(_logger, true, "tillable_names", _mapItems, this::isMaterialValidForType);
            if (lstTillableMaterials.isEmpty())
            {
                _logger.sendWarningMessage("Configuration " + getMaterialType() + ": found no item!");
                // bRet = false; No, not a critical error, just ignore __FIELD__ configuration
            }
            else
            {
                if (lstRegions.isEmpty())
                {
                    _logger.sendWarningMessage("Configuration " + getMaterialType() + ": no region found, ignored!");
                }
                else
                {   // All is OK, add it
                    // Read plantable blocks
                    lstPlantMaterials = findMaterials(_logger,  true, "plantable_names", _mapItems, this::isMaterialValidForPlantable);
                    // Read harvestable with hoe
                    lstHarvestedMaterials = findMaterials(_logger,  true, "harvestable_names", _mapItems, this::isMaterialValidForHarvestableWithHoe);

                    Function<MaterialInformation, Boolean> lambdaCheckIsValidTool =
                        (MaterialInformation materialInformation) ->
                            {   // Check if MaterialInformation is valid
                                boolean bRetValidMaterialInfo = false;

                                if (materialInformation != null && materialInformation.getMaterial() != null)
                                {
                                    switch (materialInformation.getMaterial())
                                    {
                                        case Material.WOODEN_HOE:
                                        case Material.STONE_HOE:
                                        case Material.IRON_HOE:
                                        case Material.GOLDEN_HOE:
                                        case Material.DIAMOND_HOE:
                                        case Material.NETHERITE_HOE:
                                        {   // For hoe, only some extra properties are allowed
                                            bRetValidMaterialInfo = materialInformation.getProperties()
                                                .keySet()
                                                .stream()
                                                .allMatch((strPropName) ->
                                                      strPropName.equals("name")
                                                   || strPropName.equals("lore"));
                                            break;
                                        }
                                    }
                                    if (bRetValidMaterialInfo)
                                    {
                                        // For tools only some extra properties are allowed
                                        bRetValidMaterialInfo = materialInformation.getProperties()
                                            .keySet()
                                            .stream()
                                            .allMatch((strPropName) ->
                                                  strPropName.equals("name")
                                               || strPropName.equals("lore"));
                                    }
                                }

                                return bRetValidMaterialInfo;
                            };

                    if (!readMaterial(_logger, true, "tool", _mapItems, lstHarvestToolsMaterials, true, lambdaCheckIsValidTool))
                    {
                        bRet = false;
                    }
                    else
                    {
                        InformationFieldMaterial infos      = new InformationFieldMaterial();
                        infos.m_lstTillableMaterials        = new HashSet<>(lstTillableMaterials);
                        infos.m_lstPlantMaterials           = new HashSet<>(lstPlantMaterials);
                        infos.m_lstHarvestedMaterials       = new HashSet<>(lstHarvestedMaterials);
                        infos.m_lstHarvestToolsMaterials    = lstHarvestToolsMaterials;
                        infos.m_lstRegions                  = lstRegions;
                        // To optimize time search in events, combine world name with material
                        Set<Material> allMaterials = new HashSet<>();
                        allMaterials.addAll(infos.m_lstTillableMaterials);
                        allMaterials.addAll(infos.m_lstPlantMaterials);
                        allMaterials.addAll(infos.m_lstHarvestedMaterials);
                        for (String strWorldAndRegionName : lstRegions)
                        {
                            for (Material material : allMaterials)
                            {
                                String strKey = MakeKey(strWorldAndRegionName, material);
                                if (m_mapInformationFieldMaterial.containsKey(strKey))
                                {
                                    _logger.sendErrorMessage("Configuration " + getMaterialType() + " failed to load: more than one material (" + material.name() + ") used for same world / region (" + strWorldAndRegionName + ")!");
                                    bRet = false;
                                    break;
                                }
                                m_mapInformationFieldMaterial.put(strKey, infos);
                            }
                            if (!bRet)
                            {
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
     * Display material available for this material type (for tillable / plantable / harvestable).
     *
     * @param _logger Wrap class to log to sender if provide from a command, used to write message to info logger.
     */
    @Override
    public void displayMaterials(LoggerDispatcher _logger)
    {
        _logger.sendMessage("Display material for " + getMaterialType());
        // 1) List all tillable blocks
        _logger.sendMessage("Tillable blocks: ");
        for (Material material : Material.values())
        {
            if (isMaterialValidForType(material))
            {
                _logger.sendMessage("  - " + material.name());
            }
        }
        // 2) List all plantable blocks
        _logger.sendMessage("Plantable blocks: ");
        for (Material material : Material.values())
        {
            if (isMaterialValidForPlantable(material))
            {
                _logger.sendMessage("  - " + material.name());
            }
        }
        // 3) List all harvestable blocks
        _logger.sendMessage("Harvestable blocks (with hoe): ");
        for (Material material : Material.values())
        {
            if (isMaterialValidForHarvestableWithHoe(material))
            {
                _logger.sendMessage("  - " + material.name());
            }
        }
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

        final ItemStack itemHand;
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
            final Material itemHandMaterial = itemHand.getType();
            String strKey = MakeKey(_world, _strCurrentPlayerRegionName, isMaterialValidForPlantable(itemHandMaterial) ? itemHand.getType() : _block.getType());

            if (m_mapInformationFieldMaterial.containsKey(strKey))
            {
                FieldMaterialManager.InformationFieldMaterial infosField = m_mapInformationFieldMaterial.get(strKey);

                // Create lambda function to check if harvest tools is used (to till or to harvest)
                Predicate<? super MaterialInformation> lambaIsAllowedTillOrHarvestToolsMaterials = (MaterialInformation item) ->
                {
                    boolean bRet = false;
                    if (itemHandMaterial.equals(item.getMaterial()))
                    {   // Check if has properties
                        if (item.getProperties().isEmpty())
                        {
                            bRet = true; // Ok this material is allowed to be used
                        }
                        else
                        {   // Check properties
                            if (itemHand.hasItemMeta())
                            {   // Only if has META
                                final ItemMeta itemMeta = Objects.requireNonNull(itemHand.getItemMeta()); // Cannot be null here
                                bRet = item.getProperties().entrySet().stream().allMatch(prop ->
                                       (prop.getKey().equals("name") && itemMeta.hasDisplayName() && itemMeta.getDisplayName().equals(prop.getValue()))
                                    || (prop.getKey().equals("lore") && itemMeta.hasLore() && !itemMeta.getLore().isEmpty() && itemMeta.getLore().get(0).equals(prop.getValue()))
                                );
                            }
                            // else bRet is false -> meta is mandatory if item has properties
                        }
                    }
                    return bRet;
                };

                if (infosField.m_lstHarvestToolsMaterials.stream().anyMatch(lambaIsAllowedTillOrHarvestToolsMaterials))
                {   // The user is actually try to till or harvest
                    if (infosField.m_lstTillableMaterials.contains(_block.getType()))
                    {   // Try to till
                        interactEventsInfos = new InteractEventManager.InteractEventsInfos(player, _block);
                        interactEventsInfos.addEventInfos(new InteractEventManager.InteractEventsInfos.EventInfos(PlayerInteractEvent.class, EventPriority.LOWEST, InteractEventManager.InteractEventsInfos.EventInfos.eCancelType.eCancelTypeCancel, null));
                        interactEventsInfos.addEventInfos(new InteractEventManager.InteractEventsInfos.EventInfos(PlayerInteractEvent.class, EventPriority.HIGHEST, InteractEventManager.InteractEventsInfos.EventInfos.eCancelType.eCancelTypeUncancel,null));
                        // Here, if BlockBreakEvent => it break the block, we only want to till this block so it is BlockPlaceEvent
                        interactEventsInfos.addEventInfos(new InteractEventManager.InteractEventsInfos.EventInfos(BlockPlaceEvent.class, EventPriority.LOWEST, InteractEventManager.InteractEventsInfos.EventInfos.eCancelType.eCancelTypeCancel, null));
                        interactEventsInfos.addEventInfos(new InteractEventManager.InteractEventsInfos.EventInfos(BlockPlaceEvent.class, EventPriority.HIGHEST, InteractEventManager.InteractEventsInfos.EventInfos.eCancelType.eCancelTypeUncancel,null));
                    }
                    else if (infosField.m_lstHarvestedMaterials.contains(_block.getType()))
                    {   // Try to harvest (like breaking block)
                        interactEventsInfos = new InteractEventManager.InteractEventsInfos(player, _block);
                        interactEventsInfos.addEventInfos(new InteractEventManager.InteractEventsInfos.EventInfos(PlayerInteractEvent.class, EventPriority.LOWEST, InteractEventManager.InteractEventsInfos.EventInfos.eCancelType.eCancelTypeIgnore, null));
                        interactEventsInfos.addEventInfos(new InteractEventManager.InteractEventsInfos.EventInfos(PlayerInteractEvent.class, EventPriority.HIGHEST, InteractEventManager.InteractEventsInfos.EventInfos.eCancelType.eCancelTypeIgnore,null));
                        interactEventsInfos.addEventInfos(new InteractEventManager.InteractEventsInfos.EventInfos(BlockBreakEvent.class, EventPriority.LOWEST, InteractEventManager.InteractEventsInfos.EventInfos.eCancelType.eCancelTypeCancel, null));
                        interactEventsInfos.addEventInfos(new InteractEventManager.InteractEventsInfos.EventInfos(BlockBreakEvent.class, EventPriority.HIGHEST, InteractEventManager.InteractEventsInfos.EventInfos.eCancelType.eCancelTypeUncancel,null));
                    }
                }
                else if (infosField.m_lstPlantMaterials.contains(itemHandMaterial))
                {   // Try to plant
                    interactEventsInfos = new InteractEventManager.InteractEventsInfos(player, _block, null);
                    interactEventsInfos.addEventInfos(new InteractEventManager.InteractEventsInfos.EventInfos(PlayerInteractEvent.class, EventPriority.LOWEST, InteractEventManager.InteractEventsInfos.EventInfos.eCancelType.eCancelTypeIgnore, null));
                    interactEventsInfos.addEventInfos(new InteractEventManager.InteractEventsInfos.EventInfos(PlayerInteractEvent.class, EventPriority.HIGHEST, InteractEventManager.InteractEventsInfos.EventInfos.eCancelType.eCancelTypeIgnore,null));
                    interactEventsInfos.addEventInfos(new InteractEventManager.InteractEventsInfos.EventInfos(BlockPlaceEvent.class, EventPriority.LOWEST, InteractEventManager.InteractEventsInfos.EventInfos.eCancelType.eCancelTypeCancel, null));
                    interactEventsInfos.addEventInfos(new InteractEventManager.InteractEventsInfos.EventInfos(BlockPlaceEvent.class, EventPriority.HIGHEST, InteractEventManager.InteractEventsInfos.EventInfos.eCancelType.eCancelTypeUncancel,null));
                }
            }
        }

        return interactEventsInfos;
    }
}
