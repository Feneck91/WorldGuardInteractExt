package fr.feneck91.worldguardinteractext;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Lightable;
import org.bukkit.entity.*;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Class that implements MaterialManager for campfire materials.
 */
public class CampFireMaterialManager extends AMaterialManager implements IMaterialManager
{
    /**
     * Material type that this class manage.
     */
    public static final String MATERIAL_TYPE = "__CAMPFIRE__";

    /**
     * Class that manage all informations about campfire.
     */
    private static class InformationsCampFireMaterial
    {
        /**
         * List of allowed regions.
         */
        public Set<String> m_lstRegions;

        /**
         * Material to survey.
         */
        public Set<Material> m_lstMaterials;

        /**
         * Material allowed to inflame.
         */
        public List<MaterialInformation> m_lstInflameMaterials;

        /**
         * Material allowed to extinguish fire.
         */
        public List<MaterialInformation> m_lstExtinguishMaterials;
    };

    /**
     * List of map informations.
     */
    private Map<String, InformationsCampFireMaterial> m_mapInformationsCampFireMaterial;

    /**
     * Constructor.
     *
     * @param _plugin Plugin, used to access logger ot other things.
     */
    public CampFireMaterialManager(WorldGuardInteractExt _plugin)
    {
        super(_plugin);
        m_mapInformationsCampFireMaterial = new HashMap<String, InformationsCampFireMaterial>();
    }

