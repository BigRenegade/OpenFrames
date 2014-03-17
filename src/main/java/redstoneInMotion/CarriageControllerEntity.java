package redstoneInMotion;

import java.util.HashMap;
import java.util.Map;

import cpw.mods.fml.common.Optional;

import dan200.computer.api.IComputerAccess;
import dan200.computer.api.ILuaContext;
import dan200.computer.api.IPeripheral;

import li.cil.oc.api.network.Arguments;
import li.cil.oc.api.network.Context;
import li.cil.oc.api.network.ManagedPeripheral;
import li.cil.oc.api.network.SimpleComponent;


@Optional.InterfaceList({
	@Optional.Interface(iface = "li.cil.oc.api.network.SimpleComponent", modid = "OpenComputers"),
	@Optional.Interface(iface = "li.cil.oc.api.network.ManagedPeripheral", modid = "OpenComputers"),
	@Optional.Interface(iface = "dan200.computer.api.IPeripheral", modid = "ComputerCraft")
})
public class CarriageControllerEntity extends CarriageDriveEntity implements IPeripheral, SimpleComponent, ManagedPeripheral 
{

	public enum ComputerMethod
	{
		move,
		anchored_move,
		check_anchored_move,
		unanchored_move,
		check_unanchored_move;
	}

	public static final int numMethods = ComputerMethod.values().length;
	
	public static final String[] methodNames = new String[numMethods];	{
		for (int Index = 0; Index < numMethods; Index ++)
		{
			methodNames [Index] = ComputerMethod.values()[Index].name ();
		}
	}

	public static final Map<String, Integer> methodIds = new HashMap<String, Integer>();
	static {
		for (int i = 0; i < numMethods; ++i) {
			methodIds.put(methodNames[i], i);
		}
	}

	public Object ThreadLockObject = new Object();
	public boolean Simulating;
	public Directions MotionDirection;
	public CarriageMotionException Error;
	public boolean Obstructed;
	public int ObstructionX;
	public int ObstructionY;
	public int ObstructionZ;

	@Override
	public void HandleToolUsage (int Side, boolean Sneaking)
	{
	}

	@Override
	public synchronized void updateEntity()
	{
		if (worldObj.isRemote)
		{
			return;
		}

		if (Stale)
		{
			HandleNeighbourBlockChange();
		}

		if (MotionDirection == null)
		{
			return;
		}

		try
		{
			Move();
		}
		catch (CarriageMotionException Error)
		{
			this.Error = Error;

			if (Error instanceof CarriageObstructionException)
			{
				Obstructed = true;

				ObstructionX = ((CarriageObstructionException) Error).X;
				ObstructionY = ((CarriageObstructionException) Error).Y;
				ObstructionZ = ((CarriageObstructionException) Error).Z;
			}
		}
		MotionDirection = null;
		notify ();
	}

	public boolean Anchored;

	@Override
	public boolean Anchored()
	{
		return (Anchored);
	}



	public boolean ParseBooleanArgument (Object Argument, String Label) throws Exception
	{
		try
		{
			return ((Boolean) Argument);
		}
		catch (Throwable Throwable)
		{
			throw (new Exception ("invalid " + Label + " flag"));
		}
	}

	public Directions ParseDirectionArgument (Object Argument) throws Exception
	{
		if (Argument instanceof Double)
		{
			try
			{
				return (Directions.values () [ (int) Math.round ((Double) Argument) ]);
			}
			catch (Throwable Throwable)
			{
				throw (new Exception ("direction index out of range"));
			}
		}

		try
		{
			String Direction = (String) Argument;

			if (Direction.equalsIgnoreCase ("down") || Direction.equalsIgnoreCase ("negy"))
			{
				return (Directions.NegY);
			}

			if (Direction.equalsIgnoreCase ("up") || Direction.equalsIgnoreCase ("posy"))
			{
				return (Directions. PosY);
			}

			if (Direction.equalsIgnoreCase ("north") || Direction.equalsIgnoreCase ("negz"))
			{
				return (Directions.NegZ);
			}

			if (Direction.equalsIgnoreCase ("south") || Direction.equalsIgnoreCase ("posz"))
			{
				return (Directions.PosZ);
			}

			if (Direction.equalsIgnoreCase ("west") || Direction.equalsIgnoreCase ("negx"))
			{
				return (Directions.NegX);
			}

			if (Direction.equalsIgnoreCase ("east") || Direction.equalsIgnoreCase ("posx"))
			{
				return (Directions.PosX);
			}
		}
		catch (Throwable Throwable)
		{
		}

		throw (new Exception ("invalid direction"));
	}

	public void SetupMotion (Directions MotionDirection, boolean Simulating, boolean Anchored)
	{
		this.MotionDirection = MotionDirection;

		this.Simulating = Simulating;

		this.Anchored = Anchored;
	}


