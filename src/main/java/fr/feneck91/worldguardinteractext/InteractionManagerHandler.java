package fr.feneck91.worldguardinteractext;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.session.Session;
import com.sk89q.worldguard.session.handler.Handler;
import org.bukkit.block.Block;

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

    MaterialConfig m_materialConfig;
    public void setPendingDecision(MaterialConfig _materialConfig)
    {
        m_materialConfig = _materialConfig;
    }
}