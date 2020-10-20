package net.okocraft.plotgui;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StringFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;

class PlotFlag {

    private static final String FLAG_NAME = "plotdata";
    private static StringFlag plotFlag;

    private PlotFlag() {
    }

    /**
     * Use this inside the method JavaPlugin#onLoad()
     */
    public static void register() throws FlagConflictException {
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();

        Flag<?> existing = registry.get(FLAG_NAME);
        if (existing != null) {
            if (existing instanceof StringFlag) {
                plotFlag = (StringFlag) existing;
            } else {
                throw new FlagConflictException("Some plugin registered same name but different type flag. The type is: " + existing.getClass().getName());
            }
        } else {
            plotFlag = new StringFlag(FLAG_NAME);
            registry.register(plotFlag);
        }
    }

    public static StringFlag get() throws IllegalStateException {
        if (plotFlag == null) {
            throw new IllegalStateException("The flag is not registered yet.");
        }
        return plotFlag;
    }
}
