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
     *
     * Registering has to be done before WorldGuard is enabled. Thus, it is highly recommended that you
     * register when your plugin loads. After WorldGuard is enabled, the FlagRegistry is locked and no
     * new flags can be registered.
     */
    @Override
    public void onLoad()
    {
    }

    /**
     * Called when plugin is activated.
     * <p>
     * Used to read the current configuration.
     */
    @Override
    public void onEnable()
    {
        if (readConfiguration())
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
     * Used to read the current configuration.
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
     * @return true if configuration is OK.
     */
    private boolean readConfiguration()
    {
        boolean bRet = false;

        // Will save only if the file doesn't exists
        // If readConfiguration() is called because operator make a reload command (wgiextreload), he may
        // have deleted this file to get new one.
        saveDefaultConfig();

        try
        {
            FileConfiguration config = getConfig();
            // Reading config
            m_bIsVerboseLogEnabled = config.getBoolean("enable_verbose_logs");
            if (isVerboseLogEnabled())
            {
                getLogger().info("Reading configuration");
            }
            MaterialConfig materialConfig = new MaterialConfig(this);
            if (materialConfig.RaadConfig(config))
            {
                m_materialConfig = materialConfig;
                m_interactionManager.setMaterialConfig(m_materialConfig);
                bRet = true;
            }
        }
        catch(Exception _ex)
        {
            getLogger().severe("WorldGuardInteractExt::readConfiguration(), exception: " + _ex.getMessage());
            getLogger().severe("Previous configuration is keep.");
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

        if (_command.getName().equalsIgnoreCase("wgiextmaterials"))
        {
            if (!_sender.hasPermission("wgiext.materials"))
            {
                _sender.sendMessage(ChatColor.RED + "You don't have permission to execute this command!");
            }
            else if (_args.length != 1)
            {
                _sender.sendMessage(ChatColor.RED + "One and only one argument is needed for this command!");
            }
            else
            {
                m_materialConfig.displayMaterials(_args[0]);
            }
        }
        else if (_command.getName().equalsIgnoreCase("wgiextreload"))
        {
            if (!_sender.hasPermission("wgiext.reload"))
            {
                _sender.sendMessage(ChatColor.RED + "You don't have permission to execute this command!");
            }
            else if (_args.length != 0)
            {
                _sender.sendMessage(ChatColor.RED + "No argument needed for this command!");
            }
            else
            {
                // Reload configuration here
                if (readConfiguration())
                {
                    _sender.sendMessage(ChatColor.GREEN + "WorldGuardInteractExt configuration reloaded successfully.");
                    bRet = true;
                }
                else
                {
                    _sender.sendMessage(ChatColor.RED + "Error while reloading WorldGuardInteractExt configuration!");
                }
            }
        }
        return bRet;
    }
}