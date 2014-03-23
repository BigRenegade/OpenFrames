package openframes.entity;

import openframes.blocks.BlockPosition;
import openframes.blocks.BlockRecord;
import openframes.blocks.BlockRecordList;
import openframes.blocks.BlockRecordSet;
import openframes.blocks.Blocks;
import openframes.blocks.TileEntity;
import openframes.carriage.CarriagePackage;
import openframes.core.Configuration;
import openframes.core.MultipartPropagationPacket;
import openframes.core.Reflection;
import openframes.renders.CarriageRenderCache;
import openframes.util.Directions;
import openframes.util.ModInteraction;
import openframes.util.SneakyWorldUtil;
import net.minecraft.server.management.PlayerInstance;
import net.minecraft.world.WorldServer;

public class MotiveSpectreEntity extends TileEntity {
    public Directions MotionDirection;

    public BlockPosition RenderCacheKey;

    public net.minecraft.nbt.NBTTagList PendingBlockUpdates;

    public BlockRecord DriveRecord;

    public boolean DriveIsAnchored;

    public BlockRecordSet Body;

    public int TicksExisted;

    public static double Velocity;

    public void ShiftBlockPosition(BlockRecord Record) {
        Record.Shift(MotionDirection);
    }

    public void ShiftPipeItemPosition(Object Position) throws Throwable {
        ModInteraction.BC_Position_x.set(Position, ((Double) ModInteraction.BC_Position_x.get(Position)) + MotionDirection.DeltaX);
        ModInteraction.BC_Position_y.set(Position, ((Double) ModInteraction.BC_Position_y.get(Position)) + MotionDirection.DeltaY);
        ModInteraction.BC_Position_z.set(Position, ((Double) ModInteraction.BC_Position_z.get(Position)) + MotionDirection.DeltaZ);
    }

    public void ScheduleShiftedBlockUpdate(net.minecraft.nbt.NBTTagCompound PendingBlockUpdateRecord) {
        worldObj.scheduleBlockUpdateFromLoad
                (
                        PendingBlockUpdateRecord.getInteger("X") + MotionDirection.DeltaX,
                        PendingBlockUpdateRecord.getInteger("Y") + MotionDirection.DeltaY,
                        PendingBlockUpdateRecord.getInteger("Z") + MotionDirection.DeltaZ,

                        PendingBlockUpdateRecord.getInteger("Id"),

                        PendingBlockUpdateRecord.getInteger("Delay"),

                        PendingBlockUpdateRecord.getInteger("Priority")
                );
    }

    @Override
    public void updateEntity() {
        TicksExisted++;

        for (CapturedEntity Entity : CapturedEntities) {
            Entity.Update();
        }

        if (worldObj.isRemote) {
            return;
        }

        if (TicksExisted < Configuration.CarriageMotion.MotionDuration) {
            return;
        }

        Release();
    }

    public static int MultipartContainerBlockId;

