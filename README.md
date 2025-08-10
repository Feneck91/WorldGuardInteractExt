# WorldGuardInteractExt
This plugin allow to add more interaction with block using WorldGuard.

Where you are using WorldGuard, if you often use block-break to deny you will not be able to do some things:
  - Extinguish or inflame camp fire.
  - Get / put water / snow from cauldron.
  - Put book on lectern.
  - Plant / get food from / to fields.
  - etc...

If you let block-break to allow, you will able to do some action on these blocks but also break the blocks into the area.
This is why this plugin was created, to add more interaction rules by disable temporary the WorldGuard rules for some interaction only.

# Versions history:
  - __1.0__ : 2025/08/08
    This first version allow to manage __camp fire__ :
      - inflame and extinguish CAMPFIRE and SOUL_CAMPFIRE into an area.<br/>
        It allow to extinguish camp fire with hand (usually not possible) and when using WATER_BUCKET without keep the water spread everywhere.
  - __2.0__ : 2025/08/10
    This version allow to manage __lectern__ : 
      - Choose book that can be put and removed.
      - Possibility to forbid book removal but let it read.
    Command has been changed (wgi reload / wgi materials) and managed to display output messages to player or server console.
    Extended informations has been added for items book and shovel allowing to check items meta. 
    Camp fire allow to add extended informations for shovel as name / lore to better specify wich tools
    can extinguish fire.

# The configuration file (config.yaml):
```yaml
#
# WorldGuardInteractExt configuration file
#
# WARNING: This file is based on WorldGuard region configuration.
#          A single mistyped character can corrupt the file.
#          If WorldGuardInteractExt is unable to parse the file, your configuration
#          will fail to load and the contents of this file will ignored.
#          Please use a YAML validator such as http://yaml-online-parser.appspot.com
#
# REMEMBER TO KEEP PERIODICAL BACKUPS.
#
# AIR is used when user has no item into his hand
# If action is not possible by Minecraft, even you put material, it should be
# not work (if not specially coded)!

# Will display log while block interaction, make easier to debug and configure
# By default verbose log is false
enable_verbose_logs: false
items:
  [
    {
      # Specify type of extended interection: here it is campfire
      type: "__CAMPFIRE__",
      # name : must be only CAMPFIRE or SOUL_CAMPFIRE or both
      names: ["CAMPFIRE", "SOUL_CAMPFIRE"],
      # Region : you MUST add world name before region name to make it work
      # Put  [] to accept all regions
      # regions: ["myworld.region_1", "myworld.region_2"],
      regions: [], # All regions
      # May be inflame can work with other materials
      inflame: ["FLINT_AND_STEEL", "FIRE_CHARGE"],
      # Here, use a regex to specify shovel (for example) you want (here all SHOVEL) or specify only some tools.
      # - You can use AIR to be able to extinguish firecamp with hand (not possible in Minecraft)
      # - You can use WATER_BUCKET: in this case when a firecamp is extinguished with WATER_BUCKET, it's work and
      #     the water is removed just after (in normal way, the water is keep and spread everywhere).
      # - For SHOVEL, you can specify name / lore (only first item is checked) to accept only a special shovel like:
      #     { material : "DIAMOND_SHOVEL", name : "My shovel", lore : "Create by me" }
      #     Note : You can use regex for name into the previous line to accept several shovel materials for same
      #            extra information.
      #     Note : You must add color into text if the name / lore has color.
      #            See colors codes: https://minecraft.wiki/w/Formatting_codes
      extinguish: [".+_SHOVEL"],
    },
    {
      # Specify type of extended interection: here it is lectern
      type: "__LECTERN__",
      # name : must be only LECTERN
      names: ["LECTERN"],
      # Region : you MUST add world name before region name to make it work
      # Put  [] to accept all regions
      # regions: ["myworld.region_1", "myworld.region_2"],
      regions: [], # All regions
      # For WRITTEN_BOOK, you can specify author and / or title to accept only a special book.
      # For WRITABLE_BOOK, only WRITABLE_BOOK can be specify.
      put: [ "WRITABLE_BOOK", { "material" : "WRITTEN_BOOK", "author" : "This is the author", "title" : "The title of the book" } ],
      # This lines are also accepted too!
      # put: [ "WRITABLE_BOOK", "WRITTEN_BOOK" ],
      # put: [ ".+_BOOK" ],
      # Same for remove
      remove: ["WRITABLE_BOOK", "WRITTEN_BOOK"],
      # Message to the user if he try to remove a book from Lectern and this action is forbidden (WorldGuard don't display message)
      # See colors codes: https://minecraft.wiki/w/Formatting_codes
      remove_forbidden_message: "&eYou cannot remove this book!"
    }
  ]
  ```
