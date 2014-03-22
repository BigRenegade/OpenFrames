package openframes.blocks;

import openframes.carriage.Carriage;
import openframes.carriage.CarriageDrive;

public abstract class Blocks {
    public static Carriage Carriage;

    public static CarriageDrive CarriageDrive;

    public static Spectre Spectre;

    public static void Initialize() {
        Carriage = new Carriage();

        CarriageDrive = new CarriageDrive();

        Spectre = new Spectre();
    }
}