    public void Release() {
        for (BlockRecord Record : Body) {
            ShiftBlockPosition(Record);

            SneakyWorldUtil.SetBlock(worldObj, Record.X, Record.Y, Record.Z, Record.Id, Record.Meta);
        }

        BlockRecordList PipesToInitialize = new BlockRecordList();

        if (ModInteraction.ForgeMultipart.MultipartSaveLoad_loadingWorld_$eq != null) {
            try {
                ModInteraction.ForgeMultipart.MultipartSaveLoad_loadingWorld_$eq.invoke(null, worldObj);
            } catch (Throwable Throwable) {
                Throwable.printStackTrace();
            }
        }

        BlockRecordList MultipartTilesToInitialize = new BlockRecordList();

        java.util.HashMap<net.minecraft.world.chunk.Chunk, java.util.HashMap<Object, net.minecraft.tileentity.TileEntity>> MultipartTileSetsToPropagate =
                new java.util.HashMap<net.minecraft.world.chunk.Chunk, java.util.HashMap<Object, net.minecraft.tileentity.TileEntity>>();

        for (BlockRecord Record : Body) {
            if (Record.EntityRecord != null) {
                Record.EntityRecord.setInteger("x", Record.X);
                Record.EntityRecord.setInteger("y", Record.Y);
                Record.EntityRecord.setInteger("z", Record.Z);

                if (Record.EntityRecord.getString("id").equals("savedMultipart")) {
                    try {
                        if (ModInteraction.ForgeMultipart.MultipartHelper_createTileFromNBT != null) {
                            Record.Entity = (net.minecraft.tileentity.TileEntity) ModInteraction.ForgeMultipart.MultipartHelper_createTileFromNBT
                                    .invoke(null, worldObj, Record.EntityRecord);
                        } else {
                            Record.Entity = (net.minecraft.tileentity.TileEntity) ModInteraction.ForgeMultipart.TileMultipart_createFromNBT.invoke(null, Record.EntityRecord);

                            MultipartContainerBlockId = Record.Id;

                            net.minecraft.world.chunk.Chunk Chunk = worldObj.getChunkFromBlockCoords(Record.X, Record.Z);

                            java.util.HashMap<Object, net.minecraft.tileentity.TileEntity> MultipartTilesToPropagate = MultipartTileSetsToPropagate.get(Chunk);

                            if (MultipartTilesToPropagate == null) {
                                MultipartTilesToPropagate = new java.util.HashMap<Object, net.minecraft.tileentity.TileEntity>();

                                MultipartTileSetsToPropagate.put(Chunk, MultipartTilesToPropagate);
                            }

                            MultipartTilesToPropagate.put(Record.Entity, Record.Entity);
                        }

                        MultipartTilesToInitialize.add(Record);
                    } catch (Throwable Throwable) {
                        Throwable.printStackTrace();

                        continue;
                    }
                } else {
                    Record.Entity = net.minecraft.tileentity.TileEntity.createAndLoadEntity(Record.EntityRecord);
                }

                SneakyWorldUtil.SetTileEntity(worldObj, Record.X, Record.Y, Record.Z, Record.Entity);

                if (Configuration.DirtyHacks.UpdateBuildcraftPipes) {
                    if (ModInteraction.BC_TileGenericPipe != null) {
                        if (ModInteraction.BC_TileGenericPipe.isInstance(Record.Entity)) {
                            PipesToInitialize.add(Record);
                        }
                    }
                }
            }
        }

/*		for ( BlockRecord Record : MultipartTilesToInitialize )
        {
			try
			{
				ModInteraction . ForgeMultipart . TileMultipart_onChunkLoad . invoke ( Record . Entity ) ;
			}
			catch ( Throwable Throwable )
			{
				Throwable . printStackTrace ( ) ;
			}
		}*/

        if (ModInteraction.ForgeMultipart.MultipartHelper_sendDescPacket != null) {
            for (BlockRecord Record : MultipartTilesToInitialize) {
                try {
                    ModInteraction.ForgeMultipart.MultipartHelper_sendDescPacket.invoke(null, worldObj, Record.Entity);
                } catch (Throwable Throwable) {
                    Throwable.printStackTrace();
                }
            }
        } else {
            for (java.util.Map.Entry<net.minecraft.world.chunk.Chunk, java.util.HashMap<Object, net.minecraft.tileentity.TileEntity>> MultipartTilesToPropagate
                    : MultipartTileSetsToPropagate.entrySet()) {
                net.minecraft.world.chunk.Chunk Chunk = MultipartTilesToPropagate.getKey();

                java.util.Map SavedTileEntityMap = Chunk.chunkTileEntityMap;

                Chunk.chunkTileEntityMap = MultipartTilesToPropagate.getValue();

                try {
                    final PlayerInstance watcher = ((WorldServer) worldObj).getPlayerManager().getOrCreateChunkWatcher(Chunk.xPosition, Chunk.zPosition, false);
                    final java.util.List<net.minecraft.entity.player.EntityPlayerMP> players = (java.util.List<net.minecraft.entity.player.EntityPlayerMP>) Reflection.EstablishField(PlayerInstance.class, "playersInChunk").get(watcher);
                    for (net.minecraft.entity.player.EntityPlayerMP Player : players) {
                        if (!Player.loadedChunks.contains(Chunk.getChunkCoordIntPair())) {
                            try {
                                if (ModInteraction.ForgeMultipart.MultipartSaveLoad_loadingWorld_$eq == null) {
                                    MultipartPropagationPacket.Dispatch(Player, MultipartTilesToPropagate.getValue().values());
                                }

                                ModInteraction.ForgeMultipart.MultipartSPH_onChunkWatch.invoke(null, Player, Chunk);
                            } catch (Throwable Throwable) {
                                Throwable.printStackTrace();
                            }
                        }
                    }
                } catch (Throwable Throwable) {
                    Throwable.printStackTrace();
                }

                Chunk.chunkTileEntityMap = SavedTileEntityMap;
            }
        }

        for (BlockRecord Record : PipesToInitialize) {
            try {
                Object Pipe = ModInteraction.BC_TileGenericPipe_pipe.get(Record.Entity);

                ModInteraction.BC_TileGenericPipe_initialize.invoke(Record.Entity, Pipe);

                Object Transport = ModInteraction.BC_Pipe_transport.get(Pipe);

                if (!ModInteraction.BC_PipeTransportItems.isInstance(Transport)) {
                    continue;
                }

                ModInteraction.BC_PipeTransportItems_delay.set(Transport, -1);

                java.util.List DelayedEntities = (java.util.List) ModInteraction.BC_PipeTransportItems_delayedEntitiesToLoad.get(Transport);

                java.util.Map EntityMap = (java.util.Map) ModInteraction.BC_PipeTransportItems_travelingEntities.get(Transport);

                for (Object Entity : DelayedEntities) {
                    Object Item = ModInteraction.BC_EntityData_item.get(Entity);

                    ModInteraction.BC_EntityPassiveItem_setWorld.invoke(Item, worldObj);

                    int Id = (Integer) ModInteraction.BC_EntityPassiveItem_getEntityId.invoke(Item);

                    EntityMap.put(Id, Entity);

                    Object ItemPosition = ModInteraction.BC_EntityPassiveItem_position.get(Item);

                    ShiftPipeItemPosition(ItemPosition);
                }

                DelayedEntities.clear();
            } catch (Throwable Throwable) {
                Throwable.printStackTrace();
            }
        }

        try {
            CarriageDriveEntity Drive = (CarriageDriveEntity) worldObj.getBlockTileEntity(DriveRecord.X, DriveRecord.Y, DriveRecord.Z);

            if (!DriveIsAnchored) {
                Drive.Active = true;
            }

            Drive.ToggleActivity();
        } catch (Throwable Throwable) {
            Throwable.printStackTrace();
        }

        SneakyWorldUtil.RefreshBlock(worldObj, xCoord, yCoord, zCoord, Blocks.Spectre.blockID, 0);

        for (BlockRecord Record : Body) {
            SneakyWorldUtil.RefreshBlock(worldObj, Record.X, Record.Y, Record.Z, 0, Record.Id);
        }

        int PendingBlockUpdateCount = PendingBlockUpdates.tagCount();

        for (int Index = 0; Index < PendingBlockUpdateCount; Index++) {
            ScheduleShiftedBlockUpdate((net.minecraft.nbt.NBTTagCompound) PendingBlockUpdates.tagAt(Index));
        }
    }

