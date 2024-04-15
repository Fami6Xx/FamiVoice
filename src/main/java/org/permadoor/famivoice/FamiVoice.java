package org.permadoor.famivoice;

import com.craftmend.openaudiomc.api.ClientApi;
import com.craftmend.openaudiomc.api.EventApi;
import com.craftmend.openaudiomc.api.clients.Client;
import com.craftmend.openaudiomc.api.events.Handler;
import com.craftmend.openaudiomc.api.events.client.ClientConnectEvent;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public final class FamiVoice extends JavaPlugin implements Listener {
    List<UUID> players = new ArrayList<>();
    BukkitTask voiceTask;
    BukkitTask messageTask;
    FileConfiguration config;

    @Override
    public void onEnable() {
        if(!getDataFolder().exists()){
            getDataFolder().mkdir();
            this.saveDefaultConfig();
            config = this.getConfig();
        }else{
            String[] configYml = Arrays.stream(getDataFolder().list())
                    .filter(s -> s.equals("config.yml"))
                    .toArray(String[]::new);

            if(configYml.length == 0){
                this.saveDefaultConfig();
            }
            config = this.getConfig();
        }

        startVoiceTask();
        startMessageTask();
        getServer().getPluginManager().registerEvents(this, this);

        EventApi.getInstance().registerHandlers(new Object() {
            @Handler
            public void onDisconnect(ClientConnectEvent event) {
                players.add(event.getClient().getActor().getUniqueId());
            }
        });
    }

    @Override
    public void onDisable() {
        voiceTask.cancel();
        messageTask.cancel();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (player.hasPermission("famivoice.bypass")) {
            return;
        }

        UUID playerUUID = event.getPlayer().getUniqueId();
        Client client = ClientApi.getInstance().getClient(playerUUID);

        if (client == null) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    Client finalClient = ClientApi.getInstance().getClient(playerUUID);
                    if (finalClient != null) {
                        players.add(playerUUID);
                    }
                }
            }.runTaskTimer(this, 1L, 1L);
        }else{
            players.add(playerUUID);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        players.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (players.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    public void startVoiceTask() {
        voiceTask = new BukkitRunnable() {
            final List<UUID> toRemove = new ArrayList<>();
            @Override
            public void run() {
                for (UUID player : toRemove){
                    players.remove(player);
                }
                toRemove.clear();

                for (UUID player : players) {
                    Client client = ClientApi.getInstance().getClient(player);
                    if (client != null) {
                        if (client.isConnected()) {
                            if (client.hasVoicechatEnabled()) {
                                toRemove.add(player);
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(this, 1L, 1L);
    }

    public void startMessageTask() {
        messageTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID player : players) {
                    Player p = getServer().getPlayer(player);
                    if (p != null) {
                        p.sendMessage(Objects.requireNonNull(config.getString("connectToVoiceMessage")));
                    }
                }
            }
        }.runTaskTimer(this, 1L, 35L);
    }
}
