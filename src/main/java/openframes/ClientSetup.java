package openframes;

import openframes.blocks.TileEntity;
import openframes.entity.MotiveSpectreEntity;
import openframes.entity.TeleportativeSpectreEntity;
import openframes.renders.CarriageDriveRenderer;
import openframes.renders.CarriageRenderer;
import openframes.renders.MotiveSpectreRenderer;
import openframes.renders.TeleportativeSpectreRenderer;
import openframes.renders.TileEntityRenderer;

public class ClientSetup extends ClientSetupProxy {
    public void RegisterTileEntityRenderer(TileEntityRenderer Renderer, Class<? extends TileEntity>... EntityClasses) {
        for (Class<? extends TileEntity> EntityClass : EntityClasses) {
            cpw.mods.fml.client.registry.ClientRegistry.bindTileEntitySpecialRenderer(EntityClass, Renderer);
        }
    }

    @Override
    public void Execute() {
        RegisterTileEntityRenderer(new MotiveSpectreRenderer(), MotiveSpectreEntity.class);

        RegisterTileEntityRenderer(new TeleportativeSpectreRenderer(), TeleportativeSpectreEntity.class);

        new CarriageRenderer();

        new CarriageDriveRenderer();
    }
}
