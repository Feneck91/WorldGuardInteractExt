package fr.feneck91.worldguardinteractext;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Class used to manage all event interaction
 */
public class InteractEventManager implements Listener
{
    public static class InteractEventsInfos
    {
        public static class EventInfos
        {
            public enum eCancelType
            {
                eCancelTypeIgnore,
                eCancelTypeCancel,
                eCancelTypeUncancel,
                eCancelTypeRecompute,
            };

            /**
             * Priority event.
             */
            private final EventPriority     m_priorityEvent;

            /**
             * Event cancel type.
             */
            private final eCancelType       m_cancelType;

            /**
             * Event class to manage.
             */
            private final Class<?>          m_eventClass;

            /**
             * Lambda to call if event is managed, could be null.
             */
            private final Consumer<Event>   m_lambdaAction;

            /**
             * Constructor.
             *
             * @param _eventClass Event class.
             * @param _priorityEvent Priority event.
             * @param _cancelType Event cancel type.
             * @param _lambdaAction Lambda to call if event is managed, could be null.
             */
            public EventInfos(Class<?> _eventClass, EventPriority _priorityEvent, eCancelType _cancelType, Consumer<Event> _lambdaAction)
            {
                m_eventClass = _eventClass;
                m_priorityEvent = _priorityEvent;
                m_cancelType = _cancelType;
                m_lambdaAction = _lambdaAction;
            }

            /**
             * Get event priority.
             *
             * @return The priority event.
             */
            public EventPriority GetEventPriority()
            {
                return m_priorityEvent;
            }

            /**
             * Get cancel type.
             *
             * @return The cancel type.
             */
            public eCancelType GetCancelType()
            {
                return m_cancelType;
            }

            /**
             * Get the event class.
             *
             * @return The event class.
             */
            public Class<?> GetEventClass()
            {
                return m_eventClass;
            }

            /**
             * Call event action if nt null.
             *
             * @param _event Event informations.
             */
            public void callLambda(Event _event)
            {
                if (m_lambdaAction != null)
                {
                    m_lambdaAction.accept(_event);
                }
            }
        }

        /**
         * Player.
         */
        private Player                  m_player;

        /**
         * Block.
         */
        private Block                   m_block;

        /**
         * List of events to cancel / uncancel.
         */
        private ArrayList<EventInfos>   m_lstEvents;

        /**
         * Constuctor.
         *
         * @param _player Player.
         * @param _block Block.
         */
        public InteractEventsInfos(Player _player, Block _block)
        {
            m_player = _player;
            m_block = _block;
            m_lstEvents = new ArrayList<EventInfos>();
        }

        /**
         * Assign InteractEventsInfos to this.
         * <p>
         *     This is used when eCancelTypeRecompute is reached.
         * </p>
         * @param _interactEventsInfos Interaction class.
         */
        private void assignToThis(InteractEventsInfos _interactEventsInfos)
        {
            m_player = _interactEventsInfos.m_player;
            m_block = _interactEventsInfos.m_block;
            m_lstEvents = _interactEventsInfos.m_lstEvents;
        }

        /**
         * Add a new event informations.
         *
         * @param _eventInfos Event informations.
         */
        public void addEventInfos(EventInfos _eventInfos)
        {
            m_lstEvents.add(_eventInfos);
        }

