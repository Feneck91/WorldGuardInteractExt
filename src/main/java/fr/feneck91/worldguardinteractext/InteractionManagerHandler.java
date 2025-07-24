package fr.feneck91.worldguardinteractext;

import com.sk89q.worldguard.session.Session;
import com.sk89q.worldguard.session.handler.Handler;
/*
import org.bukkit.Location;

import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;


import com.sk89q.worldguard.protection.ApplicableRegionSet;

import com.sk89q.worldguard.session.Handler;
import com.sk89q.worldguard.session.Session;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
 */

/**
 * Handler manager.
 */
public class InteractionManagerHandler extends Handler
{
    /**
     * Factory.
     */
    public static final Factory FACTORY = new Factory();

    /**
     * Factory class.
     */
    public static class Factory extends Handler.Factory<InteractionManagerHandler>
    {
        /**
         * Handler creation.
         *
         * @param _session Session.
         * @return The interaction manager handler instance.
         */
        @Override
        public InteractionManagerHandler create(Session _session)
        {
            return new InteractionManagerHandler(_session);
        }
    }

    /**
     * Constructor.
     * @param _session Session.
     */
    protected InteractionManagerHandler(Session _session)
    {
        super(_session);
    }
}