    @Override
    public void Finalize() {
        if (worldObj.isRemote) {
            CarriageRenderCache.Release(RenderCacheKey);
        }
    }

    @Override
    public void WriteCommonRecord(net.minecraft.nbt.NBTTagCompound TagCompound) {
        TagCompound.setInteger("Motion", MotionDirection.ordinal());

        TagCompound.setInteger("RenderCacheKeyX", RenderCacheKey.X);
        TagCompound.setInteger("RenderCacheKeyY", RenderCacheKey.Y);
        TagCompound.setInteger("RenderCacheKeyZ", RenderCacheKey.Z);

        TagCompound.setInteger("RenderCacheKeyD", RenderCacheKey.Dimension);
    }

    @Override
    public void ReadCommonRecord(net.minecraft.nbt.NBTTagCompound TagCompound) {
        MotionDirection = Directions.values()[TagCompound.getInteger("Motion")];

        RenderCacheKey = new BlockPosition(TagCompound.getInteger("RenderCacheKeyX"), TagCompound.getInteger("RenderCacheKeyY"), TagCompound.getInteger("RenderCacheKeyZ"),
                TagCompound.getInteger("RenderCacheKeyD"));
    }

    @Override
    public void WriteServerRecord(net.minecraft.nbt.NBTTagCompound TagCompound) {
        TagCompound.setInteger("DriveX", DriveRecord.X);
        TagCompound.setInteger("DriveY", DriveRecord.Y);
        TagCompound.setInteger("DriveZ", DriveRecord.Z);

        TagCompound.setBoolean("DriveIsAnchored", DriveIsAnchored);

        TagCompound.setTag("PendingBlockUpdates", PendingBlockUpdates);

        {
            net.minecraft.nbt.NBTTagList BodyRecord = new net.minecraft.nbt.NBTTagList();

            for (BlockRecord Record : Body) {
                net.minecraft.nbt.NBTTagCompound BodyBlockRecord = new net.minecraft.nbt.NBTTagCompound();

                BodyBlockRecord.setInteger("X", Record.X);
                BodyBlockRecord.setInteger("Y", Record.Y);
                BodyBlockRecord.setInteger("Z", Record.Z);

                BodyBlockRecord.setInteger("Id", Record.Id);

                BodyBlockRecord.setInteger("Meta", Record.Meta);

                if (Record.EntityRecord != null) {
                    BodyBlockRecord.setCompoundTag("EntityRecord", Record.EntityRecord);
                }

                BodyRecord.appendTag(BodyBlockRecord);
            }

            TagCompound.setTag("Body", BodyRecord);
        }
    }