        /**
         * Manage all events type.
         *
         * @param _plugin Plugin.
         * @param _event Event.
         * @param _eventPriority Event priority.
         * @param _player Player.
         * @param _block Block.
         * @return true if event is managed, false if all event must be cancelled (because all is ok, or current event is not good).
         */
        private boolean ManageEvent(WorldGuardInteractExt _plugin, Event _event, EventPriority _eventPriority, Player _player, Block _block)
        {
            boolean bRet = false;

            if (!m_lstEvents.isEmpty() && _block != null)
            {
                if (m_block.getLocation().equals(_block.getLocation()))
                {
                    EventInfos eventInfo = m_lstEvents.removeFirst();
                    if (   eventInfo.GetEventClass() == _event.getClass()
                        && eventInfo.GetEventPriority() == _eventPriority)
                    {
                        switch(eventInfo.GetCancelType())
                        {
                            case eCancelTypeIgnore:
                            {
                                if (_plugin.isVerboseLogEnabled())
                                {
                                    if (_event instanceof Cancellable)
                                    {
                                        _plugin.getLogger().info("ManageEvent[Ignore]: " + _event.getClass().getSimpleName() + " / " + _eventPriority + " ignored, event is " + (((Cancellable) _event).isCancelled() ? "cancelled" : "not cancelled"));
                                    }
                                    else
                                    {
                                        _plugin.getLogger().info("ManageEvent[Ignore]: " + _event.getClass().getSimpleName() + " / " + _eventPriority + " ignored, event is not cancellable");
                                    }
                                }
                                bRet = true;
                                break;
                            }
                            case eCancelTypeCancel:
                            {
                                if (_event instanceof Cancellable)
                                {
                                    bRet = true;
                                    if (!((Cancellable) _event).isCancelled())
                                    {
                                        ((Cancellable) _event).setCancelled(true);
                                        if (_plugin.isVerboseLogEnabled())
                                        {
                                            _plugin.getLogger().info("ManageEvent[Cancel]: " + _event.getClass().getSimpleName() + " / " + _eventPriority + " set to cancelled");
                                        }
                                    }
                                    else if (_plugin.isVerboseLogEnabled())
                                    {
                                        _plugin.getLogger().info("ManageEvent[Cancel]: " + _event.getClass().getSimpleName() + " / " + _eventPriority + " already cancelled");
                                    }
                                }
                                else
                                {
                                    _plugin.getLogger().severe("ManageEvent[Cancel]: CancelType = " + eventInfo.GetCancelType() + " is not cancellable!");
                                }
                                break;
                            }
                            case eCancelTypeUncancel:
                            {
                                if (_event instanceof Cancellable)
                                {
                                    bRet = true;
                                    if (((Cancellable) _event).isCancelled())
                                    {
                                        ((Cancellable) _event).setCancelled(false);
                                        if (_plugin.isVerboseLogEnabled())
                                        {
                                            _plugin.getLogger().info("ManageEvent[Uncancelled]: " + _event.getClass().getSimpleName() + " / " + _eventPriority + " set to not cancelled");
                                        }
                                    }
                                    else if (_plugin.isVerboseLogEnabled())
                                    {
                                        _plugin.getLogger().info("ManageEvent[Uncancelled]: " + _event.getClass().getSimpleName() + " / " + _eventPriority + " already not cancelled");
                                    }
                                }
                                else
                                {
                                    _plugin.getLogger().severe("ManageEvent[Uncancelled]: CancelType = " + eventInfo.GetCancelType() + " is not cancellable!");
                                }
                                break;
                            }
                            case eCancelTypeRecompute:
                            {
                                if (!m_lstEvents.isEmpty())
                                {
                                    _plugin.getLogger().severe("ManageEvent[Recompute]: CancelType = " + eventInfo.GetCancelType() + " must be the last event into the list!");
                                }
                                m_lstEvents.clear();
                                InteractEventManager.InteractEventsInfos interactEventsInfos = _plugin.getMaterialConfig().managePlayerInteraction(_event);
                                if (interactEventsInfos != null)
                                {
                                    if (_plugin.isVerboseLogEnabled())
                                    {
                                        _plugin.getLogger().info("ManageEvent[Recompute]: " + _event.getClass().getSimpleName() + " / " + _eventPriority + " recomputed");
                                    }
                                    assignToThis(interactEventsInfos);
                                    bRet = ManageEvent(_plugin, _event, _eventPriority, _player, _block);
                                }
                                else if (_plugin.isVerboseLogEnabled())
                                {
                                    _plugin.getLogger().info("ManageEvent[Recompute]: " + _event.getClass().getSimpleName() + " / " + _eventPriority + " recomputed but is null");
                                }
                                break;
                            }
                            default:
                            {
                                _plugin.getLogger().severe("ManageEvent : CancelType = " + eventInfo.GetCancelType() + " is unknown!");
                                break;
                            }
                        }
                        if (bRet)
                        {
                            // Call lambda if set
                            eventInfo.callLambda(_event);
                            // If m_lstEvents is empty : return false,  it's done!
                            bRet = !m_lstEvents.isEmpty();
                        }
                    }
                    else if (_plugin.isVerboseLogEnabled())
                    {
                        _plugin.getLogger().info("ManageEvent: " + _event.getClass().getSimpleName() + " / " + _eventPriority + " is not the sme as awaitted event (" + eventInfo.GetEventClass().getSimpleName() + " / " + eventInfo.GetEventPriority() + "), surveillance of actions is stopped!");
                    }
                }
                else  if (_plugin.isVerboseLogEnabled())
                {
                    _plugin.getLogger().info("ManageEvent: " + _event.getClass().getSimpleName() + " / " + _eventPriority + " block location are not sames, surveillance of actions is stopped!");
                }
            }
            else if (_plugin.isVerboseLogEnabled())
            {
                if (m_lstEvents.isEmpty())
                {
                    _plugin.getLogger().info("ManageEvent: " + _event.getClass().getSimpleName() + " / " + _eventPriority + " event list is empty, surveillance of actions is stopped!");
                }
                else if (_block != null)
                {
                    _plugin.getLogger().info("ManageEvent: " + _event.getClass().getSimpleName() + " / " + _eventPriority + " target block is null, surveillance of actions is stopped!");
                }
            }

            return bRet;
        }

