package openframes.carriage;

import openframes.entity.CarriageDriveEntity;
import openframes.entity.CarriageEntity;
import openframes.util.Directions;

public class CarriageMotorEntity extends CarriageDriveEntity {
    @Override
    public CarriagePackage GeneratePackage(CarriageEntity Carriage, Directions CarriageDirection, Directions MotionDirection) throws CarriageMotionException {
        if (MotionDirection == CarriageDirection) {
            throw (new CarriageMotionException("motor cannot push carriage away from itself"));
        }

        if (MotionDirection == CarriageDirection.Opposite()) {
            throw (new CarriageMotionException("motor cannot pull carriage into itself"));
        }

        CarriagePackage Package = new CarriagePackage(this, Carriage, MotionDirection);

        Carriage.FillPackage(Package);

        if (Package.Body.contains(Package.DriveRecord)) {
            throw (new CarriageMotionException("carriage is attempting to move motor"));
        }

        if (Package.Body.contains(Package.DriveRecord.NextInDirection(MotionDirection.Opposite()))) {
            throw (new CarriageObstructionException("carriage motion is obstructed by motor", xCoord, yCoord, zCoord));
        }

        Package.Finalize();

        return (Package);
    }

    @Override
    public boolean Anchored() {
        return (true);
    }
}