	public synchronized Object[] callMethod (int MethodIndex, Object[] Arguments) throws Exception
	{
		try
		{
			switch (ComputerMethod.values () [ MethodIndex ])
			{
				case move :

					if (Arguments.length !=  3) {
						throw (new Exception ("usage:  move('direction', true/false, true/false)"));
					}

					SetupMotion (ParseDirectionArgument(Arguments[0]), ParseBooleanArgument(Arguments[1], "simulation"), ParseBooleanArgument(Arguments[2], "anchoring"));

					break;

				case anchored_move :

					if (Arguments.length !=  1) {
						throw (new Exception ("usage:  anchored_move('direction')"));
					}

					SetupMotion (ParseDirectionArgument(Arguments[0]), false, true);

					break;

				case check_anchored_move :

					if (Arguments.length !=  1) {
						throw (new Exception ("usage:  check_anchored_move('direction')"));
					}

					SetupMotion (ParseDirectionArgument(Arguments[0]), true, true);

					break;

				case unanchored_move :

					if (Arguments.length !=  1) {
						throw (new Exception ("usage:  unanchored_move('direction')"));
					}

					SetupMotion (ParseDirectionArgument(Arguments[0]), false, false);

					break;

				case check_unanchored_move :

					if (Arguments.length !=  1) {
						throw (new Exception ("usage:  check_unanchored_move('direction')"));
					}

					SetupMotion (ParseDirectionArgument(Arguments[0]), true, false);

					break;
			}
		}
		catch (Throwable Throwable)
		{
			throw (new Exception ("no such command"));
		}

		Error = null;

		Obstructed = false;

		try
		{
			do
			{
				wait ();
			}
			while (MotionDirection != null);
		}
		catch (Throwable Throwable)
		{
			throw (new Exception (Throwable));
		}

		if (Error == null)
		{
			return (new Object[] { true });
		}

		if (Obstructed == false)
		{
			return (new Object[] { false, Error.getMessage () });
		}

		return (new Object[] { false, Error.getMessage (), ObstructionX, ObstructionY, ObstructionZ });
	}

	public void Move () throws CarriageMotionException
	{
		if (Active)
		{
			throw (new CarriageMotionException ("controller already active"));
		}

		if (CarriageDirection == null)
		{
			throw (new CarriageMotionException ("no carriage or too many carriages attached to controller"));
		}

		CarriagePackage Package = PreparePackage (MotionDirection);

		if (Simulating)
		{
			return;
		}

		InitiateMotion (Package);
	}

	@Override
	public CarriagePackage GeneratePackage (CarriageEntity Carriage, Directions CarriageDirection, Directions MotionDirection) throws CarriageMotionException
	{
		CarriagePackage Package;

		if (Anchored)
		{
			if (MotionDirection == CarriageDirection)
			{
				throw (new CarriageMotionException ("cannot push carriage away from controller in anchored mode"));
			}

			if (MotionDirection == CarriageDirection.Opposite ())
			{
				throw (new CarriageMotionException ("cannot pull carriage into controller in anchored mode"));
			}

			Package = new CarriagePackage (this, Carriage, MotionDirection);

			Carriage.FillPackage (Package);

			if (Package.Body.contains (Package.DriveRecord))
			{
				throw (new CarriageMotionException ("carriage is attempting to move controller while in anchored mode"));
			}

			if (Package.Body.contains (Package.DriveRecord.NextInDirection (MotionDirection.Opposite ())))
			{
				throw (new CarriageMotionException ("carriage is obstructed by controller while in anchored mode"));
			}
		}
		else
		{
			Package = new CarriagePackage (this, Carriage, MotionDirection);

			Package.AddBlock (Package.DriveRecord);

			if (MotionDirection != CarriageDirection)
			{
				Package.AddPotentialObstruction (Package.DriveRecord.NextInDirection (MotionDirection));
			}

			Carriage.FillPackage (Package);
		}

		Package.Finalize ();

		return (Package);
	}

	// ComputerCraft
	
	@Optional.Method(modid = "ComputerCraft")
	@Override
	public String getType()
	{
		return ("RIM_CarriageController") ;
	}

	
	@Optional.Method(modid = "ComputerCraft")
	@Override
	public boolean canAttachToSide(int Side)
	{
		return ( true ) ;
	}

	@Optional.Method(modid = "ComputerCraft")
	@Override
	public void attach(IComputerAccess Computer)
	{
	}

	@Optional.Method(modid = "ComputerCraft")
	@Override
	public void detach(IComputerAccess Computer)
	{
	}

	@Optional.Method(modid = "ComputerCraft")
	@Override
	public String[] getMethodNames() {
		return methodNames;
	}
	
	@Optional.Method(modid = "ComputerCraft")
	@Override
	public Object[] callMethod (IComputerAccess Computer, ILuaContext LuaContext, 
			int MethodIndex, Object[] Arguments) throws Exception
	{
		return (callMethod (MethodIndex, Arguments));
	}
	


	// OpenComputers
	

	@Override
	@Optional.Method(modid = "OpenComputers")
	public String getComponentName() {
		// Convention for OC names is a) lower case, b) valid variable names,
		// so this can be used as `component.br_reactor.setActive(true)` e.g.
		return "RIM_Carriage_Controller";
	}

	@Override
	@Optional.Method(modid = "OpenComputers")
	public String[] methods() {
		return methodNames;
	}
	
	@Override
	@Optional.Method(modid = "OpenComputers")
	public Object[] invoke(final String method, final Context context,
						   final Arguments args) throws Exception {
		final Object[] arguments = new Object[args.count()];
		for (int i = 0; i < args.count(); ++i) {
			arguments[i] = args.checkAny(i);
		}
		final Integer methodId = methodIds.get(method);
		if (methodId == null) {
			throw new NoSuchMethodError();
		}
		return callMethod(methodId, arguments);
	}

}