package redstoneInMotion ;

public class BlockRecord implements Comparable < BlockRecord >
{
	public int X ;
	public int Y ;
	public int Z ;

	@Override
	public String toString ( )
	{
		return ( "(" + X + "," + Y + "," + Z + ")" ) ;
	}

	public BlockRecord ( int X , int Y , int Z )
	{
		this . X = X ;
		this . Y = Y ;
		this . Z = Z ;
	}

	public BlockRecord ( BlockRecord Record )
	{
		X = Record . X ;
		Y = Record . Y ;
		Z = Record . Z ;
	}

	public void Shift ( Directions Direction )
	{
		X += Direction . DeltaX ;
		Y += Direction . DeltaY ;
		Z += Direction . DeltaZ ;
	}

	public BlockRecord NextInDirection ( Directions Direction )
	{
		return ( new BlockRecord ( X + Direction . DeltaX , Y + Direction . DeltaY , Z + Direction . DeltaZ ) ) ;
	}

	@Override
	public int compareTo ( BlockRecord Target )
	{
		int Result = X - Target . X ;

		if ( Result == 0 )
		{
			Result = Y - Target . Y ;

			if ( Result == 0 )
			{
				Result = Z - Target . Z ;
			}
		}

		return ( Result ) ;
	}

	public int Id ;

	public int Meta ;

	public net . minecraft . tileentity . TileEntity Entity ;

	public net . minecraft . nbt . NBTTagCompound EntityRecord ;

	public void Identify ( net . minecraft . world . World World )
	{
		Id = World . getBlockId ( X , Y , Z ) ;

		Meta = World . getBlockMetadata ( X , Y , Z ) ;

		Entity = World . getBlockTileEntity ( X , Y , Z ) ;
	}

	public static BlockRecord Identified ( TileEntity Anchor , int X , int Y , int Z )
	{
		BlockRecord Record = new BlockRecord ( X , Y , Z ) ;

		Record . Identify ( Anchor . worldObj ) ;

		return ( Record ) ;
	}
}
