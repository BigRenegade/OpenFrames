package openframes.entity;

import openframes.blocks.BlockRecord;
import openframes.blocks.BlockRecordSet;
import openframes.carriage.CarriageMotionException;
import openframes.carriage.CarriagePackage;
import openframes.core.Configuration;
import openframes.util.Directions;

public class PlatformCarriageEntity extends CarriageEntity {
    public void FailBecauseOverburdened() throws CarriageMotionException {
        throw (new CarriageMotionException("platform carriage exceeds maximum burden of " + Configuration.Carriage.MaxPlatformBurden + " blocks carried"));
    }

    @Override
    public void FillPackage(CarriagePackage Package) throws CarriageMotionException {
        BlockRecordSet CarriagesToCheck = new BlockRecordSet();

        BlockRecordSet BlocksToCheck = new BlockRecordSet();

        BlockRecordSet BlocksChecked = new BlockRecordSet();

        BlocksChecked.add(Package.DriveRecord);

        BlocksChecked.add(Package.AnchorRecord);

        Package.AddBlock(Package.AnchorRecord);

        CarriagesToCheck.add(Package.AnchorRecord);

        int BlocksCarried = 0;

        while (CarriagesToCheck.size() > 0) {
            BlockRecord CarriageRecord = CarriagesToCheck.pollFirst();

            for (Directions TargetDirection : Directions.values()) {
                BlockRecord TargetRecord = CarriageRecord.NextInDirection(TargetDirection);

                if (((PlatformCarriageEntity) CarriageRecord.Entity).SideClosed[TargetDirection.ordinal()]) {
                    if (TargetDirection == Package.MotionDirection) {
                        Package.AddPotentialObstruction(TargetRecord);
                    }

                    continue;
                }

                if (!BlocksChecked.add(TargetRecord)) {
                    continue;
                }

                if (worldObj.isAirBlock(TargetRecord.X, TargetRecord.Y, TargetRecord.Z)) {
                    continue;
                }

                TargetRecord.Identify(worldObj);

                Package.AddBlock(TargetRecord);

                if (Package.MatchesCarriageType(TargetRecord)) {
                    CarriagesToCheck.add(TargetRecord);

                    continue;
                }

                BlocksToCheck.add(TargetRecord);

                BlocksCarried++;

                if (BlocksCarried > Configuration.Carriage.MaxPlatformBurden) {
                    FailBecauseOverburdened();
                }
            }
        }

        while (BlocksToCheck.size() > 0) {
            BlockRecord BlockRecord = BlocksToCheck.pollFirst();

            for (Directions TargetDirection : Directions.values()) {
                BlockRecord TargetRecord = BlockRecord.NextInDirection(TargetDirection);

                if (!BlocksChecked.add(TargetRecord)) {
                    continue;
                }

                if (worldObj.isAirBlock(TargetRecord.X, TargetRecord.Y, TargetRecord.Z)) {
                    continue;
                }

                TargetRecord.Identify(worldObj);

                Package.AddBlock(TargetRecord);

                BlocksToCheck.add(TargetRecord);

                BlocksCarried++;

                if (BlocksCarried > Configuration.Carriage.MaxPlatformBurden) {
                    FailBecauseOverburdened();
                }
            }
        }
    }
}
