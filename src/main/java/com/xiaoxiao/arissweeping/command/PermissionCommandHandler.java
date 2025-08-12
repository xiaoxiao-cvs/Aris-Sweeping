package com.xiaoxiao.arissweeping.command;

import com.xiaoxiao.arissweeping.ArisSweeping;
import com.xiaoxiao.arissweeping.permission.PermissionManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 权限命令处理器 - 负责处理权限管理相关命令
 */
public class PermissionCommandHandler {
    private final ArisSweeping plugin;
    private final PermissionManager permissionManager;

    public PermissionCommandHandler(ArisSweeping plugin) {
        this.plugin = plugin;
        this.permissionManager = plugin.getPermissionManager();
    }

    /**
     * 处理权限命令
     */
    public void handlePermissionCommand(CommandSender sender, String[] args) {
        // 检查权限
        if (!hasPermission(sender, PermissionManager.ADMIN)) {
            sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 老师~没有权限管理权限哦~");
            return;
        }

        if (args.length == 1) {
            sendPermissionHelp(sender);
            return;
        }

        String subCommand = args[1].toLowerCase();
        
        switch (subCommand) {
            case "give":
            case "add":
                handleGivePermission(sender, args);
                break;
            case "remove":
            case "delete":
                handleRemovePermission(sender, args);
                break;
            case "list":
            case "show":
                handleListPermissions(sender, args);
                break;
            case "reload":
                handleReloadPermissions(sender);
                break;
            default:
                sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 未知的权限命令: " + subCommand);
                sendPermissionHelp(sender);
                break;
        }
    }

    /**
     * 处理给予权限命令
     */
    private void handleGivePermission(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 用法: /arissweeping permission give <玩家名> <权限>");
            sender.sendMessage(ChatColor.YELLOW + "可用权限: admin, cleanup, stats, config");
            return;
        }
        
        String playerName = args[2];
        String permission = "arissweeping." + args[3].toLowerCase();
        
        if (!PermissionManager.VALID_PERMISSIONS.contains(permission)) {
            sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 无效的权限节点: " + args[3]);
            sender.sendMessage(ChatColor.YELLOW + "可用权限: admin, cleanup, stats, config");
            return;
        }
        
        if (permissionManager.givePermission(playerName, permission)) {
            sender.sendMessage(ChatColor.GREEN + "[邦邦卡邦！] 成功给予玩家 " + ChatColor.YELLOW + playerName + 
                             ChatColor.GREEN + " 权限: " + ChatColor.AQUA + args[3]);
            
            // 通知目标玩家
            Player targetPlayer = Bukkit.getPlayer(playerName);
            if (targetPlayer != null) {
                targetPlayer.sendMessage(ChatColor.GREEN + "[邦邦卡邦！] 老师~获得了新权限: " + ChatColor.AQUA + args[3]);
            }
        } else {
            sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 给予权限失败！");
        }
    }
    
    /**
     * 处理移除权限命令
     */
    private void handleRemovePermission(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 用法: /arissweeping permission remove <玩家名> <权限>");
            sender.sendMessage(ChatColor.YELLOW + "可用权限: admin, cleanup, stats, config");
            return;
        }
        
        String playerName = args[2];
        String permission = "arissweeping." + args[3].toLowerCase();
        
        if (!PermissionManager.VALID_PERMISSIONS.contains(permission)) {
            sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 无效的权限节点: " + args[3]);
            sender.sendMessage(ChatColor.YELLOW + "可用权限: admin, cleanup, stats, config");
            return;
        }
        
        if (permissionManager.removePermission(playerName, permission)) {
            sender.sendMessage(ChatColor.GREEN + "[邦邦卡邦！] 成功移除玩家 " + ChatColor.YELLOW + playerName + 
                             ChatColor.GREEN + " 的权限: " + ChatColor.AQUA + args[3]);
            
            // 通知目标玩家
            Player targetPlayer = Bukkit.getPlayer(playerName);
            if (targetPlayer != null) {
                targetPlayer.sendMessage(ChatColor.RED + "[邦邦卡邦！] 老师~失去了权限: " + ChatColor.AQUA + args[3]);
            }
        } else {
            sender.sendMessage(ChatColor.RED + "[邦邦卡邦！] 移除权限失败！该玩家可能没有此权限。");
        }
    }
    
    /**
     * 处理列出权限命令
     */
    private void handleListPermissions(CommandSender sender, String[] args) {
        if (args.length >= 3) {
            // 查看特定玩家的权限
            String playerName = args[2];
            Set<String> permissions = permissionManager.getPlayerPermissions(playerName);
            
            sender.sendMessage(ChatColor.GOLD + "[邦邦卡邦！] " + ChatColor.WHITE + "玩家 " + 
                             ChatColor.YELLOW + playerName + ChatColor.WHITE + " 的权限:");
            
            if (permissions.isEmpty()) {
                sender.sendMessage(ChatColor.GRAY + "  无任何权限");
            } else {
                for (String perm : permissions) {
                    String shortPerm = perm.replace("arissweeping.", "");
                    sender.sendMessage(ChatColor.GREEN + "  - " + ChatColor.AQUA + shortPerm);
                }
            }
        } else {
            // 列出所有有权限的玩家
            Map<String, Set<String>> allPermissions = permissionManager.getAllPlayerPermissions();
            
            sender.sendMessage(ChatColor.GOLD + "[邦邦卡邦！] " + ChatColor.WHITE + "所有权限用户:");
            
            if (allPermissions.isEmpty()) {
                sender.sendMessage(ChatColor.GRAY + "  暂无权限用户");
            } else {
                for (Map.Entry<String, Set<String>> entry : allPermissions.entrySet()) {
                    String playerName = entry.getKey();
                    Set<String> permissions = entry.getValue();
                    
                    sender.sendMessage(ChatColor.YELLOW + "  " + playerName + ChatColor.WHITE + ": " + 
                                     ChatColor.AQUA + permissions.stream()
                                             .map(p -> p.replace("arissweeping.", ""))
                                             .collect(Collectors.joining(", ")));
                }
            }
        }
    }
    
    /**
     * 处理重载权限命令
     */
    private void handleReloadPermissions(CommandSender sender) {
        permissionManager.reloadPermissions();
        sender.sendMessage(ChatColor.GREEN + "[邦邦卡邦！] 权限配置已重新加载！");
    }
    
    /**
     * 发送权限帮助信息
     */
    private void sendPermissionHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "[邦邦卡邦！] " + ChatColor.WHITE + "权限管理帮助:");
        sender.sendMessage(ChatColor.YELLOW + "/arissweeping permission give <玩家名> <权限>" + ChatColor.WHITE + " - 给予权限");
        sender.sendMessage(ChatColor.YELLOW + "/arissweeping permission remove <玩家名> <权限>" + ChatColor.WHITE + " - 移除权限");
        sender.sendMessage(ChatColor.YELLOW + "/arissweeping permission list [玩家名]" + ChatColor.WHITE + " - 查看权限");
        sender.sendMessage(ChatColor.YELLOW + "/arissweeping permission reload" + ChatColor.WHITE + " - 重载权限");
        sender.sendMessage(ChatColor.GRAY + "可用权限: admin, cleanup, stats, config");
    }

    /**
     * 检查权限
     */
    private boolean hasPermission(CommandSender sender, String permission) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            return permissionManager.hasPermission(player.getName(), permission) || player.hasPermission(permission);
        }
        return true; // 控制台总是有权限
    }
}