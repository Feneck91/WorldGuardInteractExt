/**
 * This plugin allow to add more interaction with block and items using WorldGuard.
 *
 * Where using WorldGuard to protect you world, by default WorldGuard use block-break / block-place to deny to
 * prevent user to break the world. It is exactly what we need.
 *
 * But, even if you use <b>interact</b> and <b>use</b> flags to <b>allow</b> you will not be able to do some things:
 * <ul>
 *   <li> Extinguish or inflame camp fire. </li>
 *   <li> Get or put water/snow from/to cauldron. </li>
 *   <li> Put book on lectern. </li>
 *   <li> For fields, you cannot plants seed / harvest. </li>
 *   <li> etc... </li>
 * </ul>
 *
 * You  must let <b>block-break</b> to <b>allow</b> to do that, and you will be able to do these actions on these blocks.
 * But also the player will be able to break the blocks into the area or place new one.
 *
 * This is why this plugin was created, to add more interaction rules by disable temporary the WorldGuard rules for some interaction only.
 * The config.yml explain how to use it.
 *
 * For the moment, only these interactions are supported:
 * <ul>
 *   <li> Extinguish or inflame camp fire. </li>
 *   <li> Put / remove book on lectern. </li>
 * </ul>
 *
 * @author Feneck91
 * @version 2.0
 * @since 2025-08-01
 */
package fr.feneck91.worldguardinteractext;