        /**
         * Manage player interaction event.
         *
         * @param _plugin Plugin.
         * @param _event Event.
         * @param _eventPriority Event priority.
         * @return true if event is managed, false if all event must be cancelled (because all is ok, or current event is not good).
         */
        public boolean ManageEvent(WorldGuardInteractExt _plugin, PlayerInteractEvent _event, EventPriority _eventPriority)
        {
            return ManageEvent(_plugin, _event, _eventPriority, _event.getPlayer(), _event.getClickedBlock());
        }

        /**
         * Manage player block place event.
         *
         * @param _plugin Plugin.
         * @param _event Event.
         * @param _eventPriority Event priority.
         * @return true if event is managed, false if all event must be cancelled (because all is ok, or current event is not good).
         */
        public boolean ManageEvent(WorldGuardInteractExt _plugin, BlockPlaceEvent _event, EventPriority _eventPriority)
        {
            return ManageEvent(_plugin, _event, _eventPriority, _event.getPlayer(), _event.getBlock());
        }

        /**
         * Manage block ignit event.
         *
         * @param _plugin Plugin.
         * @param _event Event.
         * @param _eventPriority Event priority.
         * @return true if event is managed, false if all event must be cancelled (because all is ok, or current event is not good).
         */
        public boolean ManageEvent(WorldGuardInteractExt _plugin, BlockIgniteEvent _event, EventPriority _eventPriority)
        {
            return ManageEvent(_plugin, _event, _eventPriority, _event.getPlayer(), _event.getBlock());
        }

        /**
         * Manage bucket emptying event.
         *
         * @param _plugin Plugin.
         * @param _event Event.
         * @param _eventPriority Event priority.
         * @return true if event is managed, false if all event must be cancelled (because all is ok, or current event is not good).
         */
        public boolean ManageEvent(WorldGuardInteractExt _plugin, PlayerBucketEmptyEvent _event, EventPriority _eventPriority)
        {
            return ManageEvent(_plugin, _event, _eventPriority, _event.getPlayer(), _event.getBlock());
        }
    }

    /**
     * Instance of plugin.
     */
    private final WorldGuardInteractExt             m_plugin;

    /**
     * Material configuration.
     *
     * Loaded by the plugin.
     */
    private MaterialConfig                          m_materialConfig;

    /**
     * Next PlaceEvent block.
     *
     * Used to quickly check if PlaceEvent will use this block, to reactivate the cancel event.
     * Key is the player UUID.
     */
    private final Map<UUID, InteractEventsInfos>    m_mapNextPlaceEventBlock;