    @Override
    public void ReadServerRecord(net.minecraft.nbt.NBTTagCompound TagCompound) {
        DriveRecord = new BlockRecord(TagCompound.getInteger("DriveX"), TagCompound.getInteger("DriveY"), TagCompound.getInteger("DriveZ"));

        DriveIsAnchored = TagCompound.getBoolean("DriveIsAnchored");

        PendingBlockUpdates = TagCompound.getTagList("PendingBlockUpdates");

        Body = new BlockRecordSet();

        {
            net.minecraft.nbt.NBTTagList BodyRecord = TagCompound.getTagList("Body");

            int BodyBlockCount = BodyRecord.tagCount();

            for (int Index = 0; Index < BodyBlockCount; Index++) {
                net.minecraft.nbt.NBTTagCompound BodyBlockRecord = (net.minecraft.nbt.NBTTagCompound) BodyRecord.tagAt(Index);

                BlockRecord Record = new BlockRecord(BodyBlockRecord.getInteger("X"), BodyBlockRecord.getInteger("Y"), BodyBlockRecord.getInteger("Z"));

                Record.Id = BodyBlockRecord.getInteger("Id");

                Record.Meta = BodyBlockRecord.getInteger("Meta");

                if (BodyBlockRecord.hasKey("EntityRecord")) {
                    Record.EntityRecord = BodyBlockRecord.getCompoundTag("EntityRecord");
                }

                Body.add(Record);
            }
        }
    }

    @Override
    public void WriteClientRecord(net.minecraft.nbt.NBTTagCompound TagCompound) {
        net.minecraft.nbt.NBTTagList CapturedEntityRecords = new net.minecraft.nbt.NBTTagList();

        for (CapturedEntity Entity : CapturedEntities) {
            net.minecraft.nbt.NBTTagCompound CapturedEntityRecord = new net.minecraft.nbt.NBTTagCompound();

            CapturedEntityRecord.setInteger("Id", Entity.Entity.entityId);

            CapturedEntityRecord.setDouble("InitialX", Entity.InitialX);
            CapturedEntityRecord.setDouble("InitialY", Entity.InitialY);
            CapturedEntityRecord.setDouble("InitialZ", Entity.InitialZ);

            CapturedEntityRecords.appendTag(CapturedEntityRecord);
        }

        TagCompound.setTag("CapturedEntities", CapturedEntityRecords);
    }

    @Override
    public void ReadClientRecord(net.minecraft.nbt.NBTTagCompound TagCompound) {
        net.minecraft.nbt.NBTTagList CapturedEntityRecords = TagCompound.getTagList("CapturedEntities");

        CapturedEntities.clear();

        int CapturedEntityCount = CapturedEntityRecords.tagCount();

        for (int Index = 0; Index < CapturedEntityCount; Index++) {
            net.minecraft.nbt.NBTTagCompound EntityRecord = (net.minecraft.nbt.NBTTagCompound) CapturedEntityRecords.tagAt(Index);

            net.minecraft.entity.Entity Entity = worldObj.getEntityByID(EntityRecord.getInteger("Id"));

            if (Entity == null) {
                continue;
            }

            CapturedEntities.add(new CapturedEntity(Entity, EntityRecord.getDouble("InitialX"), EntityRecord.getDouble("InitialY"), EntityRecord.getDouble("InitialZ")));
        }
    }

    public class CapturedEntity {
        public net.minecraft.entity.Entity Entity;

        public double InitialX;
        public double InitialY;
        public double InitialZ;

        boolean WasOnGround;

        boolean WasAirBorne;

        public CapturedEntity(net.minecraft.entity.Entity Entity) {
            this(Entity, Entity.posX, Entity.posY, Entity.posZ);
        }

        public CapturedEntity(net.minecraft.entity.Entity Entity, double InitialX, double InitialY, double InitialZ) {
            this.Entity = Entity;

            this.InitialX = InitialX;
            this.InitialY = InitialY;
            this.InitialZ = InitialZ;

            WasOnGround = Entity.onGround;

            WasAirBorne = Entity.isAirBorne;

            Update();
        }

        public void SetPosition(double OffsetX, double OffsetY, double OffsetZ) {
            Entity.setPosition(InitialX + OffsetX, InitialY + OffsetY + Entity.yOffset, InitialZ + OffsetZ);
        }

