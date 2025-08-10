package com.xiaoxiao.arissweeping;

import com.xiaoxiao.arissweeping.config.ModConfig;
import com.xiaoxiao.arissweeping.handler.EntityCleanupHandler;
import com.xiaoxiao.arissweeping.command.CleanupCommand;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(ArisSweeping.MODID)
public class ArisSweeping {
    public static final String MODID = "arissweeping";
    public static final Logger LOGGER = LogManager.getLogger();
    
    public ArisSweeping() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        
        // 注册配置
        ModLoadingContext.get().registerConfig(Type.SERVER, ModConfig.SPEC);
        
        // 注册事件监听器
        modEventBus.addListener(this::commonSetup);
        
        // 注册到Forge事件总线
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(CleanupCommand.class);
        
        LOGGER.info("Aris Sweeping mod initialized!");
    }
    
    private void commonSetup(final FMLCommonSetupEvent event) {
        // 初始化实体清理处理器
        EntityCleanupHandler.init();
        LOGGER.info("Aris Sweeping setup completed!");
    }
}