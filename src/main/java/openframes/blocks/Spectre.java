package openframes.blocks;

import openframes.core.Configuration;
import openframes.entity.MotiveSpectreEntity;
import openframes.entity.TeleportativeSpectreEntity;
import openframes.items.SpectreItem;
import openframes.items.ToolItemSet;
import openframes.util.WorldUtil;

public class Spectre extends Block {
    public Spectre() {
        super(Configuration.BlockIds.Spectre, bedrock, SpectreItem.class, MotiveSpectreEntity.class, null, TeleportativeSpectreEntity.class);

        RenderId = -1;
    }

    public enum Types {
        Motive,
        Supportive,
        Teleportative;
    }

    @Override
    public boolean onBlockActivated(net.minecraft.world.World World, int X, int Y, int Z, net.minecraft.entity.player.EntityPlayer Player, int Side, float HitX, float HitY, float HitZ) {
        if (World.isRemote) {
            return (false);
        }

        if (World.getBlockMetadata(X, Y, Z) != Types.Supportive.ordinal()) {
            return (false);
        }

        if (!ToolItemSet.IsScrewdriverOrEquivalent(Player.inventory.getCurrentItem())) {
            return (false);
        }

        WorldUtil.ClearBlock(World, X, Y, Z);

        return (true);
    }

    @Override
    public boolean isOpaqueCube() {
        return (false);
    }

    @Override
    public boolean renderAsNormalBlock() {
        return (false);
    }
}
