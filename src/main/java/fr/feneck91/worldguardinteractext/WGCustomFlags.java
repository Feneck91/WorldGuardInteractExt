package fr.feneck91.worldguardinteractext;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;

/**
 * Manage WorldGuard custom flag.
 */
public class WGCustomFlags
{
    /**
     * Current flag name.
     */
    private static final String NAME_WG_INTERACT_EXT_FLAG = "wg-interact-ext";

    /**
     * Current flag.
     */
    public static StateFlag WG_INTERACT_EXT_FLAG;

    /**
     * Register the flage
     */
    public static void registerFlags()
    {
        try
        {
            FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
            StateFlag wgInteractExtFlag = new StateFlag(WGCustomFlags.NAME_WG_INTERACT_EXT_FLAG, true);
            registry.register(wgInteractExtFlag);
            WG_INTERACT_EXT_FLAG = wgInteractExtFlag; // only set our field if there was no error
        }
        catch (FlagConflictException e)
        {   // some other plugin registered a flag by the same name already.
            // you can use the existing flag, but this may cause conflicts - be sure to check type
            Flag<?> existing = WorldGuard.getInstance().getFlagRegistry().get(WGCustomFlags.NAME_WG_INTERACT_EXT_FLAG);
            if (existing instanceof StateFlag wgInteractExtFlag)
            {
                WG_INTERACT_EXT_FLAG = wgInteractExtFlag;
                WorldGuardInteractExt.getInstance().getLogger().warning("Some other plugin registered the flag, reuse it: " + WGCustomFlags.NAME_WG_INTERACT_EXT_FLAG);
            }
            else
            {   // types don't match - this is bad news! some other plugin conflicts with you
                // hopefully this never actually happens
                WorldGuardInteractExt.getInstance().getLogger().severe("Flag types don't match. Some other plugin conflicts with this plugin: " + WGCustomFlags.NAME_WG_INTERACT_EXT_FLAG);
            }
        }
    }
}
