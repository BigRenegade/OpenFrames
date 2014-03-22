package openframes.renders;

import openframes.blocks.Block;
import openframes.blocks.BlockItem;
import openframes.blocks.BlockRecord;
import openframes.blocks.BlockRenderer;
import openframes.blocks.Blocks;
import openframes.carriage.Carriage;
import openframes.entity.TemplateCarriageEntity;
import openframes.items.CarriageItem;
import openframes.util.Directions;

public class CarriageRenderer extends BlockRenderer {
    public CarriageRenderer() {
        Blocks.Carriage.RenderId = Initialize(Blocks.Carriage.blockID);
    }

    @Override
    public void Render(net.minecraft.tileentity.TileEntity TileEntity) {
        if (TileEntity instanceof TemplateCarriageEntity) {
            TemplateCarriageEntity Carriage = (TemplateCarriageEntity) TileEntity;

            if (Carriage.Pattern == null) {
                return;
            }

            if (Carriage.RenderPattern == false) {
                return;
            }

            SetSideSpan(0, 0, 1, 1);

            SetTextureSpan(openframes.carriage.Carriage.PlaceholderIcon);

            for (BlockRecord Record : Carriage.Pattern) {
                RenderGhost(Record.X, Record.Y, Record.Z);
            }
        }
    }

    @Override
    public net.minecraft.util.Icon GetIcon(net.minecraft.item.ItemStack Item, Directions Side) {
        if (Side != Directions.PosY) {
            int DecorationId = CarriageItem.GetDecorationId(Item);

            if (DecorationId != 0) {
                int DecorationMeta = CarriageItem.GetDecorationMeta(Item);

                try {
                    return (Block.Get(DecorationId).getIcon(Side.ordinal(), DecorationMeta));
                } catch (Throwable Throwable) {
                    Throwable.printStackTrace();
                }
            }
        }

        return (Blocks.Carriage.getIcon(0, BlockItem.GetBlockType(Item)));
    }
}
