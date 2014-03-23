package openframes.core;

import java.util.HashMap;

import openframes.BuildInfo;
import cpw.mods.fml.common.ObfuscationReflectionHelper;
import cpw.mods.fml.relauncher.ReflectionHelper;

public abstract class Reflection {
	
	//Thanks AfterLifeLochie
	private static HashMap<String, String> nameRegistry = new HashMap<String, String>();
	private static void registerMapping(String deob, String ob) {
		nameRegistry.put(deob, ob);
	}

	static {
		registerMapping("storageArrays", "field_76652_q");
		registerMapping("pendingTickListEntriesTreeSet", "field_73065_O");
		registerMapping("pendingTickListEntriesHashSet", "field_73064_N");
	}
	//Made things SO MUCH EASIER.
    public static boolean Verbose = true;

    public static Class EstablishClass(String Name) {
        try {
            return (Class.forName(Name));
        } catch (Throwable Throwable) {
            if (Verbose) {
                Throwable.printStackTrace();
            }

            return (null);
        }
    }

    public static java.lang.reflect.Method EstablishMethod(Class Class, String Name, Class... Arguments) {
        try {
        	
    		if (BuildInfo.getBuildNumber() > 0) 
    			Name = nameRegistry.get(Name);
    		
            java.lang.reflect.Method Method = Class.getDeclaredMethod(Name, Arguments);

            Method.setAccessible(true);

            return (Method);
        } catch (Throwable Throwable) {
            if (Verbose) {
                Throwable.printStackTrace();
            }

            return (null);
        }
    }

    public static java.lang.reflect.Field EstablishField(Class Class, String Name) {
        try {
        	
    		if (BuildInfo.getBuildNumber() > 0) 
    			Name = nameRegistry.get(Name);
        	
            ReflectionHelper.findField(Class, Name);
            java.lang.reflect.Field Field = Class.getDeclaredField(Name);

            Field.setAccessible(true);

            return (Field);
        } catch (Throwable Throwable) {
            if (Verbose) {
                Throwable.printStackTrace();
            }

            return (null);
        }
    }
}