    /**
     * Constructor.
     *
     * @param _plugin Plugin, used to access logger ot other things.
     * @param _materialConfig Material configuration.
     */
    public InteractEventManager(WorldGuardInteractExt _plugin, MaterialConfig _materialConfig)
    {
        // Plugin
        m_plugin = _plugin;
        // Initialize event manager
        m_mapNextPlaceEventBlock = new HashMap<UUID, InteractEventsInfos>();
        setMaterialConfig(_materialConfig);
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
     * Called when plugin is activated.
     * <p/>
     * Used to register events.
     */
    public void onEnable()
    {
        getPlugin().getServer().getPluginManager().registerEvents(this, getPlugin());
    }

    /**
     * Called when plugin is disabled.
     * <p/>
     * Used to unregister events.
     */
    public void onDisable()
    {
        HandlerList.unregisterAll();
    }

    /**
     * Clear all events infos for this player.
     *
     * @param _player Infos for this player.
     * @return true if a pending interaction was deleted!
     */
    public boolean clearInteractEventsInfos(Player _player)
    {
        boolean bRet = false;
        if (_player != null)
        {
            bRet = m_mapNextPlaceEventBlock.remove(_player.getUniqueId()) != null;
        }
        return bRet;
    }

    /**
     * Set the material configuration.
     *
     * @param _materialConfig Material configuration.
     */
    public void setMaterialConfig(MaterialConfig _materialConfig)
    {
        m_materialConfig = _materialConfig;
    }

    //=====================================================================================================//
    //                                                                                                     //
    //                                          Events                                                     //
    //                                                                                                     //
    //=====================================================================================================//

    /**
     * Clear all events interaction infos for the player that leave the game.
     *
     * @param _event Event.
     */
    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent _event)
    {
        if (getPlugin().isVerboseLogEnabled())
        {
            getPlugin().getLogger().info("Player " + _event.getPlayer().getName() + " has left the game");
        }
        if (   clearInteractEventsInfos(_event.getPlayer())
            && getPlugin().isVerboseLogEnabled())
        {
            getPlugin().getLogger().info("Pending interaction for player " + _event.getPlayer().getName() + " has been cleared");
        }
    }

