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


# The configuration file (config.yaml):
```yaml
#
# WorldGuardInteractExt configuration file
#
# WARNING: This file is based on WorldGuard region configuration.
#          A single mistyped character can corrupt the file.
#          If WorldGuardInteractExt is unable to parse the file, your configuration
#          will fail to load and the contents of this file will ignored.
#          Please use a YAML validator such as http://yaml-online-parser.appspot.com (for smaller files).
#
# REMEMBER TO KEEP PERIODICAL BACKUPS.
#
# AIR is used when user has no item into his hand
# If action is not possible by Minecraft, even you put material, it should be not work (if not specially coded)!

# Will display log while block interaction, make more easy to debug and configure
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
      inflame: ["FLINT_AND_STEEL", "FIRE_CHARGE"],
      # Here, use a regex to specify shovel you want (here all SHOVEL) or specify only some tools.
      # You can use AIR to be able to extinguish firecamp with hand (not possible in Minecraft)
      # You can use WATER_BUCKET: in this case when a firecamp is extinguish with WATER_BUCKET, it's work and
      # the water is removed just after (in normal way, the water is keep and spread everywhere).
      extinguish: [".+_SHOVEL"],
    }
  ]
```

# Versions history:
  - 1.0<br/>
    This first version allow to manage camp fire : inflame and extinguish CAMPFIRE and SOUL_CAMPFIRE into an area.<br/>
    It allow to extinguish camp fire with hand (usually not possible) and when using WATER_BUCKET without keep the water spread everywhere.
