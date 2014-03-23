package openframes.carriage;

import java.util.Set;
import java.util.TreeSet;

import openframes.blocks.Block;
import openframes.blocks.BlockPosition;
import openframes.blocks.BlockRecord;
import openframes.blocks.BlockRecordSet;
import openframes.blocks.Blocks;
import openframes.core.Configuration;
import openframes.core.Reflection;
import openframes.entity.CarriageDriveEntity;
import openframes.entity.CarriageEntity;
import openframes.util.Directions;
import openframes.util.ModInteraction;
import net.minecraft.world.WorldServer;

public class CarriagePackage {
    public BlockPosition RenderCacheKey;

    public CarriageTranslocatorEntity Translocator;

    public WorldServer World;

    public BlockRecord DriveRecord;

    public boolean DriveIsAnchored;

    public BlockRecord AnchorRecord;

    public Directions MotionDirection;

    public CarriagePackage(CarriageDriveEntity Drive, CarriageEntity Anchor, Directions MotionDirection) {
        World = (WorldServer) Drive.worldObj;

        DriveRecord = new BlockRecord(Drive.xCoord, Drive.yCoord, Drive.zCoord);

        DriveRecord.Identify(World);

        DriveIsAnchored = Drive.Anchored();

        AnchorRecord = new BlockRecord(Anchor.xCoord, Anchor.yCoord, Anchor.zCoord);

        AnchorRecord.Identify(World);

        this.MotionDirection = MotionDirection;
    }

    public boolean MatchesCarriageType(BlockRecord Record) {
        if (Record.Id == AnchorRecord.Id) {
            if (Record.Meta == AnchorRecord.Meta) {
                return (true);
            }
        }

        return (false);
    }

    public BlockRecordSet Body = new BlockRecordSet();

    public static int MaxBlockCount;

    public int MinX = Integer.MAX_VALUE;
    public int MinY = Integer.MAX_VALUE;
    public int MinZ = Integer.MAX_VALUE;

    public int MaxX = Integer.MIN_VALUE;
    public int MaxY = Integer.MIN_VALUE;
    public int MaxZ = Integer.MIN_VALUE;

    public BlockRecordSet NewPositions = new BlockRecordSet();

    public BlockRecordSet Carriages = new BlockRecordSet();
    public BlockRecordSet Cargo = new BlockRecordSet();

    public double Mass;

    public void AddBlock(BlockRecord Record) throws CarriageMotionException {
        if ((MotionDirection == Directions.PosY) && (Record.Y >= 254)) {
            throw (new CarriageObstructionException("cannot move carriage above height limit", Record.X, Record.Y, Record.Z));
        }

        if ((MotionDirection == Directions.NegY) && (Record.Y <= 0)) {
            throw (new CarriageObstructionException("cannot move carriage below depth limit", Record.X, Record.Y, Record.Z));
        }

        if (CarriagePackageBlacklist.Lookup(Record)) {
            throw (new CarriageObstructionException("carriage contains system-wide blacklisted block", Record.X, Record.Y, Record.Z));
        }

        Body.add(Record);

        if (MaxBlockCount > 0) {
            if (Body.size() > MaxBlockCount) {
                throw (new CarriageMotionException("carriage exceeds maximum size of " + MaxBlockCount + " blocks"));
            }
        }

        if (MotionDirection == null) {
            NewPositions.add(new BlockRecord(Record.X - DriveRecord.X, Record.Y - DriveRecord.Y, Record.Z - DriveRecord.Z));
        } else {
            NewPositions.add(Record.NextInDirection(MotionDirection));
        }

        MinX = Math.min(MinX, Record.X);
        MinY = Math.min(MinY, Record.Y);
        MinZ = Math.min(MinZ, Record.Z);

        MaxX = Math.max(MaxX, Record.X);
        MaxY = Math.max(MaxY, Record.Y);
        MaxZ = Math.max(MaxZ, Record.Z);

        if (Record.Entity != null) {
            Record.EntityRecord = new net.minecraft.nbt.NBTTagCompound();

            Record.Entity.writeToNBT(Record.EntityRecord);
        }

        if (Configuration.HardmodeActive) {
            if (Record.Id == Blocks.Carriage.blockID) {
                Carriages.add(Record);

                Mass += Carriage.Types.values()[Record.Meta].Burden * Carriage.Tiers.values()[((CarriageEntity) Record.Entity).Tier].CarriageBurdenFactor;
            } else {
                Cargo.add(Record);
            }
        }
    }