    /**
     * Get Material type like __CAMPFIRE__, __FIELD__, etc.
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
     * @return true if this material is valid, false else.
     */
    @Override
    public boolean isMaterialValidForType(Material _material)
    {
        return _material != null && (_material.equals(Material.CAMPFIRE) || _material.equals(Material.SOUL_CAMPFIRE));
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
        List<Material> listMaterial = null;
        List<MaterialInformation> listInflame = new ArrayList<MaterialInformation>();
        List<MaterialInformation> listExtinguish = new ArrayList<MaterialInformation>();

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
            lstRegions = findRegions(regions,
                    (String strRegionName) -> { _logger.sendInfoMessage("Configuration " + getMaterialType() + ": add region '" + strRegionName + "'"); },
                    (String strRegionName) -> { _logger.sendWarningMessage("Configuration " + getMaterialType() + ": found '" + strRegionName + "' more than once, second is ignored"); }
            );
        }
        listMaterial = findMaterials("names", _mapItems, this::isMaterialValidForType);
        if (listMaterial.isEmpty())
        {
            _logger.sendWarningMessage("Configuration " + getMaterialType() + ": found no item!");
            // bRet = false; No, not a critical error, just ignore __CAMPFIRE__ configuration
        }
        else
        {
            Function<MaterialInformation, Boolean> lambdaCheckIsValid =
                (MaterialInformation materialInformation) ->
                    {   // Check if MaterialInformation is valid
                        boolean bRetValidMaterialInfo = false;

                        if (materialInformation != null && materialInformation.getMaterial() != null)
                        {
                            switch (materialInformation.getMaterial())
                            {
                                case Material.WOODEN_SHOVEL:
                                case Material.STONE_SHOVEL:
                                case Material.GOLDEN_SHOVEL:
                                case Material.DIAMOND_SHOVEL:
                                case Material.NETHERITE_SHOVEL:
                                {   // For shovel, only some extra properties are allowed
                                    bRetValidMaterialInfo = materialInformation.getProperties()
                                        .keySet()
                                        .stream()
                                        .allMatch((strPropName) ->
                                              strPropName.equals("name")
                                           || strPropName.equals("lore"));
                                    break;
                                }
                                default:
                                {   // For others, no extra properties are allowed
                                    bRetValidMaterialInfo = materialInformation.getProperties().isEmpty();
                                    break;
                                }
                            }
                        }
                        return bRetValidMaterialInfo;
                    };

            // Read inflame
            if (   readMaterial("inflame", _mapItems, listInflame, true, lambdaCheckIsValid)
                   // Read extinguish
                && readMaterial("extinguish", _mapItems, listExtinguish, true, lambdaCheckIsValid))
            {
                if (listInflame.isEmpty() && listExtinguish.isEmpty())
                {   // Should have at least one of both
                    _logger.sendWarningMessage("Configuration " + getMaterialType() + ": no material found for at least one of both inflame / extinguish, ignored!");
                    // bRet = false; No, not a critical error, just ignore __CAMPFIRE__ configuration
                }
                else
                {
                    if (lstRegions.isEmpty())
                    {
                        _logger.sendWarningMessage("Configuration " + getMaterialType() + ": no region found, ignored!");
                    }
                    else
                    {   // All is OK, add it
                        InformationsCampFireMaterial infos = new InformationsCampFireMaterial();
                        infos.m_lstMaterials           = new HashSet<>(listMaterial);
                        infos.m_lstInflameMaterials    = listInflame;
                        infos.m_lstExtinguishMaterials = listExtinguish;
                        infos.m_lstRegions             = lstRegions;
                        // To optimize time search in events, combine world name with material
                        for (String strWorldAndRegionName : lstRegions)
                        {
                            for (Material material : listMaterial)
                            {
                                String strKey = MakeKey(strWorldAndRegionName, material);
                                if (m_mapInformationsCampFireMaterial.containsKey(strKey))
                                {
                                    _logger.sendErrorMessage("Configuration " + getMaterialType() + " failed to load: more than one material (" + material.name() + ") used for same world / region (" + strWorldAndRegionName + ")!");
                                    bRet = false;
                                    break;
                                }
                                m_mapInformationsCampFireMaterial.put(strKey, infos);
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
     * Display material available for this material type.
     *
     * @param _logger Wrap class to log to sender if provide from a command, used to write message to info logger.
     */
    @Override
    public void displayMaterials(LoggerDispatcher _logger)
    {
        _logger.sendMessage("Display material for " + getMaterialType());
        _logger.sendMessage("🔥 = Flammable / B = Burnable / 🛢 = Is Fuel");
        for (Material material : Material.values())
        {
            if (material.isFlammable() || material.isBurnable() || material.isFuel())
            {
                StringBuilder strInfos;
                strInfos = new StringBuilder(material.isFlammable() ? "🔥 / " : "☐ / ");
                strInfos.append(material.isBurnable() ? "B / " : "☐ / ");
                strInfos.append(material.isFuel() ? "🛢" : "☐");

                _logger.sendMessage(strInfos.toString() + " ==> " + material.name());
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
        Player player = null;

        String strKey = MakeKey(_world, _strCurrentPlayerRegionName, _block.getType());
        if (m_mapInformationsCampFireMaterial.containsKey(strKey))
        {
            InformationsCampFireMaterial infosFire = m_mapInformationsCampFireMaterial.get(strKey);
            // Here, we are sure, _block.getType() is Flammable
            if (_block.getBlockData() instanceof Lightable lightableBlockData)
            {
                final ItemStack handItem;       // If the user have an item into his hand
                final Material causeMaterial;   // Can be handItem material or other if handItem is null

                if (_event instanceof PlayerInteractEvent playerInteractEvent)
                {
                    handItem = playerInteractEvent.getItem();
                    causeMaterial = (handItem == null)
                        ? Material.AIR
                        : handItem.getType();
                    player = playerInteractEvent.getPlayer();
                }
                else if (_event instanceof BlockIgniteEvent blockIgniteEvent)
                {
                    player = blockIgniteEvent.getPlayer();
                    if (player != null)
                    {   // If the fire ignit with player
                        handItem = player.getInventory().getItemInMainHand();
                        causeMaterial = handItem.getType();
                    }
                    else if (blockIgniteEvent.getIgnitingEntity() != null)
                    {   // If the fire ignit with entity (like mob, arrow, etc)
                        handItem = null;
                        Entity igniter = blockIgniteEvent.getIgnitingEntity();

                        if (igniter instanceof Projectile projectile)
                        {   //  If the fire ignit with a projectile (eg fireball)
                            if (projectile.getType() == EntityType.SMALL_FIREBALL || projectile.getType() == EntityType.FIREBALL)
                            {
                                causeMaterial = Material.FIRE_CHARGE;
                            }
                            else
                            {
                                causeMaterial = Material.ARROW; // Generic example
                            }
                        }
                        else if (igniter.getType() == EntityType.BLAZE)
                        {   // Mobs like blaze
                            causeMaterial = Material.BLAZE_ROD;
                        }
                        else
                        {   // Generic case
                            causeMaterial = Material.AIR; // ignore (not supported)
                        }
                    }
                    else
                    {
                        handItem = null;
                        switch (blockIgniteEvent.getCause())
                        {
                            case LAVA           -> causeMaterial = Material.LAVA;
                            case LIGHTNING      -> causeMaterial = Material.LIGHTNING_ROD;
                            case EXPLOSION      -> causeMaterial = Material.TNT;
                            case ENDER_CRYSTAL  -> causeMaterial = Material.END_CRYSTAL;
                            case SPREAD         -> causeMaterial = Material.FIRE;
                            default             -> causeMaterial = Material.AIR; // ignore (not supported)
                        }
                    }
                }
                else
                {
                    handItem = null;
                    causeMaterial = null;
                }
                // Create lambda function
                Predicate<? super MaterialInformation> lambaIsAllowedMaterial = (MaterialInformation item) ->
                {
                    boolean bRet = false;
                    if (causeMaterial.equals(item.getMaterial()))
                    {   // Check if has properties
                        if (item.getProperties().isEmpty())
                        {
                            bRet = true; // Ok this material is allowed to be used
                        }
                        else if (handItem != null)
                        {   // Check properties
                            bRet = true;
                            if (handItem.hasItemMeta())
                            {   // Only if has META
                                final ItemMeta itemMeta = Objects.requireNonNull(handItem.getItemMeta()); // Cannot be null here
                                bRet = item.getProperties().entrySet().stream().allMatch(prop ->
                                       (prop.getKey().equals("name") && itemMeta.hasDisplayName() && itemMeta.getDisplayName().equals(prop.getValue()))
                                    || (prop.getKey().equals("lore") && itemMeta.hasLore() && !itemMeta.getLore().isEmpty() && itemMeta.getLore().get(0).equals(prop.getValue()))
                                );
                            }
                        }
                    }
                    return bRet;
                };

                if (lightableBlockData.isLit())
                {   // 🔥 Stop fire with shovel or other (in m_lstExtinguishMaterials)
                    if (causeMaterial != null)
                    {
                        if (infosFire.m_lstExtinguishMaterials.stream().anyMatch(lambaIsAllowedMaterial))
                        {
                            interactEventsInfos = new InteractEventManager.InteractEventsInfos(player, _block);
                            interactEventsInfos.addEventInfos(new InteractEventManager.InteractEventsInfos.EventInfos(PlayerInteractEvent.class, EventPriority.LOWEST, InteractEventManager.InteractEventsInfos.EventInfos.eCancelType.eCancelTypeCancel, null));
                            // For bucket
                            if (causeMaterial.name().endsWith("BUCKET"))
                            {
                                interactEventsInfos.addEventInfos(new InteractEventManager.InteractEventsInfos.EventInfos(PlayerInteractEvent.class, EventPriority.HIGHEST, InteractEventManager.InteractEventsInfos.EventInfos.eCancelType.eCancelTypeUncancel, null));
                                interactEventsInfos.addEventInfos(new InteractEventManager.InteractEventsInfos.EventInfos(PlayerBucketEmptyEvent.class, EventPriority.LOWEST, InteractEventManager.InteractEventsInfos.EventInfos.eCancelType.eCancelTypeCancel, null));
                                interactEventsInfos.addEventInfos(new InteractEventManager.InteractEventsInfos.EventInfos(PlayerBucketEmptyEvent.class, EventPriority.HIGHEST, InteractEventManager.InteractEventsInfos.EventInfos.eCancelType.eCancelTypeUncancel,
                                    causeMaterial.equals(Material.WATER_BUCKET)
                                        ? (event) ->
                                            {
                                                if (event instanceof PlayerBucketEmptyEvent playerBucketEmptyEvent)
                                                {   // Let action to do (the fire will be extinguish automatically), then
                                                    // remove water 1 tick after
                                                    Bukkit.getScheduler().runTaskLater(getPlugin(), () ->
                                                    {
                                                        lightableBlockData.setLit(false);
                                                        _block.setBlockData(lightableBlockData);
                                                        if (getPlugin().isVerboseLogEnabled())
                                                        {
                                                            getPlugin().getLogger().info("Remove water after extinguish fire camp with water bucket");
                                                        }
                                                    }, 1L); // 1 tick later: water is placed, we can remove it
                                                }
                                            }
                                        : null));
                            }
                            else
                            {
                                if (causeMaterial.equals(Material.AIR))
                                {   // Trying to stop fire with hand only : no BlockPlaceEvent and lambda for PlayerInteractEvent HIGHEST
                                    interactEventsInfos.addEventInfos(new InteractEventManager.InteractEventsInfos.EventInfos(PlayerInteractEvent.class, EventPriority.HIGHEST, InteractEventManager.InteractEventsInfos.EventInfos.eCancelType.eCancelTypeUncancel,
                                        (event) ->
                                        {   // Replace by not burn item
                                            lightableBlockData.setLit(false);
                                            _block.setBlockData(lightableBlockData);
                                            // And play sound
                                            _block.getWorld().playSound(_block.getLocation(), "block.fire.extinguish", 1.0f, 1.0f);
                                        }));
                                }
                                else
                                {   // Normal way
                                    interactEventsInfos.addEventInfos(new InteractEventManager.InteractEventsInfos.EventInfos(PlayerInteractEvent.class, EventPriority.HIGHEST, InteractEventManager.InteractEventsInfos.EventInfos.eCancelType.eCancelTypeUncancel, null));
                                    interactEventsInfos.addEventInfos(new InteractEventManager.InteractEventsInfos.EventInfos(BlockPlaceEvent.class, EventPriority.LOWEST, InteractEventManager.InteractEventsInfos.EventInfos.eCancelType.eCancelTypeCancel, null));
                                    interactEventsInfos.addEventInfos(new InteractEventManager.InteractEventsInfos.EventInfos(BlockPlaceEvent.class, EventPriority.HIGHEST, InteractEventManager.InteractEventsInfos.EventInfos.eCancelType.eCancelTypeUncancel, null));
                                }
                            }
                        }
                    }
                }
                else
                {   // 🔥 Start fire with fire charge or flint and steel or other
                    if (infosFire.m_lstInflameMaterials.stream().anyMatch(lambaIsAllowedMaterial))
                    {
                        interactEventsInfos = new InteractEventManager.InteractEventsInfos(player, _block);

                        if (_event instanceof BlockIgniteEvent)
                        {   // Compute for BlockIgniteEvent
                            interactEventsInfos.addEventInfos(new InteractEventManager.InteractEventsInfos.EventInfos(BlockIgniteEvent.class, EventPriority.LOWEST, InteractEventManager.InteractEventsInfos.EventInfos.eCancelType.eCancelTypeCancel, null));
                            interactEventsInfos.addEventInfos(new InteractEventManager.InteractEventsInfos.EventInfos(BlockIgniteEvent.class, EventPriority.HIGHEST, InteractEventManager.InteractEventsInfos.EventInfos.eCancelType.eCancelTypeUncancel, null));
                            interactEventsInfos.addEventInfos(new InteractEventManager.InteractEventsInfos.EventInfos(BlockPlaceEvent.class, EventPriority.LOWEST, InteractEventManager.InteractEventsInfos.EventInfos.eCancelType.eCancelTypeCancel, null));
                            interactEventsInfos.addEventInfos(new InteractEventManager.InteractEventsInfos.EventInfos(BlockPlaceEvent.class, EventPriority.HIGHEST, InteractEventManager.InteractEventsInfos.EventInfos.eCancelType.eCancelTypeUncancel, null));
                        }
                        else
                        {   // Waiting BlockIgniteEvent to recompute
                            interactEventsInfos.addEventInfos(new InteractEventManager.InteractEventsInfos.EventInfos(PlayerInteractEvent.class, EventPriority.LOWEST, InteractEventManager.InteractEventsInfos.EventInfos.eCancelType.eCancelTypeIgnore, null));
                            interactEventsInfos.addEventInfos(new InteractEventManager.InteractEventsInfos.EventInfos(PlayerInteractEvent.class, EventPriority.HIGHEST, InteractEventManager.InteractEventsInfos.EventInfos.eCancelType.eCancelTypeIgnore, null));
                            interactEventsInfos.addEventInfos(new InteractEventManager.InteractEventsInfos.EventInfos(BlockIgniteEvent.class, EventPriority.LOWEST, InteractEventManager.InteractEventsInfos.EventInfos.eCancelType.eCancelTypeRecompute, null));
                        }
                    }
                }
            }
            // Event is tested, the test failed
            if (interactEventsInfos == null)
            {
                interactEventsInfos = new InteractEventManager.InteractEventsInfos(player, _block);
            }
        }

        return interactEventsInfos;
    }
}
