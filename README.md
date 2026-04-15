# WorldGuardInteractExt
This plugin allow to add more interaction with block using WorldGuard.

Where you are using WorldGuard, if you often use block-break to deny you will not be able to do some things:
  - Extinguish or inflame camp fire.
  - Get / put water / snow into cauldron.
  - Put book on lectern.
  - Prevent block breaking.
  - Plant / get food from / to fields (not yet implemented).
  - etc...

In WorldGuard, if you let block-break to allow, you will able to do some action on these blocks but also break the blocks into the area.
This is why this plugin was created, to add more interaction rules by disable temporary the WorldGuard rules for some interaction only.

# Commands:
- wgi reload: reload WorldGuardInteractExt configuration 
- wgi materials <material>:  
  - <material> : __CAULDRON__, __CAMPFIRE__, __LECTERN__, __BLOCKBREAK__


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
  - __3.0__ : 2025/08/12
    This version allow to manage __cauldron__ : 
      - Choose cauldron type to manage.
      - Choose items that can fill or empty cauldron.
      - Allow to forbidden interaction with cauldron or let WorldGuard do the job with its with own flags.
      - Allow to display messages to player when interaction is forbidden (fill / empty).
    Bugs fixes: fix logger for command.
    Allow to use place holder to display material / block to the user into message (explanation into config.yaml).
  - __4.0__ : 2025/08/16
    This version allow to manage __breaking blocks__ : 
      - Choose the type of blocks allowed to be broken.
      - Choose tools allowed to break the blocks.
    This allow to break block even the region is protected from breaking blocks with WorldGuard.  
    Bug fix: compute regions from block location instead of player's one.
  - __4.1__ : 2026/04/12
    This version fix some bugs for cauldron:
      - Fix placing / breaking block for cauldron.
        It is not possible to deny block breaking / placing into WorldGuard configuration and let cauldron working fine with this plugin.
        A workaround has been implemented, see the config file that contains explanations. 
      - Prevent cauldron to be break and add an error message if needed.
      - Prevent block to be placed in cauldron area and add an error message if needed.
  - __4.2__ : 2026/04/15
    This version fix some bugs for campfire:
      - Fix extinguish for Java version.

