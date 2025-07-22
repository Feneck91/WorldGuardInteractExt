package fr.feneck91.worldguardinteractext;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Lightable;
import org.bukkit.entity.*;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.checkerframework.checker.regex.qual.Regex;
import org.w3c.dom.DOMStringList;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Class that implements MaterialManager for fire materials.
 */
public class FireMaterialManager extends AMaterialManager implements IMaterialManager
{
    /**
     * Material type that this class manage.
     */
    public static final String MATERIAL_TYPE = "__FIRE__";

    /**
     * Class that manage all informations about fire.
     */
    private static class InformationsFireMaterial
    {
        /**
         * List of region allowed
         */
        public Set<String> m_lstRegions;

        /**
         * Material to survey.
         */
        public Set<Material> m_lstMaterials;

        /**
         * Material allowed to inflame.
         */
        public Set<Material> m_lstInflameMaterials;

        /**
         * Material allowed to extinguish fire.
         */
        public Set<Material> m_lstExtinguishMaterials;
    };

    /**
     * List of map informations.
     */
    private Map<String, InformationsFireMaterial> m_mapInformationsFireMaterial;

    /**
     * Constructor.
     *
     * @param _plugin Plugin, used to access logger ot other things.
     */
    public FireMaterialManager(WorldGuardInteractExt _plugin)
    {
        super(_plugin);
        m_mapInformationsFireMaterial = new HashMap<String, InformationsFireMaterial>();
    }

    /**
     * Get Material type like __FIRE__, __FIELD__, etc.
     *
     * @return tye material type.
     */
    @Override
    public String getMaterialType()
    {
        return MATERIAL_TYPE;
    }

    /**
     * Ask ig this material is valid for this type.
     *
     * @param _material Material to test.
     * @return true if this material is valid, false else.
     */
    @Override
    public boolean isMaterialValidForType(Material _material)
    {
        return _material != null && (_material.isFlammable() || _material.isBurnable());
    }