        public void Update() {
            Entity.fallDistance = 0;

            if (TicksExisted == Configuration.CarriageMotion.MotionDuration) {
                Entity.motionX = 0;
                Entity.motionY = 0;
                Entity.motionZ = 0;

                SetPosition(MotionDirection.DeltaX, MotionDirection.DeltaY, MotionDirection.DeltaZ);

                Entity.prevPosX = Entity.posX;
                Entity.prevPosY = Entity.posY;
                Entity.prevPosZ = Entity.posZ;

                Entity.onGround = WasOnGround;

                Entity.isAirBorne = WasAirBorne;

                return;
            }

            Entity.onGround = false;

            Entity.isAirBorne = true;

            Entity.motionX = Velocity * MotionDirection.DeltaX;
            Entity.motionY = Velocity * MotionDirection.DeltaY;
            Entity.motionZ = Velocity * MotionDirection.DeltaZ;

            SetPosition(Entity.motionX * TicksExisted, Entity.motionY * TicksExisted, Entity.motionZ * TicksExisted);

            Entity.prevPosX = Entity.posX - Entity.motionX;
            Entity.prevPosY = Entity.posY - Entity.motionY;
            Entity.prevPosZ = Entity.posZ - Entity.motionZ;
        }
    }

    public java.util.ArrayList<CapturedEntity> CapturedEntities = new java.util.ArrayList<CapturedEntity>();

    public boolean ShouldCaptureEntity(net.minecraft.entity.Entity Entity) {
        if (Entity instanceof net.minecraft.entity.player.EntityPlayer) {
            return (Configuration.CarriageMotion.CapturePlayerEntities);
        }

        if (Entity instanceof net.minecraft.entity.EntityLiving) {
            return (Configuration.CarriageMotion.CaptureOtherLivingEntities);
        }

        if (Entity instanceof net.minecraft.entity.item.EntityItem) {
            return (Configuration.CarriageMotion.CaptureItemEntities);
        }

        return (Configuration.CarriageMotion.CaptureOtherEntities);
    }

    public void ProcessCapturedEntity(net.minecraft.entity.Entity Entity) {
        CapturedEntities.add(new CapturedEntity(Entity));
    }

    public void CaptureEntities(int MinX, int MinY, int MinZ, int MaxX, int MaxY, int MaxZ) {
        net.minecraft.util.AxisAlignedBB EntityCaptureBox = net.minecraft.util.AxisAlignedBB.getBoundingBox(MinX - 5, MinY - 5, MinZ - 5, MaxX + 5, MaxY + 5, MaxZ + 5);

        java.util.List EntitiesFound = worldObj.getEntitiesWithinAABB(net.minecraft.entity.Entity.class, EntityCaptureBox);

        for (Object EntityObject : EntitiesFound) {
            net.minecraft.entity.Entity Entity = (net.minecraft.entity.Entity) EntityObject;

            BlockRecord PositionCheck = new BlockRecord((int) Math.floor(Entity.posX), (int) Math.floor(Entity.posY), (int) Math.floor(Entity.posZ));

            if (!Body.contains(PositionCheck)) {
                PositionCheck.Y--;

                if (!Body.contains(PositionCheck)) {
                    PositionCheck.Y--;

                    if (!Body.contains(PositionCheck)) {
                        Entity = null;
                    }
                }
            }

            if (Entity == null) {
                continue;
            }

            if (ShouldCaptureEntity(Entity)) {
                try {
                    ProcessCapturedEntity(Entity);
                } catch (Throwable Throwable) {
                    Throwable.printStackTrace();
                }
            }
        }
    }

    public void Absorb(CarriagePackage Package) {
        MotionDirection = Package.MotionDirection;

        Body = Package.Body;

        RenderCacheKey = Package.RenderCacheKey;

        PendingBlockUpdates = Package.PendingBlockUpdates;

        DriveRecord = new BlockRecord(Package.DriveRecord);

        if (!Package.DriveIsAnchored) {
            DriveRecord.Shift(Package.MotionDirection);
        }

        if (Package.MotionDirection != null) {
            CaptureEntities(Package.MinX, Package.MinY, Package.MinZ, Package.MaxX, Package.MaxY, Package.MaxZ);
        }
    }

    @Override
    public net.minecraft.util.AxisAlignedBB getRenderBoundingBox() {
        return (INFINITE_EXTENT_AABB);
    }

    @Override
    public boolean shouldRenderInPass(int Pass) {
        return (true);
    }
}
