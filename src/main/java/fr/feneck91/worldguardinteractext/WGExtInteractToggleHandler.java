package fr.feneck91.worldguardinteractext;

import com.sk89q.worldedit.util.Location;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.session.Session;
import com.sk89q.worldguard.session.handler.Handler;
import org.bukkit.Material;
import org.bukkit.block.Block;

public class WGExtInteractToggleHandler extends Handler //extends com.sk89q.worldguard.session.handler.BuildFlagHandler
{
    public static final Handler.Factory<WGExtInteractToggleHandler> FACTORY = new Handler.Factory<WGExtInteractToggleHandler>()
    {
        @Override
        public WGExtInteractToggleHandler create(Session session)
        {
            return new WGExtInteractToggleHandler(session);
        }
    };

    public WGExtInteractToggleHandler(Session session)
    {
        super(session);
    }

    @Override
    protected StateFlag.State getState(LocalPlayer player, Location location, ApplicableRegionSet set)
    {
        Block block = null; //this.block;

        if (block.getType() == Material.CAMPFIRE || block.getType() == Material.SOUL_CAMPFIRE)
        {
            // Si notre flag est ALLOW dans la région, on force l’autorisation
            if (set.testState(player, WGCustomFlags.WG_INTERACT_EXT_FLAG))
            {
                return StateFlag.State.ALLOW;
            }
        }
    }
}