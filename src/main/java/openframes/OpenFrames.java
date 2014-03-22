package openframes;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.network.NetworkMod;
import openframes.core.Configuration;
import openframes.core.Core;
import openframes.core.PacketManager;

@Mod(modid = BuildInfo.modID, name = BuildInfo.modName, version = BuildInfo.versionNumber + "-" + BuildInfo.buildNumber, dependencies = "after:ComputerCraft;after:OpenComputers;")
@NetworkMod(clientSideRequired = true, serverSideRequired = false, channels = { BuildInfo.modID }, packetHandler = openframes.core.PacketManager.class)

public class OpenFrames {
    public static final String Namespace = "openframes.";

    public static final String Handle = BuildInfo.modName;

    public static final String Title = BuildInfo.modName;

    public static final String Version = BuildInfo.versionNumber + "-" + BuildInfo.buildNumber;

    public static final String Channel = BuildInfo.modID;

    @EventHandler
    public void PreInit(cpw.mods.fml.common.event.FMLPreInitializationEvent Event) {
        (new Configuration(Event.getSuggestedConfigurationFile())).Process();

        Core.HandlePreInit();
    }

    @EventHandler
    public void Init(cpw.mods.fml.common.event.FMLInitializationEvent Event) {
        Core.HandleInit();
    }

    @EventHandler
    public void PostInit(cpw.mods.fml.common.event.FMLPostInitializationEvent Event) {
        ClientSetupProxy.Instance.Execute();

        Core.HandlePostInit();
    }

    @EventHandler
    public void ServerStopping(cpw.mods.fml.common.event.FMLServerStoppingEvent Event) {
        Core.HandleServerStopping();
    }
}
