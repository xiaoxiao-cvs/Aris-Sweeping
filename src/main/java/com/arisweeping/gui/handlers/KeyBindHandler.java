package com.arisweeping.gui.handlers;

import com.arisweeping.core.ArisSweepingMod;
import fi.dy.masa.malilib.gui.GuiBase;
import com.arisweeping.gui.GuiConfigsAris;
import com.arisweeping.config.Configs;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 键盘绑定处理器
 * 使用 malilib 的热键系统管理配置界面
 */
@Mod.EventBusSubscriber(modid = ArisSweepingMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
@OnlyIn(Dist.CLIENT)
public class KeyBindHandler {
    
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        
        // 确保不在聊天输入状态且玩家在世界中
        if (mc.screen != null || mc.player == null) {
            return;
        }
        
        // 检查配置GUI热键
        if (Configs.General.CONFIG_GUI_HOTKEY.getKeybind().wasTriggered()) {
            GuiBase.openGui(new GuiConfigsAris());
        }
        
        // 主功能切换热键由 malilib 自动处理
        // 无需在此处理 MAIN_TOGGLE，它会自动执行其布尔值切换
    }
}