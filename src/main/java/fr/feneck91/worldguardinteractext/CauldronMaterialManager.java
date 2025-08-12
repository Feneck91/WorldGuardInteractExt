package fr.feneck91.worldguardinteractext;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Class that implements MaterialManager for cauldron materials.
 */
public class CauldronMaterialManager extends AMaterialManager implements IMaterialManager
{
    /**
     * Material type that this class manage.
     */
    public static final String MATERIAL_TYPE = "__CAULDRON__";

    /**
     * Class that manage all informations about cauldron.
     */
    private static class InformationsCauldronMaterial
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
         * Allowed material to use to fill cauldron.
         */
        public List<MaterialInformation> m_lstFillMaterials;

        /**
         * Allowed material to use to empty cauldron.
         */
        public List<MaterialInformation> m_lstEmptyMaterials;

        /**
         * If action is not allowed, the plugin can do nothing and let WorldGuard do the job,
         * or force action to be cancelled and display message.
         */
        public boolean m_bForceForbidden;

        /**
         * Message displayed to the user if he has no permissions to fill a cauldron.
         */
        public String m_strFillForbiddenMessage;

        /**
         * Message displayed to the user if he has no permissions to empty a cauldron.
         */
        public String m_strEmptyForbiddenMessage;
    };

    /**
     * List of map informations.
     */
    private Map<String, CauldronMaterialManager.InformationsCauldronMaterial> m_mapInformationsCauldronMaterial;

    /**
     * Constructor.
     *
     * @param _plugin Plugin, used to access logger ot other things.
     */
    public CauldronMaterialManager(WorldGuardInteractExt _plugin)
    {
        super(_plugin);
        m_mapInformationsCauldronMaterial = new HashMap<String, CauldronMaterialManager.InformationsCauldronMaterial>();
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
     * @return true if this material is valid, false else.
     */
    @Override
    public boolean isMaterialValidForType(Material _material)
    {
        return    _material != null
               && (    _material.equals(Material.CAULDRON)
                    || _material.equals(Material.LAVA_CAULDRON)
                    || _material.equals(Material.WATER_CAULDRON)
                    || _material.equals(Material.POWDER_SNOW_CAULDRON)
                  );
    }

    /**
     * Read a piece of configuration about cauldrons.
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
        List<MaterialInformation> lstFillMaterials = new ArrayList<MaterialInformation>();
        List<MaterialInformation> lstEmptyMaterials = new ArrayList<MaterialInformation>();

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
        listMaterial = findMaterials(_logger, "names", _mapItems, this::isMaterialValidForType);
        if (listMaterial.isEmpty())
        {
            _logger.sendWarningMessage("Configuration " + getMaterialType() + ": found no item!");
            // bRet = false; No, not a critical error, just ignore __CAULDRON__ configuration
        }
        else
        {   // Read fill / empty material
            Function<MaterialInformation, Boolean> lamdaCheckIsValidFill =
                (MaterialInformation materialInformation) ->
                    {   // Check if MaterialInformation is valid
                        boolean bRetValidMaterialInfo = false;

                        if (materialInformation != null && materialInformation.getMaterial() != null)
                        {
                            switch (materialInformation.getMaterial())
                            {
                                case Material.LAVA_BUCKET:
                                case Material.WATER_BUCKET:
                                case Material.POWDER_SNOW_BUCKET:
                                case Material.POTION:
                                {   // For these materials, no extra properties are allowed
                                    bRetValidMaterialInfo = materialInformation.getProperties().isEmpty();
                                    break;
                                }
                            }
                        }
                        return bRetValidMaterialInfo;
                    };
            Function<MaterialInformation, Boolean> lambdaCheckIsValidEmpty =
                (MaterialInformation materialInformation) ->
                    {   // Check if MaterialInformation is valid
                        boolean bRetValidMaterialInfo = false;

                        if (materialInformation != null && materialInformation.getMaterial() != null)
                        {
                            switch (materialInformation.getMaterial())
                            {
                                case Material.BUCKET:
                                case Material.GLASS_BOTTLE:
                                {   // For these materials, no extra properties are allowed
                                    bRetValidMaterialInfo = materialInformation.getProperties().isEmpty();
                                    break;
                                }
                            }
                        }
                        return bRetValidMaterialInfo;
                    };

            if (    !readMaterial(_logger, "fill", _mapItems, lstFillMaterials, true, lamdaCheckIsValidFill)
                 || !readMaterial(_logger, "empty", _mapItems, lstEmptyMaterials, true, lambdaCheckIsValidEmpty))
            {
                bRet = false;
            }
            if (lstFillMaterials.isEmpty() && lstEmptyMaterials.isEmpty())
            {   // Should have at least one of both
                _logger.sendWarningMessage("Configuration " + getMaterialType() + ": no material found for at least one of both fill / empty, ignored!");
                // bRet = false; No, not a critical error, just ignore __CAULDRON__ configuration
            }
            boolean bForceForbidden = false;
            if (_mapItems.get("force_forbidden") instanceof Boolean booleanValue)
            {
                bForceForbidden = booleanValue;
            }
            _logger.sendInfoMessage("Configuration " + getMaterialType() + ": force forbidden = '" + Boolean.toString(bForceForbidden) + "'");
            String strFillForbiddenMessage = null;
            String strEmptyForbiddenMessage = null;
            if (bForceForbidden)
            {   // Forbidden for fill
                strFillForbiddenMessage = ChatColor.translateAlternateColorCodes('&', (String) _mapItems.get("fill_forbidden_message"));
                _logger.sendInfoMessage("Configuration " + getMaterialType() + ": forbidden message (fill) = '" +( (strFillForbiddenMessage.isEmpty()) ? "<not defined>" : strFillForbiddenMessage) + "'");
                // Forbidden for empty
                strEmptyForbiddenMessage = ChatColor.translateAlternateColorCodes('&', (String) _mapItems.get("empty_forbidden_message"));
                _logger.sendInfoMessage("Configuration " + getMaterialType() + ": forbidden message (empty) = '" +( (strEmptyForbiddenMessage.isEmpty()) ? "<not defined>" : strEmptyForbiddenMessage) + "'");
            }
            if (lstRegions == null || lstRegions.isEmpty())
            {
                _logger.sendWarningMessage("Configuration " + getMaterialType() + ": no region found, ignored!");
            }
            else
            {   // All is OK, add it
                CauldronMaterialManager.InformationsCauldronMaterial infos = new CauldronMaterialManager.InformationsCauldronMaterial();
                infos.m_lstMaterials                = new HashSet<>(listMaterial);
                infos.m_lstFillMaterials            = lstFillMaterials;
                infos.m_lstEmptyMaterials           = lstEmptyMaterials;
                infos.m_lstRegions                  = lstRegions;
                infos.m_bForceForbidden             = bForceForbidden;
                infos.m_strFillForbiddenMessage     = strFillForbiddenMessage;
                infos.m_strEmptyForbiddenMessage    = strEmptyForbiddenMessage;
                // To optimize time search, combine world name with material
                for (String strWorldAndRegionName : lstRegions)
                {
                    for (Material material : listMaterial)
                    {
                        String strKey = MakeKey(strWorldAndRegionName, material);
                        if (m_mapInformationsCauldronMaterial.containsKey(strKey))
                        {
                            _logger.sendErrorMessage("Configuration " + getMaterialType() + " failed to load: more than one material (" + material.name() + ") used for same world / region (" + strWorldAndRegionName + ")!");
                            bRet = false;
                            break;
                        }
                        m_mapInformationsCauldronMaterial.put(strKey, infos);
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
        _logger.sendMessage(padRight(Material.CAULDRON.name(),              20) + " (fill)  ==> " + Material.POTION.name() + " (filled with water only)");
        _logger.sendMessage(padRight(Material.CAULDRON.name(),              20) + " (fill)  ==> " + Material.LAVA_BUCKET.name());
        _logger.sendMessage(padRight(Material.CAULDRON.name(),              20) + " (fill)  ==> " + Material.WATER_BUCKET.name());
        _logger.sendMessage(padRight(Material.CAULDRON.name(),              20) + " (fill)  ==> " + Material.POWDER_SNOW_BUCKET.name());
        _logger.sendMessage(padRight(Material.WATER_CAULDRON.name(),        20) + " (empty) ==> " + Material.GLASS_BOTTLE.name() + " (is always empty)");
        _logger.sendMessage(padRight(Material.WATER_CAULDRON.name(),        20) + " (empty) ==> " + Material.BUCKET.name());
        _logger.sendMessage(padRight(Material.LAVA_CAULDRON.name(),         20) + " (empty) ==> " + Material.BUCKET.name());
        _logger.sendMessage(padRight(Material.POWDER_SNOW_CAULDRON.name(),  20) + " (empty) ==> " + Material.BUCKET.name());
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
        // Check if hand material is water potion
        boolean bIsHandIsWaterPotion = false;
        boolean bIsHandMaterialOk = true;

        if (_event instanceof PlayerInteractEvent playerInteractEvent)
        {
            itemHand =  playerInteractEvent.getItem();
            player = playerInteractEvent.getPlayer();
            if (itemHand != null)
            {
                switch (itemHand.getType())
                {
                    case Material.BUCKET:
                    case Material.LAVA_BUCKET:
                    case Material.WATER_BUCKET:
                    case Material.POWDER_SNOW_BUCKET:
                    case Material.GLASS_BOTTLE:
                    {
                        break;
                    }
                    case Material.POTION:
                    {
                        // If potion check if it is water potion
                        if (itemHand.getItemMeta() instanceof PotionMeta potionMeta)
                        {
                            bIsHandIsWaterPotion = (potionMeta.getBasePotionData().getType() == PotionType.WATER);
                            if (!bIsHandIsWaterPotion)
                            {
                                bIsHandMaterialOk = false; // Other potion are forbidden into Cauldron, ignore
                            }
                        }
                        else
                        {
                            bIsHandMaterialOk = false;
                        }
                        break;
                    }
                    default:
                    {   // The other material are not managed here
                        bIsHandMaterialOk = false;
                        break;
                    }

                }
            }
        }
        else
        {
            itemHand = null;
        }

        if (itemHand != null && bIsHandMaterialOk)
        {   // If nothing in hand, nothing to do
            String strKey = MakeKey(_world, _strCurrentPlayerRegionName, _block.getType());
            if (m_mapInformationsCauldronMaterial.containsKey(strKey))
            {
                CauldronMaterialManager.InformationsCauldronMaterial infosCauldron = m_mapInformationsCauldronMaterial.get(strKey);
                Component translatedForbiddenMessage = null;

                // Check if _block (cauldron) is full or not
                boolean bIsCauldronIsFull = false;
                BlockData blocData = _block.getBlockData();
                if (blocData instanceof Levelled blocDataLevelled)
                {
                    bIsCauldronIsFull = blocDataLevelled.getLevel() == blocDataLevelled.getMaximumLevel();
                }

                // Check if event is allowed
                boolean bIsEventAllowed = false;    // Will cancel / uncancel event to let player action work
                boolean bIsEventForbidden = false;  // Will cancel / ignore event to forbid player action (and display forbidden message)
                // Check if empty cauldron is clicked
                if (_block.getType() == Material.CAULDRON)
                {   // Filling an empty the cauldron
                    if (infosCauldron.m_lstFillMaterials.stream().anyMatch(item -> itemHand.getType().equals(item.getMaterial())))
                    {   // Ok material is accepted
                        // Event is accepted if hand have WATER POTION or other (LAVA_BUCKET, WATER_BUCKET, POWDER_SNOW_BUCKET)
                        // that can fill the cauldron
                        bIsEventAllowed = (bIsHandIsWaterPotion || itemHand.getType() != Material.POTION);
                    }
                    else if (infosCauldron.m_bForceForbidden && itemHand.getType() != Material.GLASS_BOTTLE)
                    {
                        bIsEventForbidden = true;
                        if (infosCauldron.m_strFillForbiddenMessage != null && !infosCauldron.m_strFillForbiddenMessage.isEmpty())
                        {
                            Map<String, Component> placeholders = Map.of(
                            "cauldron", TranslatableComponent.of(_block.getTranslationKey()),
                            "hand_material", TranslatableComponent.of(itemHand.getType().getTranslationKey())
                            );
                            translatedForbiddenMessage = formatPlaceHolders(infosCauldron.m_strFillForbiddenMessage, placeholders);
                        }
                    }
                }
                else if (   (_block.getType() == Material.WATER_CAULDRON && !bIsCauldronIsFull && bIsHandIsWaterPotion)
                         || (   itemHand.getType() == Material.WATER_BUCKET
                             || itemHand.getType() == Material.LAVA_BUCKET
                             || itemHand.getType() == Material.POWDER_SNOW_BUCKET))
                {   // May be Fill if Cauldron is water
                    // Only if the item in hand is WATER POTION and block is water cauldron : try to fill
                    // If cauldron hand is Material.xxxxx_BUCKET it is allowed too
                    // If cauldron is full, don't allowed only if it is a water potion!
                    // If material is not allowed by configuration, don't allowed!
                    bIsEventAllowed = infosCauldron.m_lstFillMaterials.stream().anyMatch(item -> itemHand.getType().equals(item.getMaterial()));
                    if (infosCauldron.m_bForceForbidden && !bIsEventAllowed)
                    {
                        bIsEventForbidden = true;
                        if (infosCauldron.m_strFillForbiddenMessage != null && !infosCauldron.m_strFillForbiddenMessage.isEmpty())
                        {
                            Map<String, Component> placeholders = Map.of(
                            "cauldron", TranslatableComponent.of(_block.getTranslationKey()),
                            "hand_material", TranslatableComponent.of(itemHand.getType().getTranslationKey())
                            );
                            translatedForbiddenMessage = formatPlaceHolders(infosCauldron.m_strFillForbiddenMessage, placeholders);
                        }
                    }
                }
                else
                {   // Emptying the cauldron
                    if (infosCauldron.m_lstEmptyMaterials.stream().anyMatch(item -> itemHand.getType().equals(item.getMaterial())))
                    {   // Emptying the cauldron
                        // Here, we are sure that _block is a cauldron, else we cannot go there or error into configuration reading
                        // because this condition is OK : m_mapInformationsCauldronMaterial.containsKey(key)
                        // so, don't check : _block.getType() == Material.LAVA_CAULDRON / Material.WATER_CAULDRON / Material.POWDER_SNOW_CAULDRON
                        // Empty material is OK, allow event. If user allow emptying Minecraft will let to do, else it will forbid
                        bIsEventAllowed = true;
                    }
                    else if (   infosCauldron.m_bForceForbidden
                             && (   _block.getType() == Material.WATER_CAULDRON
                                 || (   itemHand.getType() != Material.GLASS_BOTTLE
                                     && itemHand.getType() != Material.POTION
                                    )
                                )
                            )
                    {
                        bIsEventForbidden = true;
                        if (infosCauldron.m_strEmptyForbiddenMessage != null && !infosCauldron.m_strEmptyForbiddenMessage.isEmpty())
                        {
                            Map<String, Component> placeholders = Map.of(
                            "cauldron", TranslatableComponent.of(_block.getTranslationKey()),
                            "hand_material", TranslatableComponent.of(itemHand.getType().getTranslationKey())
                            );
                            translatedForbiddenMessage = formatPlaceHolders(infosCauldron.m_strEmptyForbiddenMessage, placeholders);
                        }
                    }
                }
                if (bIsEventAllowed || bIsEventForbidden)
                {   // Allow / Forbidden event
                    Consumer<Event> _lambdaActionEventForbidden = null; // Event to call if event is forbidden at the last event
                    if (translatedForbiddenMessage != null)
                    {
                        BukkitPlayer bukkitPlayer = BukkitAdapter.adapt(player);
                        Component compMessage = translatedForbiddenMessage;
                        _lambdaActionEventForbidden = (_lambdaEvent) ->
                        {
                            if (_lambdaEvent instanceof Cancellable event)
                            {
                                if (event.isCancelled())
                                {   // Only if another plugin has not uncancelled the event
                                    bukkitPlayer.print(compMessage);
                                }
                            }
                        };
                    }
                    interactEventsInfos = new InteractEventManager.InteractEventsInfos(player, _block);
                    interactEventsInfos.addEventInfos(new InteractEventManager.InteractEventsInfos.EventInfos(PlayerInteractEvent.class, EventPriority.LOWEST, InteractEventManager.InteractEventsInfos.EventInfos.eCancelType.eCancelTypeCancel, null));
                    interactEventsInfos.addEventInfos(new InteractEventManager.InteractEventsInfos.EventInfos(PlayerInteractEvent.class, EventPriority.HIGHEST, bIsEventForbidden ? InteractEventManager.InteractEventsInfos.EventInfos.eCancelType.eCancelTypeIgnore : InteractEventManager.InteractEventsInfos.EventInfos.eCancelType.eCancelTypeUncancel, _lambdaActionEventForbidden));
                }
            }
        }

        return interactEventsInfos;
    }
}
