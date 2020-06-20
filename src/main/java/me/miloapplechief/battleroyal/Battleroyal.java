package me.miloapplechief.battleroyal;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class Battleroyal extends JavaPlugin implements Listener {

    private static class Battler {
        Player player;
        int sniperAmmo;
        int handGunAmmo;
        int akAmmo;
        int reload;

        public Battler(Player player) {
            this.player = player;
        }
    }

    private static final Location WAITING_LOCATION = new Location(null,16.5, 8, -33.5, 0, 0);
    private static final List<Location> SPAWNS = Arrays.asList(
            new Location(null,2,6,-77,0,0 ),
            new Location(null,-19.5, 5.5, 21.5, -90, 0 ),
            new Location(null,62.5, 5.5, 21.5, 90, 0 ),
            new Location(null,61.5, 11, -54.5, 90, 0 )
    );


    private enum State {
        NOT_ACTIVE,
        WAITING,
        COUNTING_DOWN,
        PLAYING,
        SCORE,
    }
    private int timer;
    private State state;
    private final List<Battler> battlers = new ArrayList<>();
    private boolean teleporting = false;

    @Override
    public void onEnable() {
        // Plugin startup logic
        getServer().getPluginManager().registerEvents(this,this);
        state = State.NOT_ACTIVE;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {



        if (args.length == 1 && args[0].equals("reset")) {


            for (Battler other : battlers) {
                if (other.player != sender) {
                    other.player.sendMessage("" + ChatColor.RED + ChatColor.BOLD + "The battle has been canceled");
                }
            }

            sender.sendMessage("" + ChatColor.RED + ChatColor.BOLD + "The battle has been canceled");
            state = State.NOT_ACTIVE;
            battlers.clear();

            return true;
        }

        if (!(sender instanceof Player)) {
            return false;
        }

        Player player = (Player) sender;

        if (findBattler(player) != null) {
            player.sendMessage("You are already in a battle!");
            return false;
        }

        switch (state) {
            case NOT_ACTIVE:
                state = State.WAITING;
                addPlayer(player);
                announceGame(player);
                getGameWorld().setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
                break;
            case WAITING:
                state = State.COUNTING_DOWN;
                addPlayer(player);
                timer = 5; //30;
                scheduleCountdown();
                break;
            case COUNTING_DOWN:
                addPlayer(player);
                break;
            case PLAYING:
            case SCORE:
                player.setGameMode(GameMode.SPECTATOR);
                tpWaiting(player);
                player.sendMessage("This game already started but you can still spectate.");
                break;
        }
        return true;
    }

    private void addPlayer(Player player) {
        if (battlers.size() + 1 > SPAWNS.size()) {
            player.setGameMode(GameMode.SPECTATOR);
            player.sendMessage("This game is full but you can still spectate.");
        } else {
            Battler battler = new Battler(player);
            battlers.add(battler);
            clearPlayer(battler);
            player.setGameMode(GameMode.ADVENTURE);
        }
        tpWaiting(player);
    }

    private void tpWaiting(Player player) {
        World world = getGameWorld();
        Location newWorld = WAITING_LOCATION.clone();
        newWorld.setWorld(world);

        teleporting = true;
        try {
            player.teleport(newWorld);
        } finally {
            teleporting = false;
        }
    }

    private void clearPlayer(Battler battler) {
        Player player = battler.player;
        player.getInventory().clear();
        player.setHealth(20);
        player.setFoodLevel(20);
        player.setExp(0);
        player.setTotalExperience(0);
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
    }


    private void scheduleCountdown() {
        getServer().getScheduler().scheduleSyncDelayedTask(this, ()-> {
            if (state != State.COUNTING_DOWN) {
                return;
            }
            if (timer == 1) {
                state = State.PLAYING;

                List<Location> locations = new ArrayList<>(SPAWNS);
                Collections.shuffle(locations);
                int i = 0;
                World world = getGameWorld();

                for (Battler other : battlers) {
                    other.player.sendTitle("" + ChatColor.GREEN + ChatColor.BOLD + "START","",5,20,5);
                    Location location = locations.get(i).clone();
                    location.setWorld(world);
                    teleporting = true;
                    try {
                        other.player.teleport(location);
                    } finally {
                        teleporting = false;
                    }
                    clearPlayer(other);
                    other.player.setGameMode(GameMode.ADVENTURE);

                    ItemStack sniper = new ItemStack(Material.IRON_HOE);
                    ItemStack handGun = new ItemStack(Material.IRON_HORSE_ARMOR);
                    ItemStack ak = new ItemStack(Material.IRON_AXE);

                    ItemMeta item_meta = sniper.getItemMeta();
                    item_meta.setDisplayName("" + ChatColor.WHITE + "Sniper");
                    ArrayList<String> item_lore = new ArrayList<>();
                    item_lore.add(ChatColor.GOLD + "Pow!");
                    item_meta.setLore(item_lore);
                    sniper.setItemMeta(item_meta);

                    item_meta = handGun.getItemMeta();
                    item_meta.setDisplayName("" + ChatColor.WHITE + "Hand Gun");
                    item_lore = new ArrayList<>();
                    item_lore.add(ChatColor.GOLD + "Pow!");
                    item_meta.setLore(item_lore);
                    handGun.setItemMeta(item_meta);

                    item_meta = ak.getItemMeta();
                    item_meta.setDisplayName("" + ChatColor.WHITE + "AK-47");
                    item_lore = new ArrayList<>();
                    item_lore.add(ChatColor.GOLD + "Pow!");
                    item_meta.setLore(item_lore);
                    ak.setItemMeta(item_meta);

                    other.player.getInventory().addItem(sniper);
                    other.player.getInventory().addItem(handGun);
                    //other.player.getInventory().addItem(ak);
                    i += 1;
                }
            } else {
                timer -= 1;
                scheduleCountdown();
                for (Battler other : battlers) {
                    other.player.sendTitle("" + ChatColor.WHITE + timer, "", 0, 10, 10);
                }
            }
        }, 20);
    }

    private World getGameWorld() {
        return getServer().getWorld("battle3");
    }

    private void announceGame(Player player) {

        TextComponent message = new TextComponent("");
        TextComponent m1 = new TextComponent("A battle is starting soon! Click ");
        m1.setColor(net.md_5.bungee.api.ChatColor.WHITE);
        message.addExtra(m1);
        m1 = new TextComponent("join");
        m1.setColor(net.md_5.bungee.api.ChatColor.GREEN);
        m1.setBold(true);
        m1.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,  "/battle"));
        m1.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent[] {
                new TextComponent("Click to run /battle"),
        }));
        message.addExtra(m1);
        m1 = new TextComponent(" to join in!");
        m1.setColor(net.md_5.bungee.api.ChatColor.WHITE);
        message.addExtra(m1);

        for (Player other : player.getServer().getOnlinePlayers()) {
            if (other != player) {
                other.spigot().sendMessage(message);
            }
        }
    }

    @EventHandler
    public void onSniperClick(PlayerInteractEvent e) {
        if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Player player = e.getPlayer();
            Battler battler = findBattler(player);

            World world = getGameWorld();


            if (player.getWorld() == world &&
                    battler != null &&
                    e.getMaterial() == Material.IRON_HOE &&
                    state == State.PLAYING) {
                e.setCancelled(true);

                world.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0F, 1.0F);

                Vector rayDirection = player.getEyeLocation().getDirection().normalize();
                Location rayStart = player.getEyeLocation().add(rayDirection);
                RayTraceResult result = player.getWorld().rayTrace(rayStart, rayDirection, 400,
                        FluidCollisionMode.NEVER, true, 0, null);

                double particleDistance;
                if (result == null) {
                    particleDistance = 200;
                } else {
                    Location hitLocation = result.getHitPosition().toLocation(world);
                    world.playSound(hitLocation, Sound.BLOCK_STONE_BREAK, 1.0F, 1.0F);
                    particleDistance = result.getHitPosition().distance(player.getEyeLocation().toVector());

                    if (result.getHitEntity() instanceof Player) {
                        Player target = (Player) result.getHitEntity();
                        target.damage(3);
                    }
                }

                for (int i = 0; i < particleDistance; i++) {
                    player.getWorld().spawnParticle(Particle.END_ROD,
                            rayStart.clone().add(rayDirection.clone().multiply(i)),
                            1, 0, 0, 0, 0);
                }
            }
        }
    }

    @EventHandler
    private void onPlayerDeath(PlayerDeathEvent e) {
        killPlayer(e.getEntity());
    }

    private Battler findBattler(Player player) {
        for (Battler other : battlers) {
            if (other.player == player) {
                return other;
            }
        }
        return null;
    }

    private void killPlayer(Player loser) {
        Battler battlerLoser = findBattler(loser);
        if (battlerLoser != null) {
            battlers.remove(battlerLoser);
            loser.setGameMode(GameMode.SPECTATOR);
            loser.sendMessage(ChatColor.RED + "You died!");
            for (Player other : loser.getWorld().getPlayers()) {
                if (other != loser) {
                    other.sendMessage("" + loser.getDisplayName() + " died!");
                }
            }
            if (battlers.size() == 1) {

                Player winner = battlers.get(0).player;
                for (Player other : loser.getWorld().getPlayers()) {
                    if (other != winner) {
                        other.sendTitle("" + ChatColor.GREEN + winner.getDisplayName() + " wins!!!", "", 1,59,20);
                    } else {
                        other.sendTitle("" + ChatColor.GREEN + "You win!!!", "", 1,59,20);
                    }
                }
                battlers.clear();
                state = State.NOT_ACTIVE;
            }
        }
    }

    @EventHandler
    private void onPlayerTeleport(PlayerTeleportEvent e) {
        Player player = e.getPlayer();

        Battler battlerTP = findBattler(player);
        if (battlerTP != null && !teleporting) {
            player.sendMessage("You can't teleport in the middle of a match!");
            e.setCancelled(true);
        }
    }

    @EventHandler
    private void onPlayerLeave(PlayerQuitEvent e) {
        killPlayer(e.getPlayer());
    }
}
