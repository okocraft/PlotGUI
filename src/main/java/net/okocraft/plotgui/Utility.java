package net.okocraft.plotgui;

import java.util.List;
import java.util.Set;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;

public class Utility {

    private Utility() {
    }

    public static boolean isSign(Block sign) {
        return sign.getState() instanceof Sign;
    }

    public static boolean isWallSign(Block sign) {
        if (!isSign(sign)) {
            return false;
        }

        return sign.getBlockData() instanceof Directional;
    }

    public static boolean isStandingSign(Block sign) {
        return isSign(sign) && !isWallSign(sign);
    }

    public static BlockFace getSignFace(Block sign) {
        if (!Utility.isSign(sign)) {
            throw new IllegalArgumentException("This block is not sign.");
        }

        if (Utility.isWallSign(sign)) {
            return ((Directional) sign.getBlockData()).getFacing();
        } else {
            return ((Rotatable) sign.getBlockData()).getRotation();
        }
    }

    public static Block getSignOn(Block block) {
        Block checking = block.getRelative(BlockFace.UP);
        if (isStandingSign(checking)) {
            return checking;
        }

        for (BlockFace face : Set.of(BlockFace.NORTH, BlockFace.EAST, BlockFace.WEST, BlockFace.SOUTH)) {
            checking = block.getRelative(face);
            if (isWallSign(checking) && checking.getRelative(getSignFace(checking).getOppositeFace()).equals(block)) {
                return checking;
            }
        }

        return null;
    }

    public static boolean isPlotGUISign(Block sign) {
        return ((Sign) sign.getState()).getLine(0).equals("[PlotGUI]");
    }

    public static ProtectedRegion getRegionAtOrBihind(Block sign) {
        ProtectedRegion region = getRegion(sign);
        if (region == null) {
            BlockFace back = Utility.getSignFace(sign).getOppositeFace();
            region = getRegion(sign.getRelative(back));
        }

        return region;
    }

    public static ProtectedRegion getRegion(Block block) {
        Location location = block.getLocation();
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

    public static ProtectedRegion getRegion(World world, String name) {
        return WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world))
                .getRegion(name);
    }
}