    /**
     * When player make event.
     *
     * Check if it must be uncanceled. It is the only code place where all check is done to know if the user will
     * do something that the plugin is able to manage or not.
     *
     * @param _event The event
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPlayerInteractLowest(PlayerInteractEvent _event)
    {
        if (_event != null)
        {
            clearInteractEventsInfos(_event.getPlayer());
            // Only if WorldGuard has canceled the interaction, else do nothing
            Block block = _event.getClickedBlock();
            if (block != null)
            {
                if (_event.getHand() == EquipmentSlot.HAND)
                {   // Remove 2 call with OFF_HAND
                    InteractEventManager.InteractEventsInfos interactEventInfos = m_materialConfig.managePlayerInteraction(_event);
                    if (interactEventInfos != null)
                    {
                        m_mapNextPlaceEventBlock.put(_event.getPlayer().getUniqueId(), interactEventInfos);
                        if (!interactEventInfos.ManageEvent(getPlugin(), _event, EventPriority.LOWEST))
                        {
                            m_mapNextPlaceEventBlock.remove(_event.getPlayer().getUniqueId());
                        }
                    }
                }
            }
        }
    }

    /**
     * When player make event.
     *
     * Check if it must be uncanceled. It is the only code place where all check is done to know if the user will
     * do something that the plugin is able to manage or not.
     *
     * @param _event The event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerInteractHighest(PlayerInteractEvent _event)
    {
        UUID uuidPlayer = _event.getPlayer().getUniqueId();
        if (m_mapNextPlaceEventBlock.containsKey(uuidPlayer))
        {
            if (!m_mapNextPlaceEventBlock.get(uuidPlayer).ManageEvent(getPlugin(), _event, EventPriority.HIGHEST))
            {
                m_mapNextPlaceEventBlock.remove(_event.getPlayer().getUniqueId());
            }
        }
    }

    /**
     * When block is ignite event.
     *
     * Used when block ignite, even the player make event to put fire, it is this event that is called, check if it must be uncanceled.
     *
     * @param _event The event
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onBlockIgniteLowest(BlockIgniteEvent _event)
    {
        if (_event.getPlayer() != null)
        {
            UUID uuidPlayer = _event.getPlayer().getUniqueId();
            if (m_mapNextPlaceEventBlock.containsKey(uuidPlayer))
            {
                if (!m_mapNextPlaceEventBlock.get(uuidPlayer).ManageEvent(getPlugin(), _event, EventPriority.LOWEST))
                {
                    m_mapNextPlaceEventBlock.remove(_event.getPlayer().getUniqueId());
                }
            }
        }
    }

    /**
     * When block is ignite event.
     *
     * Used when block ignite, even the player make event to put fire, it is this event that is called, check if it must be uncanceled.
     *
     * @param _event The event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockIgniteHighest(BlockIgniteEvent _event)
    {
        if (_event.getPlayer() != null)
        {
            UUID uuidPlayer = _event.getPlayer().getUniqueId();
            if (m_mapNextPlaceEventBlock.containsKey(uuidPlayer))
            {
                if (!m_mapNextPlaceEventBlock.get(uuidPlayer).ManageEvent(getPlugin(), _event, EventPriority.HIGHEST))
                {
                    m_mapNextPlaceEventBlock.remove(_event.getPlayer().getUniqueId());
                }
            }
        }
    }

    /**
     * When block change, verify if it should be reactivated.
     *
     * @param _event The event.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onBlockPlaceEventLowest(BlockPlaceEvent _event)
    {
        UUID uuidPlayer = _event.getPlayer().getUniqueId();
        if (m_mapNextPlaceEventBlock.containsKey(uuidPlayer))
        {
            if (!m_mapNextPlaceEventBlock.get(uuidPlayer).ManageEvent(getPlugin(), _event, EventPriority.LOWEST))
            {
                m_mapNextPlaceEventBlock.remove(_event.getPlayer().getUniqueId());
            }
        }
    }

    /**
     * When block change, verify if it should be reactivated.
     *
     * @param _event The event.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockPlaceEventHighest(BlockPlaceEvent _event)
    {
        UUID uuidPlayer = _event.getPlayer().getUniqueId();
        if (m_mapNextPlaceEventBlock.containsKey(uuidPlayer))
        {
            if (!m_mapNextPlaceEventBlock.get(uuidPlayer).ManageEvent(getPlugin(), _event, EventPriority.HIGHEST))
            {
                m_mapNextPlaceEventBlock.remove(_event.getPlayer().getUniqueId());
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPlayerBucketEmptyEventLowest(PlayerBucketEmptyEvent _event)
    {
        UUID uuidPlayer = _event.getPlayer().getUniqueId();
        if (m_mapNextPlaceEventBlock.containsKey(uuidPlayer))
        {
            if (!m_mapNextPlaceEventBlock.get(uuidPlayer).ManageEvent(getPlugin(), _event, EventPriority.LOWEST))
            {
                m_mapNextPlaceEventBlock.remove(_event.getPlayer().getUniqueId());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerBucketEmptyEventHighest(PlayerBucketEmptyEvent _event)
    {
        UUID uuidPlayer = _event.getPlayer().getUniqueId();
        if (m_mapNextPlaceEventBlock.containsKey(uuidPlayer))
        {
            if (!m_mapNextPlaceEventBlock.get(uuidPlayer).ManageEvent(getPlugin(), _event, EventPriority.HIGHEST))
            {
                m_mapNextPlaceEventBlock.remove(_event.getPlayer().getUniqueId());
            }
        }
    }

    /* Future uses
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPlayerBucketFillEventLowest(PlayerBucketFillEvent _event)
    {
        getPlugin().getLogger().info("PlayerBucketFillEvent _event LOWEST");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerBucketFillEventHighest(PlayerBucketFillEvent _event)
    {
        getPlugin().getLogger().info("PlayerBucketFillEvent _event HIGHEST");
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onBlockBreakEventLowest(BlockBreakEvent _event)
    {
        getPlugin().getLogger().info("BlockBreakEvent _event LOWEST");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockBreakEventHighest(BlockBreakEvent _event)
    {
        getPlugin().getLogger().info("BlockBreakEvent _event HIGHEST");
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onBlockDamageEventLowest(BlockDamageEvent _event)
    {
        getPlugin().getLogger().info("BlockDamageEvent _event LOWEST");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockDamageEventHighest(BlockDamageEvent _event)
    {
        getPlugin().getLogger().info("BlockDamageEvent _event HIGHEST");
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onBlockGrowEventLowest(BlockDamageEvent _event)
    {
        getPlugin().getLogger().info("BlockGrowEvent _event LOWEST");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockGrowEventHighest(BlockDamageEvent _event)
    {
        getPlugin().getLogger().info("BlockGrowEvent _event HIGHEST");
    }
    */
}