    public void FailBecauseObstructed(BlockRecord Record, String Type) throws CarriageMotionException {
        throw (new CarriageObstructionException("carriage motion obstructed by " + Type, Record.X, Record.Y, Record.Z));
    }

    public static boolean ObstructedByLiquids;

    public static boolean ObstructedByFragileBlocks;

    public void AssertNotObstruction(BlockRecord Record) throws CarriageMotionException {
        if (Body.contains(Record)) {
            return;
        }

        if (World.isAirBlock(Record.X, Record.Y, Record.Z)) {
            return;
        }

        if (Block.Get(World.getBlockId(Record.X, Record.Y, Record.Z)).blockMaterial.isLiquid()) {
            if (ObstructedByLiquids) {
                FailBecauseObstructed(Record, "liquid");
            }

            return;
        }

        net.minecraft.block.BlockFlowing blockFlowing = ((net.minecraft.block.BlockFlowing) net.minecraft.block.Block.waterMoving);
        // blockFlowing.blockBlocksFlow(World, Record.X, Record.Y, Record.Z)
        boolean blocks = false;
        try {
            blocks = (Boolean) Reflection.EstablishMethod(net.minecraft.block.BlockFlowing.class, "blockBlocksFlow", World.getClass(), int.class, int.class, int.class).invoke(blockFlowing, World, Record.X, Record.Y, Record.Z);
        } catch (Throwable ignore) {
        }
        if (!blocks) {
            if (ObstructedByFragileBlocks) {
                FailBecauseObstructed(Record, "fragile block");
            }

            return;
        }

        FailBecauseObstructed(Record, "block");
    }

    public BlockRecordSet PotentialObstructions = new BlockRecordSet();

    public void AddPotentialObstruction(BlockRecord Record) {
        PotentialObstructions.add(Record);
    }

    public net.minecraft.nbt.NBTTagList PendingBlockUpdates = new net.minecraft.nbt.NBTTagList();

    public void StorePendingBlockUpdateRecord(net.minecraft.world.NextTickListEntry PendingBlockUpdate, long WorldTime) {
        net.minecraft.nbt.NBTTagCompound PendingBlockUpdateRecord = new net.minecraft.nbt.NBTTagCompound();

        PendingBlockUpdateRecord.setInteger("X", PendingBlockUpdate.xCoord);
        PendingBlockUpdateRecord.setInteger("Y", PendingBlockUpdate.yCoord);
        PendingBlockUpdateRecord.setInteger("Z", PendingBlockUpdate.zCoord);

        PendingBlockUpdateRecord.setInteger("Id", PendingBlockUpdate.blockID);

        PendingBlockUpdateRecord.setInteger("Delay", (int) (PendingBlockUpdate.scheduledTime - WorldTime));

        PendingBlockUpdateRecord.setInteger("Priority", PendingBlockUpdate.priority);

        PendingBlockUpdates.appendTag(PendingBlockUpdateRecord);
    }

    java.util.TreeMap<BlockRecord, Double> CargoBurdenFactors = new java.util.TreeMap<BlockRecord, Double>();

    public void ApplyCargoBurdenFactor(BlockRecord Position, double Factor) {
        Double CurrFactor = CargoBurdenFactors.get(Position);

        if (CurrFactor == null) {
            CargoBurdenFactors.put(Position, Factor);
        } else {
            CargoBurdenFactors.put(Position, CurrFactor * Factor);
        }
    }

