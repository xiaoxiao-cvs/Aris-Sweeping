package com.xiaoxiao.arissweeping.command;

import com.xiaoxiao.arissweeping.ArisSweeping;
import com.xiaoxiao.arissweeping.permission.PermissionManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tab补全器 - 负责处理命令的自动补全
 */
public class CleanupTabCompleter implements TabCompleter {
    private final ArisSweeping plugin;
    private final PermissionManager permissionManager;

    public CleanupTabCompleter(ArisSweeping plugin) {
        this.plugin = plugin;
        this.permissionManager = plugin.getPermissionManager();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // 主命令补全
            List<String> mainCommands = Arrays.asList(
                "cleanup", "stats", "tps", "config", "toggle", "permission", "test", "livestock-stats", "help"
            );
            
            // 根据权限过滤命令
            for (String cmd : mainCommands) {
                if (hasCommandPermission(sender, cmd) && cmd.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(cmd);
                }
            }
        } else if (args.length == 2) {
            // 二级命令补全
            String mainCommand = args[0].toLowerCase();
            
            switch (mainCommand) {
                case "cleanup":
                    if (permissionManager.hasPermission(sender, PermissionManager.CLEANUP)) {
                        List<String> cleanupCommands = Arrays.asList("items", "mobs", "all");
                        completions.addAll(filterCompletions(cleanupCommands, args[1]));
                    }
                    break;
                    
                case "config":
                    if (permissionManager.hasPermission(sender, PermissionManager.CONFIG)) {
                        List<String> configCommands = Arrays.asList(
                            "interval", "items", "mobs", "animals", "experience", "exp", "arrows",
                            "broadcast", "async", "tps-monitor",
                            "item-age", "exp-age", "arrow-age", "list", "get", "reload"
                        );
                        // livestock-check和livestock-limit已移除，现在使用YAML配置
                        completions.addAll(filterCompletions(configCommands, args[1]));
                    }
                    break;
                    
                case "permission":
                    if (permissionManager.hasPermission(sender, PermissionManager.ADMIN)) {
                        List<String> permissionCommands = Arrays.asList("give", "add", "remove", "delete", "list", "show", "reload");
                        completions.addAll(filterCompletions(permissionCommands, args[1]));
                    }
                    break;
                    
                case "test":
                    if (permissionManager.hasPermission(sender, PermissionManager.ADMIN)) {
                        List<String> testCommands = Arrays.asList("timer", "status");
                        completions.addAll(filterCompletions(testCommands, args[1]));
                    }
                    break;
            }
        } else if (args.length == 3) {
            // 三级命令补全
            String mainCommand = args[0].toLowerCase();
            String subCommand = args[1].toLowerCase();
            
            if (mainCommand.equals("config")) {
                if (permissionManager.hasPermission(sender, PermissionManager.CONFIG)) {
                    completions.addAll(getConfigValueCompletions(subCommand, args[2]));
                }
            } else if (mainCommand.equals("permission")) {
                if (permissionManager.hasPermission(sender, PermissionManager.ADMIN)) {
                    if (subCommand.equals("give") || subCommand.equals("add") || 
                        subCommand.equals("remove") || subCommand.equals("delete") ||
                        subCommand.equals("list") || subCommand.equals("show")) {
                        // 玩家名补全
                        completions.addAll(getPlayerNameCompletions(args[2]));
                    }
                }
            }
        } else if (args.length == 4) {
            // 四级命令补全
            String mainCommand = args[0].toLowerCase();
            String subCommand = args[1].toLowerCase();
            
            if (mainCommand.equals("permission")) {
                if (permissionManager.hasPermission(sender, PermissionManager.ADMIN)) {
                    if (subCommand.equals("give") || subCommand.equals("add") || 
                        subCommand.equals("remove") || subCommand.equals("delete")) {
                        // 权限节点补全
                        List<String> permissions = Arrays.asList("admin", "cleanup", "stats", "config");
                        completions.addAll(filterCompletions(permissions, args[3]));
                    }
                }
            }
        }
        
        return completions;
    }

    /**
     * 获取配置值补全
     */
    private List<String> getConfigValueCompletions(String configKey, String input) {
        List<String> completions = new ArrayList<>();
        
        // 如果是get命令，返回所有可用的配置项
        if (configKey.equals("get")) {
            List<String> configItems = Arrays.asList(
                "interval", "items", "mobs", "animals", "experience", "exp", "arrows",
                "broadcast", "async", "tps-monitor",
                "item-age", "exp-age", "arrow-age"
            );
            // livestock配置项已移除，现在使用YAML配置
            completions.addAll(filterCompletions(configItems, input));
            return completions;
        }
        
        switch (configKey) {
            case "items":
            case "mobs":
            case "animals":
            case "experience":
            case "exp":
            case "arrows":
            case "broadcast":
            case "async":
            case "tps-monitor":
            // livestock配置补全已移除，现在使用YAML配置
                
            case "interval":
                completions.addAll(filterCompletions(Arrays.asList("30", "60", "120", "300", "600"), input));
                break;
                
            case "item-age":
            case "exp-age":
            case "arrow-age":
                completions.addAll(filterCompletions(Arrays.asList("100", "200", "300", "600", "1200"), input));
                break;
        }
        
        return completions;
    }

    /**
     * 获取玩家名补全
     */
    private List<String> getPlayerNameCompletions(String input) {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(input.toLowerCase()))
                .collect(Collectors.toList());
    }

    /**
     * 过滤补全选项
     */
    private List<String> filterCompletions(List<String> options, String input) {
        return options.stream()
                .filter(option -> option.toLowerCase().startsWith(input.toLowerCase()))
                .collect(Collectors.toList());
    }

    /**
     * 检查命令权限
     */
    private boolean hasCommandPermission(CommandSender sender, String command) {
        switch (command) {
            case "cleanup":
                return permissionManager.hasPermission(sender, PermissionManager.CLEANUP);
            case "stats":
            case "tps":
            case "livestock-stats":
                return permissionManager.hasPermission(sender, PermissionManager.STATS);
            case "config":
            case "toggle":
                return permissionManager.hasPermission(sender, PermissionManager.CONFIG);
            case "permission":
            case "test":
                return permissionManager.hasPermission(sender, PermissionManager.ADMIN);
            case "help":
                return hasAnyPermission(sender);
            default:
                return false;
        }
    }

    /**
     * 检查是否有任何权限
     */
    private boolean hasAnyPermission(CommandSender sender) {
        return permissionManager.hasPermission(sender, PermissionManager.ADMIN) ||
               permissionManager.hasPermission(sender, PermissionManager.CLEANUP) ||
               permissionManager.hasPermission(sender, PermissionManager.STATS) ||
               permissionManager.hasPermission(sender, PermissionManager.CONFIG);
    }

    /**
     * 检查权限
     */
    // 权限检查方法已移至PermissionManager统一处理
}