# The configuration file (config.yaml):
```yaml
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
      # Specify type of extended interaction: here it is campfire
      type: "__CAMPFIRE__",
      # name : must be only CAMPFIRE or SOUL_CAMPFIRE or both
      names: ["CAMPFIRE", "SOUL_CAMPFIRE"],
      # Region : you MUST add world name before region name to make it work
      # Put  [] to accept all regions
      # regions: ["myworld.region_1", "myworld.region_2"],
      regions: [], # All regions
      # May be inflame can work with other materials.
      # If empty, search all material. If no material remove this entry.
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
      # If empty, search all material. If no material remove this entry.
      extinguish: [".+_SHOVEL"],
    },
    {
      # Specify type of extended interaction: here it is lectern
      type: "__LECTERN__",
      # name : must be only LECTERN
      names: ["LECTERN"],
      # Region : you MUST add world name before region name to make it work
      # Put  [] to accept all regions
      # regions: ["myworld.region_1", "myworld.region_2"],
      regions: [], # All regions
      # For WRITTEN_BOOK, you can specify author and / or title to accept only a special book.
      # For WRITABLE_BOOK, only WRITABLE_BOOK can be specified.
      # If empty, search all material. If no material remove this entry.
      put: [ "WRITABLE_BOOK", { "material" : "WRITTEN_BOOK", "author" : "This is the author", "title" : "The title of the book" } ],
      # This lines are also accepted too!
      # put: [ "WRITABLE_BOOK", "WRITTEN_BOOK" ],
      # put: [ ".+_BOOK" ],
      # If empty, search all material. If no material remove this entry.
      remove: ["WRITABLE_BOOK", "WRITTEN_BOOK"],
      # Message to the user if he try to remove a book from Lectern and this action is forbidden (WorldGuard don't display message)
      # See colors codes: https://minecraft.wiki/w/Formatting_codes
      # Allow to use <lectern> for the lectern name (automatically translated).
      # Allow to use <removed_book> for the name of the book in the lectern that the player want to remove (automatically translated).
      remove_forbidden_message: "&eYou cannot remove <removed_book> from the <lectern>!"
    },
    {
      # Specify type of extended interaction: here it is cauldron
      #
      # It is not possible for this plugin to override WorldGuard protection for the cauldron, so filling and emptying the cauldron is not possible if
      # the region is protected from block-break and block-place.
      # To make it work, create a region around the cauldron you want manage. It must be exactly one block on x, z and 2 blocs in y and have one AIR block
      # over the cauldron (with non AIR block over the cauldron you cannot fill / empty the cauldron).
      # example: if cauldron is 100, 152, 92 you must create a region with high priority of the protected region like:
      # caudron_1:
      #   min:
      #   {
      #     x: 100,
      #     y: 152,
      #     z: 92
      #   }
      #   max:
      #   {
      #     x: 100,
      #     y: 153,
      #     z: 92
      #   }
      #   flags:
      #   {
      #     block-break: allow,
      #     block-break-group: NON_MEMBERS,
      #     block-place: allow,
      #     block-place-group: NON_MEMBERS,
      #   }
      #   type: cuboid
      #   priority: 15
      type: "__CAULDRON__",
      # name : must be only CAUDRON / LAVA_CAULDRON / WATER_CAULDRON / POWDER_SNOW_CAULDRON
      names: ["CAULDRON", "LAVA_CAULDRON", "WATER_CAULDRON", "POWDER_SNOW_CAULDRON"],
      # Region : you MUST add world name before region name to make it work
      # Put  [] to accept all regions
      # regions: ["myworld.region_1", "myworld.region_2"],
      regions: [], # All regions
      # To fill cauldron, use only LAVA_BUCKET, WATER_BUCKET, POWDER_SNOW_BUCKET, POTION)
      # For POTION only potion with water could be used (and so not empty) to fill cauldron,
      #    the rule is : if cauldron is empty, allow to fill cauldron, else the cauldron must contains water.
      # If empty, search all material. If no material remove this entry (not allowed to fill cauldron).
      fill: [ "LAVA_BUCKET", "WATER_BUCKET", "POWDER_SNOW_BUCKET", "POTION" ],
      # To empty cauldron, use only BUCKET / GLASS_BOTTLE.
      # On cauldron, the GLASS_BOTTLE can only take water, the BUCKET can take lava, water, powder snow
      # Taking water from a cauldron that is not completely filled is also not possible.
      # If empty, search all material. If no material remove this entry (not allowed to empty cauldron).
      empty: ["BUCKET", "GLASS_BOTTLE"],
      # force_forbidden = false: If action is not allowed, the plugin do nothing and let WorldGuard do the job
      #                          with its with own flags,
      # force_forbidden = true: If action is not allowed, the action is cancelled and a forbidden message is displayed.
      force_forbidden: true,
      # Messages to display to the user when fill / empty is not allowed
      # Allow to use <cauldron> for the cauldron name (automatically translated).
      # Allow to use <hand_material> for the name of the item in the player's hand (automatically translated).
      fill_forbidden_message: "&eYou cannot fill this <cauldron> with <hand_material>!",
      empty_forbidden_message: "&eYou cannot empty this <cauldron> with <hand_material>!",
      # protect_cauldron = false: Let the user break the cauldron (default).
      # protect_cauldron = true: Prevent the user break the cauldron, in this case the protect_cauldron_message is displayed if not empty.
      protect_cauldron: true,
      # Allow to use <cauldron> for the cauldron name (automatically translated).
      # Allow to use <hand_material> for the name of the item in the player's hand (automatically translated).
      protect_cauldron_forbidden_message: "&eYou cannot break this <cauldron> with <hand_material>!",
      # Messages to display to the user when cauldront break is not allowed.
      # protect_block_place = false: Let the user place all blocks he wants into the area.
      # protect_block_place = true: Prevent the user place blocks into the area, in this case the protect_block_place_message is displayed if not empty.
      protect_block_place: true,
      # Messages to display to the user block place is not allowed.
      # Allow to use <hand_material> for the name of the item in the player's hand (automatically translated).
      protect_block_place_forbidden_message: "&eYou cannot place this <hand_material>!",
    },
    {
      # Specify type of extended interaction: here it is breakable blocs
      type: "__BLOCKBREAK__",
      # name : All block you want. Plugin is not able to validate material type here. It will work only for
      # breakable blocks. Stone for example (names: ["STONE"]).
      # Empty list is not allowed.
      names: [],
      # Region : you MUST add world name before region name to make it work
      # Put  [] to accept all regions
      # regions: ["myworld.region_1", "myworld.region_2"],
      regions: [], # All regions
      # Use tool(s), allowed by Minecraft to be able to break blocks.
      # You can specify name / lore (only first item is checked) to accept only a special tool like:
      #           tool: [ "STONE_PICKAXE", { material : "DIAMOND_PICKAXE", name : "My pickaxe", lore : "Create by me" } ],
      #     Note : You can use regex for name into the previous line to accept several tools materials for same
      #            extra information.
      #     Note : You must add color into text if the name / lore has color.
      #            See colors codes: https://minecraft.wiki/w/Formatting_codes
      # Empty list is not allowed.
      # Example: [{ material : "DIAMOND_PICKAXE", name : "§6Pickaxe of king", lore : "§dThe best king" }],
      tool: [ ],
    }
  ]
```
