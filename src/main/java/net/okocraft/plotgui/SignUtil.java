package net.okocraft.plotgui;

import java.util.EnumSet;
import java.util.List;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.data.type.WallSign;
import org.jetbrains.annotations.Contract;

public final class SignUtil {

    private SignUtil() {
    }

    private static ProtectedRegion getRegionAt(Location location) {
        if (location.getWorld() == null) {
            return null;
        }
        BlockVector3 pos = BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        ProtectedRegion region = new ProtectedCuboidRegion("__PLOTGUI_CHECKER__", pos, pos);

        RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer()
                .get(BukkitAdapter.adapt(location.getWorld()));
        if (regionManager == null) {
            return null;
        }
        List<ProtectedRegion> intersecting = region.getIntersectingRegions(regionManager.getRegions().values());
        if (intersecting.size() != 1) {
            return null;
        }

        return intersecting.get(0);
    }

    public static BlockFace getFacing(Sign sign) {
        if (sign.getBlockData() instanceof WallSign) {
            return ((Directional) sign.getBlockData()).getFacing();
        } else {
            return ((Rotatable) sign.getBlockData()).getRotation();
        }
    }

    public static ProtectedRegion getRegionAtOrBehind(Sign sign) {
        ProtectedRegion region = getRegionAt(sign.getLocation());
        if (region != null) {
            return region;
        }
        BlockFace back = getFacing(sign).getOppositeFace();
        return getRegionAt(sign.getBlock().getRelative(back).getLocation());
    }

    public static Sign getSignFrom(Block block) {
        if (block != null && isSign(block)) {
            return (Sign) block.getState();
        }

        return null;
    }

    public static boolean isSign(Block sign) {
        return sign == null ? false : sign.getState() instanceof Sign;
    }

    public static boolean isWallSign(Sign sign) {
        return sign == null ? false : sign.getBlockData() instanceof Directional;
    }

    public static BlockFace getSignFace(Sign sign) {
        if (isWallSign(sign)) {
            return ((Directional) sign.getBlockData()).getFacing();
        } else {
            return ((Rotatable) sign.getBlockData()).getRotation();
        }
    }

    @Contract("null -> null")
    public static Sign getSignOn(Block block) {
        if (block == null) {
            return null;
        }
        Sign result = getSignFrom(block.getRelative(BlockFace.UP));
        if (result != null) {
            return result;
        }

        for (BlockFace face : EnumSet.of(BlockFace.NORTH, BlockFace.EAST, BlockFace.WEST, BlockFace.SOUTH)) {
            result = getSignFrom(block.getRelative(face));
            if (result != null && isWallSign(result) && getFacing(result) == face) {
                return result;
            }
        }

        return null;
    }

    public static boolean isPlotGUISign(Sign sign) {
        return sign == null ? false : sign.getLine(0).equals("[PlotGUI]");
    }
}