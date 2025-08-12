package fr.feneck91.worldguardinteractext;

import com.sk89q.worldedit.util.formatting.text.format.TextColor;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

/**
 * Class used to log into server console or command.
 * <p>
 *     It log into logger if no command is send, else use the command logger.
 * </p>
 */
public class LoggerDispatcher
{
    /**
     * Instance of plugin.
     */
    private final WorldGuardInteractExt m_plugin;

    /**
     * Command sender.
     * <p>
     *     Used to log response for user command, can be null.
     * </p>
     */
    private final CommandSender m_commandSender;

    /**
     * Constructor.
     *
     * @param _plugin Plugin, used to access logger ot other things.
     * @param _commandSender To log in response of user command, can be null.
     */
    public LoggerDispatcher(WorldGuardInteractExt _plugin, CommandSender _commandSender)
    {
        m_plugin = _plugin;
        m_commandSender = _commandSender;
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
     * Is verbose log enabled?
     *
     * @return true if enables, false else.
     */
    public boolean isVerboseLogEnabled()
    {
        return getPlugin().isVerboseLogEnabled();
    }

    /**
     * Send message into correct logger.
     *
     * @param _strMessage Message to log.
     */
    public void sendInfoMessage(String _strMessage)
    {
        if (m_commandSender != null)
        {
            m_commandSender.sendMessage(ChatColor.WHITE + _strMessage);
        }
        else if (isVerboseLogEnabled())
        {
            getPlugin().getLogger().info(_strMessage);
        }
    }

    /**
     * Send warning message into correct logger.
     * <p>
     *     Send the message, even verbose log is disabled!
     * </p>
     *
     * @param _strMessage Message to log.
     */
    public void sendWarningMessage(String _strMessage)
    {
        if (m_commandSender != null)
        {
            m_commandSender.sendMessage(TextColor.GOLD + _strMessage);
        }
        else
        {
            getPlugin().getLogger().warning(_strMessage);
        }
    }

    /**
     * Send warning message into correct logger.
     * <p>
     *     Send the message, even verbose log is disabled!
     * </p>
     *
     * @param _strMessage Message to log.
     */
    public void sendErrorMessage(String _strMessage)
    {
        if (m_commandSender != null)
        {
            m_commandSender.sendMessage(ChatColor.RED + _strMessage);
        }
        else
        {
            getPlugin().getLogger().severe(_strMessage);
        }
    }

    /**
     * Send message into correct logger.
     * <p>
     *     Send the message, even verbose log is disabled!
     * </p>
     *
     * @param _strMessage Message to log.
     */
    public void sendMessage(String _strMessage)
    {
        if (m_commandSender != null)
        {
            m_commandSender.sendMessage(ChatColor.WHITE + _strMessage);
        }
        else
        {
            getPlugin().getLogger().info(_strMessage);
        }
    }

    /**
     * Send colored message into correct logger.
     * <p>
     *     Send the message, even verbose log is disabled!
     *     This log into the info logger if command sender is null.
     * </p>
     *
     * @param _color Message color.
     * @param _strMessage Message to log.
     */
    public void sendColoredMessage(ChatColor _color, String _strMessage)
    {
        if (m_commandSender != null)
        {
            m_commandSender.sendMessage(_color + _strMessage);
        }
        else
        {
            getPlugin().getLogger().info(_color + _strMessage);
        }
    }
}
