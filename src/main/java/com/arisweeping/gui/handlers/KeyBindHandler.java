package com.arisweeping.gui.handlers;

import com.arisweeping.core.ArisLogger;
import com.arisweeping.core.ArisSweepingMod;
import com.arisweeping.gui.MiniHUDConfigScreen;
import com.arisweeping.gui.hud.HUDManager;
import com.arisweeping.tasks.SmartTaskManager;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * 热键绑定处理器
 * 管理模组的所有快捷键功能
 */
@Mod.EventBusSubscriber(modid = ArisSweepingMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class KeyBindHandler {
    
    // 热键定义
    public static final KeyMapping CONFIG_KEY = new KeyMapping(
            "key.arisweeping.config",
            GLFW.GLFW_KEY_K,
            "key.categories.arisweeping"
    );
    
    public static final KeyMapping TOGGLE_HUD_KEY = new KeyMapping(
            "key.arisweeping.toggle_hud",
            GLFW.GLFW_KEY_F9,
            "key.categories.arisweeping"
    );
    
    public static final KeyMapping TOGGLE_MOD_KEY = new KeyMapping(
            "key.arisweeping.toggle_mod",
            GLFW.GLFW_KEY_J,
            "key.categories.arisweeping"
    );
    
    public static final KeyMapping UNDO_KEY = new KeyMapping(
            "key.arisweeping.undo",
            GLFW.GLFW_KEY_U,
            "key.categories.arisweeping"
    );
    
    /**
     * 注册热键映射
     */
    @SubscribeEvent
    public static void onKeyRegister(RegisterKeyMappingsEvent event) {
        ArisLogger.info("注册ArisSweeping热键绑定...");
        
        event.register(CONFIG_KEY);
        event.register(TOGGLE_HUD_KEY);
        event.register(TOGGLE_MOD_KEY);
        event.register(UNDO_KEY);
        
        ArisLogger.info("✓ 已注册4个热键绑定");
    }
    
    /**
     * 处理热键输入事件
     */
    @Mod.EventBusSubscriber(modid = ArisSweepingMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class InputEventHandler {
        
        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            Minecraft mc = Minecraft.getInstance();
            
            // 只在游戏中且没有打开GUI时处理热键
            if (mc.screen != null) return;
            
            try {
                // 配置界面热键 (K键)
                if (CONFIG_KEY.consumeClick()) {
                    ArisLogger.debug("配置热键被按下");
                    openConfigScreen();
                }
                
                // HUD显示切换热键 (F9键)
                if (TOGGLE_HUD_KEY.consumeClick()) {
                    ArisLogger.debug("HUD切换热键被按下");
                    toggleHUD();
                }
                
                // 模组开关热键 (J键)
                if (TOGGLE_MOD_KEY.consumeClick()) {
                    ArisLogger.debug("模组切换热键被按下");
                    toggleMod();
                }
                
                // 撤销热键 (U键)
                if (UNDO_KEY.consumeClick()) {
                    ArisLogger.debug("撤销热键被按下");
                    undoLastOperation();
                }
            } catch (Exception e) {
                ArisLogger.error("处理热键输入时发生错误", e);
            }
        }
        
        /**
         * 打开配置界面
         */
        private static void openConfigScreen() {
            Minecraft mc = Minecraft.getInstance();
            mc.setScreen(new MiniHUDConfigScreen(mc.screen));
            
            // 显示提示消息
            showMessage("§a配置界面已打开");
        }
        
        /**
         * 切换HUD显示
         */
        private static void toggleHUD() {
            boolean newState = HUDManager.toggleHUD();
            String status = newState ? "§a已显示" : "§c已隐藏";
            showMessage("HUD显示: " + status);
        }
        
        /**
         * 切换模组启用状态
         */
        private static void toggleMod() {
            boolean newState = !ArisSweepingMod.isEnabled();
            ArisSweepingMod.setEnabled(newState);
            
            String status = newState ? "§a已启用" : "§c已禁用";
            showMessage("ArisSweeping: " + status);
        }
        
        /**
         * 撤销上次操作
         */
        private static void undoLastOperation() {
            try {
                SmartTaskManager taskManager = SmartTaskManager.getInstance();
                if (taskManager != null) {
                    // 执行撤销操作
                    boolean success = taskManager.undoLastOperation();
                    if (success) {
                        showMessage("§a已撤销上次操作");
                    } else {
                        showMessage("§e没有可撤销的操作");
                    }
                } else {
                    showMessage("§c任务管理器未初始化");
                }
            } catch (Exception e) {
                ArisLogger.error("撤销操作失败", e);
                showMessage("§c撤销操作失败");
            }
        }
        
        /**
         * 显示消息提示
         */
        private static void showMessage(String message) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.gui != null) {
                mc.gui.setOverlayMessage(Component.literal(message), false);
            }
        }
    }
}