    public void Finalize() throws CarriageMotionException {
        for (BlockRecord PotentialObstruction : PotentialObstructions) {
            AssertNotObstruction(PotentialObstruction);
        }

        long WorldTime = World.getWorldInfo().getWorldTotalTime();

        try {
            //TreeSet treeSet = World.getPendingTickListEntriesTreeSet();
            TreeSet treeSet = (TreeSet) Reflection.EstablishField(World.getClass(), "pendingTickListEntriesTreeSet").get(World);
            java.util.Iterator PendingBlockUpdateSetIterator = treeSet.iterator();

            while (PendingBlockUpdateSetIterator.hasNext()) {
                net.minecraft.world.NextTickListEntry PendingBlockUpdate = (net.minecraft.world.NextTickListEntry) PendingBlockUpdateSetIterator.next();

                if (Body.contains(new BlockRecord(PendingBlockUpdate.xCoord, PendingBlockUpdate.yCoord, PendingBlockUpdate.zCoord))) {
                    PendingBlockUpdateSetIterator.remove();

                    //Set hashSet = World.pendingTickListEntriesHashSet;
                    Set hashSet = (Set) Reflection.EstablishField(World.getClass(), "pendingTickListEntriesHashSet").get(World);
                    hashSet.remove(PendingBlockUpdate);

                    StorePendingBlockUpdateRecord(PendingBlockUpdate, WorldTime);
                }
            }
        } catch (Throwable VanillaThrowable) {
            try {
                java.util.Set PendingBlockUpdateSet = (java.util.Set) ModInteraction.PendingBlockUpdateSetField.get(World);

                while (true) {
                    net.minecraft.world.NextTickListEntry PendingBlockUpdate = null;

                    for (Object Target : PendingBlockUpdateSet) {
                        net.minecraft.world.NextTickListEntry TargetPendingBlockUpdate = (net.minecraft.world.NextTickListEntry) Target;

                        if (Body.contains(new BlockRecord(TargetPendingBlockUpdate.xCoord, TargetPendingBlockUpdate.yCoord, TargetPendingBlockUpdate.zCoord))) {
                            PendingBlockUpdate = TargetPendingBlockUpdate;

                            break;
                        }
                    }

                    if (PendingBlockUpdate == null) {
                        break;
                    }

                    StorePendingBlockUpdateRecord(PendingBlockUpdate, WorldTime);

                    ModInteraction.RemovePendingBlockUpdate.invoke(World, PendingBlockUpdate);
                }
            } catch (Throwable McpcThrowable) {
                McpcThrowable.printStackTrace();

                VanillaThrowable.printStackTrace();
            }
        }

        if (Configuration.HardmodeActive) {
            for (BlockRecord CarriageRecord : Carriages) {
                int Tier = ((CarriageEntity) CarriageRecord.Entity).Tier;

                double Factor = Carriage.Tiers.values()[Tier].CargoBurdenFactor;

                if (Tier == 0) {
                    for (Directions Direction : Directions.values()) {
                        BlockRecord Position = CarriageRecord.NextInDirection(Direction);

                        ApplyCargoBurdenFactor(Position, Factor);
                    }
                } else {
                    for (int Distance = 1; Distance <= Tier; Distance++, Factor = Math.sqrt(Factor)) {
                        int MinX = CarriageRecord.X - Distance;
                        int MinY = CarriageRecord.Y - Distance;
                        int MinZ = CarriageRecord.Z - Distance;

                        int MaxX = CarriageRecord.X + Distance;
                        int MaxY = CarriageRecord.Y + Distance;
                        int MaxZ = CarriageRecord.Z + Distance;

                        for (int X = MinX; X <= MaxX; X++) {
                            for (int Y = MinY; Y <= MaxY; Y++) {
                                for (int Z = MinZ; Z <= MaxZ; Z++) {
                                    if ((X == MinX) || (X == MaxX) || (Y == MinY) || (Y == MaxY) || (Z == MinZ) || (Z == MaxZ)) {
                                        ApplyCargoBurdenFactor(new BlockRecord(X, Y, Z), Factor);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            for (BlockRecord CargoRecord : Cargo) {
                Double BurdenFactor = CargoBurdenFactors.get(CargoRecord);

                double Burden = GetBaseBurden(CargoRecord);

                if (BurdenFactor == null) {
                    Mass += Burden;
                } else {
                    Mass += Burden * BurdenFactor;
                }
            }
        }

        PotentialObstructions = null;

        Carriages = null;
        Cargo = null;

        CargoBurdenFactors = null;
    }

    public double GetBaseBurden(BlockRecord Record) {
        return (1);
    }
}
