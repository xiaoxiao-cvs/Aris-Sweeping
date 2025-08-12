package com.xiaoxiao.arissweeping.permission;

import com.xiaoxiao.arissweeping.ArisSweeping;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PermissionManager {
    private final ArisSweeping plugin;
    private final File permissionFile;
    private FileConfiguration permissionConfig;
    private final Map<String, Set<String>> playerPermissions = new HashMap<>();
    
    // 可用的权限节点
    public static final String ADMIN = "arissweeping.admin";
    public static final String CLEANUP = "arissweeping.cleanup";
    public static final String STATS = "arissweeping.stats";
    public static final String CONFIG = "arissweeping.config";
    
    public static final Set<String> VALID_PERMISSIONS = Set.of(ADMIN, CLEANUP, STATS, CONFIG);
    
    public PermissionManager(ArisSweeping plugin) {
        this.plugin = plugin;
        this.permissionFile = new File(plugin.getDataFolder(), "permissions.yml");
        loadPermissions();
    }
    
    private void loadPermissions() {
        try {
            if (!permissionFile.exists()) {
                try {
                    permissionFile.getParentFile().mkdirs();
                    permissionFile.createNewFile();
                } catch (IOException e) {
                    plugin.getLogger().severe("无法创建权限文件: " + e.getMessage());
                    return;
                }
            }
            
            permissionConfig = YamlConfiguration.loadConfiguration(permissionFile);
            playerPermissions.clear();
            
            if (permissionConfig.contains("players")) {
                for (String playerName : permissionConfig.getConfigurationSection("players").getKeys(false)) {
                    List<String> permissions = permissionConfig.getStringList("players." + playerName);
                    playerPermissions.put(playerName.toLowerCase(), new HashSet<>(permissions));
                }
            }
            
            plugin.getLogger().info("已加载 " + playerPermissions.size() + " 个玩家的权限配置");
        } catch (Exception e) {
            plugin.getLogger().severe("加载权限配置时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void savePermissions() {
        permissionConfig.set("players", null);
        
        for (Map.Entry<String, Set<String>> entry : playerPermissions.entrySet()) {
            permissionConfig.set("players." + entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        
        try {
            permissionConfig.save(permissionFile);
        } catch (IOException e) {
            plugin.getLogger().severe("无法保存权限文件: " + e.getMessage());
        }
    }
    
    public boolean hasPermission(String playerName, String permission) {
        try {
            // 参数验证
            if (playerName == null || permission == null) {
                plugin.getLogger().warning("权限检查参数为空: playerName=" + playerName + ", permission=" + permission);
                return false;
            }
            
            // OP始终拥有所有权限
            Player player = Bukkit.getPlayer(playerName);
            if (player != null && player.isOp()) {
                return true;
            }
            
            Set<String> permissions = playerPermissions.get(playerName.toLowerCase());
            if (permissions == null) {
                return false;
            }
            
            // 检查是否有admin权限（拥有所有权限）
            if (permissions.contains(ADMIN)) {
                return true;
            }
            
            return permissions.contains(permission);
        } catch (Exception e) {
            plugin.getLogger().warning("检查权限时发生错误: " + e.getMessage());
            return false;
        }
    }
    
    public boolean givePermission(String playerName, String permission) {
        try {
            // 参数验证
            if (playerName == null || permission == null) {
                plugin.getLogger().warning("给予权限参数为空: playerName=" + playerName + ", permission=" + permission);
                return false;
            }
            
            if (!VALID_PERMISSIONS.contains(permission)) {
                plugin.getLogger().warning("无效的权限节点: " + permission);
                return false;
            }
            
            String lowerPlayerName = playerName.toLowerCase();
            playerPermissions.computeIfAbsent(lowerPlayerName, k -> new HashSet<>()).add(permission);
            savePermissions();
            
            // 通知所有OP和有权限的用户
            notifyPermissionChange("给予", playerName, permission);
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("给予权限时发生错误: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean removePermission(String playerName, String permission) {
        try {
            // 参数验证
            if (playerName == null || permission == null) {
                plugin.getLogger().warning("移除权限参数为空: playerName=" + playerName + ", permission=" + permission);
                return false;
            }
            
            String lowerPlayerName = playerName.toLowerCase();
            Set<String> permissions = playerPermissions.get(lowerPlayerName);
            
            if (permissions == null || !permissions.remove(permission)) {
                return false;
            }
            
            if (permissions.isEmpty()) {
                playerPermissions.remove(lowerPlayerName);
            }
            
            savePermissions();
            
            // 通知所有OP和有权限的用户
            notifyPermissionChange("移除", playerName, permission);
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("移除权限时发生错误: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public Set<String> getPlayerPermissions(String playerName) {
        try {
            if (playerName == null) {
                plugin.getLogger().warning("获取玩家权限时玩家名为空");
                return new HashSet<>();
            }
            
            Set<String> permissions = playerPermissions.get(playerName.toLowerCase());
            return permissions != null ? new HashSet<>(permissions) : new HashSet<>();
        } catch (Exception e) {
            plugin.getLogger().warning("获取玩家权限时发生错误: " + e.getMessage());
            return new HashSet<>();
        }
    }
    
    public Map<String, Set<String>> getAllPlayerPermissions() {
        try {
            return new HashMap<>(playerPermissions);
        } catch (Exception e) {
            plugin.getLogger().warning("获取所有玩家权限时发生错误: " + e.getMessage());
            return new HashMap<>();
        }
    }
    
    private void notifyPermissionChange(String action, String playerName, String permission) {
        try {
            String message = ChatColor.GOLD + "[权限管理] " + ChatColor.WHITE + action + "了老师~ " + 
                            ChatColor.YELLOW + playerName + ChatColor.WHITE + " 的权限: " + 
                            ChatColor.GREEN + permission;
            
            // 通知所有在线的OP
            for (Player player : Bukkit.getOnlinePlayers()) {
                try {
                    if (player.isOp() || hasPermission(player.getName(), ADMIN)) {
                        player.sendMessage(message);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("向玩家 " + player.getName() + " 发送权限变更通知时发生错误: " + e.getMessage());
                }
            }
            
            // 记录到控制台
            plugin.getLogger().info("[权限管理] " + action + "了老师~ " + playerName + " 的权限: " + permission);
        } catch (Exception e) {
            plugin.getLogger().severe("发送权限变更通知时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void notifyConfigChange(String configKey, String oldValue, String newValue) {
        try {
            String message = ChatColor.GOLD + "[配置变更] " + ChatColor.WHITE + "配置项 " + 
                            ChatColor.YELLOW + configKey + ChatColor.WHITE + " 已从 " + 
                            ChatColor.RED + oldValue + ChatColor.WHITE + " 更改为 " + 
                            ChatColor.GREEN + newValue;
            
            // 通知所有在线的OP和有配置权限的用户
            for (Player player : Bukkit.getOnlinePlayers()) {
                try {
                    if (player.isOp() || hasPermission(player.getName(), ADMIN) || hasPermission(player.getName(), CONFIG)) {
                        player.sendMessage(message);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("向玩家 " + player.getName() + " 发送配置变更通知时发生错误: " + e.getMessage());
                }
            }
            
            // 记录到控制台
            plugin.getLogger().info("[配置变更] " + configKey + " 从 " + oldValue + " 更改为 " + newValue);
        } catch (Exception e) {
            plugin.getLogger().severe("发送配置变更通知时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void reloadPermissions() {
        try {
            loadPermissions();
            plugin.getLogger().info("权限配置重载完成");
        } catch (Exception e) {
            plugin.getLogger().severe("重载权限配置时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}