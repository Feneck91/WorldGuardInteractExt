package fr.feneck91.worldguardinteractext;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Lectern;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTakeLecternBookEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.*;

/**
 * Class that implements MaterialManager for lectern materials.
 */
public class LecternMaterialManager extends AMaterialManager implements IMaterialManager
{
    /**
     * Material type that this class manage.
     */
    public static final String MATERIAL_TYPE = "__LECTERN__";

    /**
     * Class that manage all informations about lectern.
     */
    private static class InformationsLecternMaterial
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
         * Allowed book to put in lectern.
         */
        public List<MaterialInformation> m_lstPutMaterials;

        /**
         * Allowed book to remove from lectern.
         */
        public List<MaterialInformation> m_lstRemoveMaterials;

        /**
         * Message displayed to the user if he has not permission to remove the book from lectern.
         */
        public String m_strRemoveForbiddenMessage;
    };

    /**
     * List of map informations.
     */
    private Map<String, LecternMaterialManager.InformationsLecternMaterial> m_mapInformationsLecternMaterial;

    /**
     * Constructor.
     *
     * @param _plugin Plugin, used to access logger ot other things.
     */
    public LecternMaterialManager(WorldGuardInteractExt _plugin)
    {
        super(_plugin);
        m_mapInformationsLecternMaterial = new HashMap<String, LecternMaterialManager.InformationsLecternMaterial>();
    }

    /**
     * Get Material type like __LECTERN__, __FIELD__, etc.
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
        return _material != null && _material.equals(Material.LECTERN);
    }

    /**
     * Read a piece of configuration about lectern.
     *
     * @param _mapItems Maps items Config to read.
     * @return true if _mapItems is read without error, false else.
     */
    @Override
    public boolean readConfig(Map<String, Object> _mapItems)
    {
        boolean bRet = true;
        List<Material> listMaterial = null;
        List<MaterialInformation> lstPutMaterials = new ArrayList<MaterialInformation>();
        List<MaterialInformation> lstRemoveMaterials = new ArrayList<MaterialInformation>();
        Set<String> lstRegions;

        listMaterial = findMaterials((ArrayList<String>) _mapItems.get("names"),
            this::isMaterialValidForType,
            (Material itemMaterial) -> { getPlugin().getLogger().info("Configuration " + getMaterialType() + ": add '" + itemMaterial.name() + "'"); },
            (Material itemMaterial) -> { getPlugin().getLogger().warning("Configuration " + getMaterialType() + ": found '" + itemMaterial.name() + "' that is not valid for this type, material ignored"); }
        );
        if (listMaterial.isEmpty())
        {
            getPlugin().getLogger().warning("Configuration " + getMaterialType() + ": found no item!");
            // bRet = false; No, not a critical error, just ignore __LECTERN__ configuration
        }
        else
        {   // Read put / remove material
            if (    !readMaterial(_mapItems, "put", lstPutMaterials, true)
                 || !readMaterial(_mapItems, "remove", lstRemoveMaterials, true))
            {
                bRet = false;
            }
        }
        if (lstPutMaterials.isEmpty() && lstRemoveMaterials.isEmpty())
        {   // Should have at least one of both
            getPlugin().getLogger().warning("Configuration " + getMaterialType() + ": no material found for at least one of both put / remove, ignored!");
            // bRet = false; No, not a critical error, just ignore __LECTERN__ configuration
        }
        lstRegions = findRegions((ArrayList<String>) _mapItems.get("regions"),
                (String strRegionName) -> { getPlugin().getLogger().info("Configuration " + getMaterialType() + ": add region '" + strRegionName + "'"); },
                (String strRegionName) -> { getPlugin().getLogger().warning("Configuration " + getMaterialType() + ": found '" + strRegionName + "' more than once, second is ignored"); }
        );
        String strRemoveForbiddenMessage =  ChatColor.translateAlternateColorCodes('&', (String) _mapItems.get("remove_forbidden_message"));
        if (lstRegions.isEmpty())
        {
            getPlugin().getLogger().warning("Configuration " + getMaterialType() + ": no region found, ignored!");
        }
        else
        {   // All is OK, add it
            LecternMaterialManager.InformationsLecternMaterial infos = new LecternMaterialManager.InformationsLecternMaterial();
            infos.m_lstMaterials                = new HashSet<>(listMaterial);
            infos.m_lstPutMaterials             = lstPutMaterials;
            infos.m_lstRemoveMaterials          = lstRemoveMaterials;
            infos.m_lstRegions                  = lstRegions;
            infos.m_strRemoveForbiddenMessage   = strRemoveForbiddenMessage;
            // To optimize time search, combine world name with material
            for (String strRegionName : lstRegions)
            {
                for (Material material : listMaterial)
                {
                    String strKey = strRegionName + "_._" + material.name();
                    if (m_mapInformationsLecternMaterial.containsKey(strKey))
                    {
                        getPlugin().getLogger().severe("Configuration " + getMaterialType() + " failed to load: more than one material (" + material.name() + ") used for same world / region (" + strRegionName + ")!");
                        bRet = false;
                        break;
                    }
                    m_mapInformationsLecternMaterial.put(strKey, infos);
                }
                if (!bRet)
                {
                    break;
                }
            }
        }

        return bRet;
    }

    /**
     * Read material.
     *
     * @param _mapItems Configuration that contains datas.
     * @param _strKey Key into this configuration to read.
     * @param _lstMaterialsInformation Materials information list to fill.
     * @param _bIsEmptyAllowed If empty is not allowed, return false if the key doesn't exists, else just
     *                         not fill _lstMaterialsInformation.
     * @return true if no error, false else.
     */
    private boolean readMaterial(Map<String, Object> _mapItems, String _strKey, List<MaterialInformation> _lstMaterialsInformation, boolean _bIsEmptyAllowed)
    {
        boolean bRet = true;
        if (_mapItems.containsKey(_strKey))
        {
            List<MaterialInformation> lstMaterialsInformation = findMaterialsX(_strKey, _mapItems,
                (MaterialInformation materialInformation) ->
                    {   // Check if MaterialInformation is valid
                        boolean bRetValidMaterialInfo = false;

                        if (materialInformation != null && materialInformation.getMaterial() != null)
                        {
                            switch (materialInformation.getMaterial())
                            {
                                case Material.WRITABLE_BOOK:
                                {   // For writable book, no extra properties are allowed
                                    bRetValidMaterialInfo = materialInformation.getProperties().isEmpty();
                                    break;
                                }
                                case Material.WRITTEN_BOOK:
                                {   // For written book, only some extra properties are allowed
                                    bRetValidMaterialInfo = materialInformation.getProperties().keySet().stream().allMatch(strPropName -> strPropName.equals("author") || strPropName.equals("title"));
                                    break;
                                }
                            }
                        }
                        return bRetValidMaterialInfo;
                    },
                (MaterialInformation materialInformation) ->
                    {
                        getPlugin().getLogger().info("Configuration " + getMaterialType() + ": add " + _strKey + ": " + materialInformation.toString());
                    },
                (MaterialInformation materialInformation) ->
                    {
                        getPlugin().getLogger().warning("Configuration " + getMaterialType() + ": found '" + materialInformation.toString() + "' that is not valid to " + _strKey + "material ignored. ");
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
     * Display material available for this material type.
     */
    @Override
    public void displayMaterials()
    {
        getPlugin().getLogger().info("Display material for " + getMaterialType());
        getPlugin().getLogger().info(Material.LECTERN.name() + " ==>" + Material.WRITABLE_BOOK.name());
        getPlugin().getLogger().info(Material.LECTERN.name() + " ==>" + Material.WRITTEN_BOOK.name());
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
        ItemStack lecternBookToRemove = null;
        ItemStack itemHand;
        Player player = null;

        if (_event instanceof PlayerTakeLecternBookEvent playerTakeLecternBookEvent)
        {
            itemHand = null;   // Used when player remove item from Lectern
            lecternBookToRemove = playerTakeLecternBookEvent.getBook();
            player = playerTakeLecternBookEvent.getPlayer();
        }
        else  if (_event instanceof PlayerInteractEvent playerInteractEvent)
        {
            itemHand =  playerInteractEvent.getItem();
            player = playerInteractEvent.getPlayer();
        }
        else
        {
            itemHand = null;
        }

        String key = _world.getName() + "." + _strCurrentPlayerRegionName + "_._" + _block.getType().name();
        if (m_mapInformationsLecternMaterial.containsKey(key))
        {
            LecternMaterialManager.InformationsLecternMaterial infosLectern = m_mapInformationsLecternMaterial.get(key);
            // Check if lectern is clicked
            if (_block.getType() == Material.LECTERN)
            {
                ItemStack itemInLecternToTest;
                List<MaterialInformation> listToTest = null;
                boolean bCbeckAllowed = true;
                boolean bInvertCondition = false;
                if (lecternBookToRemove != null)
                {   // When remove book from lectern
                    itemInLecternToTest = lecternBookToRemove;
                    listToTest = infosLectern.m_lstRemoveMaterials;
                    bInvertCondition = true;
                }
                else if (itemHand != null)
                {   // When put book to lectern (itemHand MUST NOT BE null)
                    // Get lectern bloc
                    Lectern lectern = (Lectern) _block.getState();
                    // Get lectern content
                    if (lectern.getInventory().getItem(0) == null)
                    {   // If lectern is empty, use item in hand to test if player is able to place this item
                        itemInLecternToTest = itemHand;
                        listToTest = infosLectern.m_lstPutMaterials;
                    }
                    else // Let user clic on lectern
                    {
                        itemInLecternToTest = null;
                        bCbeckAllowed = false; // Always allowed
                    }
                }
                else
                {
                    itemInLecternToTest = null;
                }
                if (itemInLecternToTest != null || !bCbeckAllowed)
                {   // Lectern is empty, allow to put book (if not nul)) ?
                    // Here, test is inverted:
                    //   - To put, if allowed, we must block event
                    //   - To remove, if allowed do nothing, block even if not allowed
                    boolean bAllowed = (!bCbeckAllowed) || listToTest.stream().anyMatch(item ->
                    {
                        boolean bRet = false;
                        if (itemInLecternToTest.getType().equals(item.getMaterial()))
                        {   // Check if has properties
                            if (item.getProperties().isEmpty())
                            {
                                bRet = true; // Ok this material is allowed to be placed on lectern
                            }
                            else
                            {   // Check properties
                                if (itemInLecternToTest.hasItemMeta())
                                {   // Only if has META
                                    if (itemInLecternToTest.getItemMeta() instanceof BookMeta meta)
                                    {
                                        bRet = item.getProperties().entrySet().stream().allMatch(prop ->
                                        {
                                            return     (prop.getKey().equals("title")  && meta.getTitle()  != null && meta.getTitle().equals(prop.getValue()))
                                                    || (prop.getKey().equals("author") && meta.getAuthor() != null && meta.getAuthor().equals(prop.getValue()));
                                        });
                                    }
                                }
                            }
                        }

                        return bRet;
                    });

                    if ((bAllowed && !bInvertCondition) || (!bAllowed && bInvertCondition))
                    {
                        interactEventsInfos = new InteractEventManager.InteractEventsInfos(player, _block);
                        if (lecternBookToRemove != null)
                        {   // Remove book on lectern
                            interactEventsInfos.addEventInfos(new InteractEventManager.InteractEventsInfos.EventInfos(PlayerTakeLecternBookEvent.class, EventPriority.LOWEST, InteractEventManager.InteractEventsInfos.EventInfos.eCancelType.eCancelTypeCancel,
                                (event) ->
                                {
                                    if (infosLectern.m_strRemoveForbiddenMessage != null && !infosLectern.m_strRemoveForbiddenMessage.isEmpty())
                                    {
                                        ((PlayerTakeLecternBookEvent) event).getPlayer().sendMessage(infosLectern.m_strRemoveForbiddenMessage);
                                    }
                                }));
                        }
                        else
                        {   // Put book on lectern
                            // Don't display message: you can't do that
                            interactEventsInfos.addEventInfos(new InteractEventManager.InteractEventsInfos.EventInfos(PlayerInteractEvent.class, EventPriority.LOWEST, InteractEventManager.InteractEventsInfos.EventInfos.eCancelType.eCancelTypeCancel, null));
                            // Allow to place the book
                            interactEventsInfos.addEventInfos(new InteractEventManager.InteractEventsInfos.EventInfos(PlayerInteractEvent.class, EventPriority.HIGHEST, InteractEventManager.InteractEventsInfos.EventInfos.eCancelType.eCancelTypeUncancel, null));
                        }
                    }
                }
            }
        }

        return interactEventsInfos;
    }
}
