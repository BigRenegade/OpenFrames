package openframes.util;
import net.minecraft.world.WorldServer;
public class TeleportativeSpectreTeleporter extends net.minecraft.world.Teleporter {
    public TeleportativeSpectreTeleporter(net.minecraft.world.World World) {
        super((WorldServer) World);
    }

    @Override
    public void placeInPortal(net.minecraft.entity.Entity Entity, double X, double Y, double Z, float Yaw) {
    }
}
