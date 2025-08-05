package fr.feneck91.worldguardinteractext;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 *  Interface used to read config display materials or manage action for material familly.
 *
 *  The material family can be fire, fields, etc.
 */
interface IMaterialManager
{
    /**
     * Get Material type like __FIRE__, __FIELD__, etc.
     *
     * @return The material type string.
     */
    String getMaterialType();

    /**
     * Ask ig this material is valid for this type.
     *
     * @param _material Material to test.
     * @return true if this material is valid, false else.
     */
    boolean isMaterialValidForType(Material _material);

    /**
     * Read a piece of configuration.
     *
     * @param _mapItems Maps items Config to read.
     * @return true if _mapItems is read without error, false else.
     */
    boolean readConfig(Map<String, Object> _mapItems);

    /**
     * Display material available for this material type.
     */
    void displayMaterials();

    /**
     * Manage player interaction
     *
     * @param _event Generic event.
     * @param _block Block that the user clic.
     * @param _world Current player world.
     * @param _strCurrentPlayerRegionName Current region name where player is located actually.
     * @return InteractEventsInfos if something is done, null else.
     */
    InteractEventManager.InteractEventsInfos managePlayerInteraction(Event _event, Block _block, World _world, String _strCurrentPlayerRegionName);
}