    /**
     * Read a piece of configuration about fire.
     *
     * @param _mapItems Maps items Config to read.
     * @return true if _mapItems is read without error, false else.
     */
    @Override
    public boolean readConfig(Map<String, Object> _mapItems)
    {
        boolean bRet = true;
        List<Material> listMaterial = null;
        List<Material> listInflame = new ArrayList<Material>();
        List<Material> listExtinguish = new ArrayList<Material>();
        Set<String> lstRegions;

        listMaterial = findMaterials((ArrayList<String>) _mapItems.get("names"),
                                     this::isMaterialValidForType,
                                     (Material itemMaterial) -> { getPlugin().getLogger().info("Configuration " + getMaterialType() + ": add '" + itemMaterial.name() + "'"); },
                                     (Material itemMaterial) -> { getPlugin().getLogger().warning("Configuration " + getMaterialType() + ": found '" + itemMaterial.name() + "' that is not valid for this type, material ignored"); }
                                    );
        if (listMaterial.isEmpty())
        {
            getPlugin().getLogger().warning("Configuration " + getMaterialType() + ": found no item!");
            // bRet = false; No, not a critical error, just ignore __FIRE__ configuration
        }
        else
        {   // Read inflame
            if (_mapItems.containsKey("inflame"))
            {
                listInflame = findMaterials((ArrayList<String>) _mapItems.get("inflame"),
                                            (Material itemMaterial) -> { return true; },
                                            (Material itemMaterial) -> { getPlugin().getLogger().info("Configuration " + getMaterialType() + ": add inflame '" + itemMaterial.name() + "'"); },
                                            (Material itemMaterial) -> { getPlugin().getLogger().warning("Configuration " + getMaterialType() + ": found '" + itemMaterial.name() + "' that is not valid to inflame, material ignored"); }
                                           );

            }
            if (_mapItems.containsKey("extinguish"))
            {
                listExtinguish = findMaterials((ArrayList<String>) _mapItems.get("extinguish"),
                                               (Material itemMaterial) -> { return true; },
                                               (Material itemMaterial) -> { getPlugin().getLogger().info("Configuration " + getMaterialType() + ": add extinguish '" + itemMaterial.name() + "'"); },
                                               (Material itemMaterial) -> { getPlugin().getLogger().warning("Configuration " + getMaterialType() + ": found '" + itemMaterial.name() + "' that is not valid to extinguish, material ignored"); }
                                              );

            }
        }
        if (listInflame.isEmpty() && listExtinguish.isEmpty())
        {   // Should have at least one of both
            getPlugin().getLogger().warning("Configuration " + getMaterialType() + ": no material found for at least one of both inflame / extinguish, ignored!");
            // bRet = false; No, not a critical error, just ignore __FIRE__ configuration
        }
        lstRegions = findRegions((ArrayList<String>) _mapItems.get("regions"),
                                 (String strRegionName) -> { getPlugin().getLogger().info("Configuration " + getMaterialType() + ": add region '" + strRegionName + "'"); },
                                 (String strRegionName) -> { getPlugin().getLogger().warning("Configuration " + getMaterialType() + ": found '" + strRegionName + "' more than once, second is ignored"); }
                                );
        if (lstRegions.isEmpty())
        {
            getPlugin().getLogger().warning("Configuration " + getMaterialType() + ": no region found, ignored!");
        }
        else
        {   // All is OK, add it
            InformationsFireMaterial infos = new InformationsFireMaterial();
            infos.m_lstMaterials           = new HashSet<>(listMaterial);
            infos.m_lstInflameMaterials    = new HashSet<>(listInflame);
            infos.m_lstExtinguishMaterials = new HashSet<>(listExtinguish);
            infos.m_lstRegions             = lstRegions;
            // To optimize time search, combine world name with material
            for (String strRegionName : lstRegions)
            {
                for (Material material : listMaterial)
                {
                    String strKey = strRegionName + "_._" + material.name();
                    if (m_mapInformationsFireMaterial.containsKey(strKey))
                    {
                        getPlugin().getLogger().severe("Configuration " + getMaterialType() + " failed to load: more than one material (" + material.name() + ") used for same world / region (" + strRegionName + ")!");
                        bRet = false;
                        break;
                    }
                    m_mapInformationsFireMaterial.put(strKey, infos);
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
     * Display material available for this material type.
     */
    @Override
    public void displayMaterials()
    {
        getPlugin().getLogger().info("Display material for " + getMaterialType());
        getPlugin().getLogger().info("🔥 = Flammable / B = Burnable / 🛢 = Is Fuel");
        for (Material material : Material.values())
        {
            if (material.isFlammable() || material.isBurnable() || material.isFuel())
            {
                StringBuilder strInfos;
                strInfos = new StringBuilder(material.isFlammable() ? "🔥 / " : "☐ / ");
                strInfos.append(material.isBurnable() ? "B / " : "☐ / ");
                strInfos.append(material.isFuel() ? "🛢" : "☐");

                getPlugin().getLogger().info(strInfos.toString() + " ==> " + material.name());
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
     * @param _cancelLambdaAction Lambda that this function MUST call to re-enable event cancelled by WorldGuard. The parameters is Block to uncancel next block placement.
     * @return true if something is done, false else.
     */
    @Override
    public boolean managePlayerInteraction(Event _event, Block _block, World _world, String _strCurrentPlayerRegionName, Consumer<Block> _cancelLambdaAction)
    {
        boolean bRet = false;

        String key = _world.getName() + "." + _strCurrentPlayerRegionName + "_._" + _block.getBlockData().getMaterial().name();
        if (m_mapInformationsFireMaterial.containsKey(key))
        {
            InformationsFireMaterial infosFire = m_mapInformationsFireMaterial.get(key);
            // Here, we are sure, _block.getType() is Flammable
            if (_block.getBlockData() instanceof Lightable lightableBlockData)
            {
                Material causeMaterial = null;

                if (_event instanceof PlayerInteractEvent playerInteractEvent)
                {
                    causeMaterial = playerInteractEvent.getItem() == null
                        ? Material.AIR
                        : playerInteractEvent.getItem().getType();
                }
                else if (_event instanceof BlockIgniteEvent blockIgniteEvent)
                {
                    if (blockIgniteEvent.getPlayer() != null)
                    {   // If the fire ignit with player
                        causeMaterial = blockIgniteEvent.getPlayer().getInventory().getItemInMainHand().getType();
                    }
                    else if (blockIgniteEvent.getIgnitingEntity() != null)
                    {   // If the fire ignit with entity (like mob, arrow, etc)
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
                if (lightableBlockData.isLit())
                {   // 🔥 Stop fire with hand or shovel or other (in m_lstExtinguishMaterials)
                    if (infosFire.m_lstExtinguishMaterials.contains(causeMaterial))
                    {
                        bRet = true; // done
                        _cancelLambdaAction.accept(_block);
                    }
                }
                else
                {   // 🔥 Start fire with fire charge or flint and steel or other
                    if (infosFire.m_lstInflameMaterials.contains(causeMaterial))
                    {
                        bRet = true; // done
                        _cancelLambdaAction.accept(_block);
                    }
                }
            }
            // Event is tested, not sur we must continue to search even the test failed
            bRet = true; // done
        }

        return bRet;
    }
}
