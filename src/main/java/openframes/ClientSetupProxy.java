package openframes;


public class ClientSetupProxy {
    public static final String ClientSideClassName = OpenFrames.Namespace + "ClientSetup";
    public static final String ServerSideClassName = OpenFrames.Namespace + "ClientSetupProxy";

    @cpw.mods.fml.common.SidedProxy(clientSide = ClientSideClassName, serverSide = ServerSideClassName)
    public static ClientSetupProxy Instance;

    public void Execute() {
    }
}
