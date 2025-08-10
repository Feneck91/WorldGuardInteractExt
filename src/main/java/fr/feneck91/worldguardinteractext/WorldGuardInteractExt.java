package fr.feneck91.worldguardinteractext;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Base class to manage plugin.
 *
 * Allow to catch event and check if some block interaction forbidden by WorldGuard can be allowed or not by this plugin.
 */
public class WorldGuardInteractExt extends JavaPlugin
{
    /**
     * Is verbose log enabled?
     */
    private boolean                         m_bIsVerboseLogEnabled;

    /**
     * Configuration
     */
    private MaterialConfig                  m_materialConfig;

    /**
     * Manage events
     */
    private final InteractEventManager      m_interactionManager;

    /**
     * Constructor.
     */
    public WorldGuardInteractExt()
    {
        m_bIsVerboseLogEnabled = false;
        // Default config with nothing into it
        m_materialConfig = new MaterialConfig(this);
        m_interactionManager = new InteractEventManager(this, m_materialConfig);
    }

    /**
     * Is verbose log enabled?
     *
     * @return true if enables, false else.
     */
    public boolean isVerboseLogEnabled()
    {
        return m_bIsVerboseLogEnabled;
    }

    /**
     * Get the material coonfig instance.
     * @return The instance of MaterialConfig.
     */
    public MaterialConfig getMaterialConfig()
    {
        return m_materialConfig;
    }

    /**
     * Called when plugin is loaded.
     * <p>
     * Registering has to be done before WorldGuard is enabled. Thus, it is highly recommended that you
     * register when your plugin loads. After WorldGuard is enabled, the FlagRegistry is locked and no
     * new flags can be registered.</p>
     */
    @Override
    public void onLoad()
    {
    }

    /**
     * Called when plugin is activated.
     * <p>
     * Used to read the current configuration.</p>
     */
    @Override
    public void onEnable()
    {
        if (readConfiguration(false, new LoggerDispatcher(this, null)))
        {
            if (isVerboseLogEnabled())
            {
                getLogger().info("WorldGuardInteractExt activated!");
            }
        }
        m_interactionManager.onEnable();
    }

    /**
     * Called when plugin is disabled.
     * <p>
     * Used to read the current configuration.</p>
     */
    @Override
    public void onDisable()
    {
        m_interactionManager.onDisable();
        if (isVerboseLogEnabled())
        {
            getLogger().info("WorldGuardInteractExt disabled!");
        }
    }

    /**
     * Read the plugin configuration.
     *
     * @param _bReloadConfig true to force reload config, false else.
     * @param _logger Wrap class to log to sender if provide from a command, used to write message to info logger.
     * @return true if configuration is OK.
     */
    private boolean readConfiguration(boolean _bReloadConfig, LoggerDispatcher _logger)
    {
        boolean bRet = false;

        // Will save only if the file doesn't exists
        // If readConfiguration() is called because operator make a reload command (wgiextreload), he may
        // have deleted this file to get new one.
        saveDefaultConfig();

        if (_bReloadConfig)
        {
            reloadConfig();
        }
        try
        {
            FileConfiguration config = getConfig();
            // Reading config
            m_bIsVerboseLogEnabled = config.getBoolean("enable_verbose_logs");
            _logger.sendInfoMessage("Reading configuration");

            MaterialConfig materialConfig = new MaterialConfig(this);
            if (materialConfig.ReadConfig(config, _logger))
            {
                m_materialConfig = materialConfig;
                m_interactionManager.setMaterialConfig(m_materialConfig);
                bRet = true;
            }
        }
        catch(Exception _ex)
        {
            _logger.sendErrorMessage("WorldGuardInteractExt::readConfiguration(), exception: " + _ex.getMessage());
            _logger.sendErrorMessage("Previous configuration is keep.");
        }

        return bRet;
    }

    /**
     * Used when user run a command.
     *
     * @param _sender Sender.
     * @param _command Command.
     * @param _strLabel Label.
     * @param _args Argument.
     * @return true if the command is executed, false else.
     */
    @Override
    public boolean onCommand(CommandSender _sender, Command _command, String _strLabel, String[] _args)
    {
        boolean bRet = false;
        LoggerDispatcher logger = new LoggerDispatcher(this, _sender);

        if (_args.length == 0)
        {
            logger.sendColoredMessage(ChatColor.BLUE,"Usage: /wgi <reload|materials>");
            bRet = true;
        }
        else
        {
            String strSubCommand = _args[0].toLowerCase();

            switch (strSubCommand)
            {
                case "reload":
                {
                    if (!_sender.hasPermission("wgiext.command.reload"))
                    {
                        logger.sendErrorMessage("You don't have permission to execute this command!");
                    }
                    else if (_args.length != 1)
                    {
                        logger.sendErrorMessage("No argument needed for this command!");
                        logger.sendColoredMessage(ChatColor.BLUE,"Usage: /wgi reload");
                    }
                    else
                    {
                        // Reload configuration here
                        if (readConfiguration(true, logger))
                        {
                            logger.sendColoredMessage(ChatColor.GREEN, "WorldGuardInteractExt configuration reloaded successfully.");
                            bRet = true;
                        }
                        else
                        {
                            logger.sendErrorMessage("Error while reloading WorldGuardInteractExt configuration!");
                        }
                    }
                    bRet = true;
                    break;
                }
                case "materials":
                {
                    if (!_sender.hasPermission("wgiext.command.materials"))
                    {
                        logger.sendErrorMessage("You don't have permission to execute this command!");
                    }
                    else if (_args.length != 2)
                    {
                        logger.sendErrorMessage("One and only one argument is needed for this command!");
                        logger.sendColoredMessage(ChatColor.BLUE,"Usage: /wgi materials <material>");
                        logger.sendColoredMessage(ChatColor.BLUE,"<material> : " + m_materialConfig.getAllMaterialsTypes());
                    }
                    else
                    {
                        m_materialConfig.displayMaterials(_args[1], logger);
                    }
                    bRet = true;
                    break;
                }
            }
        }

        return bRet;
    }
}