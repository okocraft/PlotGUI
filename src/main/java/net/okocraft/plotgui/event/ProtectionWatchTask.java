package net.okocraft.plotgui.event;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.managers.RemovalStrategy;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.ProtectedRegion.CircularInheritanceException;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

public class ProtectionWatchTask extends BukkitRunnable {

    private final Multimap<World, ProtectedRegion> previousRegions = ArrayListMultimap.create();
    private RegionContainer rc = WorldGuard.getInstance().getPlatform().getRegionContainer();

    @Override
    public void run() {
        for (World world : Bukkit.getWorlds()) {
            com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(world);
            Collection<ProtectedRegion> previousWorldRegions = previousRegions.get(world);

            RegionManager rm = rc.get(weWorld);
            Map<String, ProtectedRegion> regions = rm.getRegions();

            if (previousWorldRegions.isEmpty()) {
                previousWorldRegions.addAll(regions.values());
                continue;
            } else if (regions.values().containsAll(previousWorldRegions)) {
                if (regions.size() <= previousWorldRegions.size()) {
                    continue;
                }
                
                for (ProtectedRegion region : regions.values()) {
                    if (previousWorldRegions.contains(region)) {
                        continue;
                    }
                    
                    ProtectionAddEvent event = new ProtectionAddEvent(region, weWorld);
                    Bukkit.getPluginManager().callEvent(event);
                    
                    if (event.isCancelled()) {
                        rm.removeRegion(region.getId(), RemovalStrategy.UNSET_PARENT_IN_CHILDREN);
                        continue;
                    }
                    
                    previousWorldRegions.add(region);
                }

                continue;
            }

            previousWorldRegions.removeAll(regions.values());
            for (ProtectedRegion region : previousWorldRegions) {
                ProtectedRegion renamed = getRenamed(region, weWorld);
                if (renamed != null) {
                    ProtectionRenameEvent event = new ProtectionRenameEvent(region, renamed, weWorld);
                    Bukkit.getPluginManager().callEvent(event);
                    if (!event.isCancelled()) {
                        continue;
                    }

                    rm.addRegion(region);
                    regions.values().forEach(child -> {
                        try {
                            if (child.getParent() != null && child.getParent().equals(renamed)) {
                                child.setParent(region);
                            }
                        } catch (CircularInheritanceException ignore) {
                        }
                    });

                    rm.removeRegion(renamed.getId(), RemovalStrategy.UNSET_PARENT_IN_CHILDREN);

                } else {
                    ProtectionRemoveEvent event = new ProtectionRemoveEvent(region, weWorld);
                    Bukkit.getPluginManager().callEvent(event);
                    if (!event.isCancelled()) {
                        continue;
                    }

                    if (region.getParent() != null && rm.getRegion(region.getParent().getId()) == null) {
                        rm.addRegion(region.getParent());
                    }

                    if (rm.getRegion(region.getId()) == null) {
                        rm.addRegion(region);
                    }
                }
            }

            previousWorldRegions.clear();
            previousWorldRegions.addAll(rm.getRegions().values());
        }
    }

    private ProtectedRegion getRenamed(ProtectedRegion region, com.sk89q.worldedit.world.World world) {
        Optional<ProtectedRegion> renamed = region.getIntersectingRegions(rc.get(world).getRegions().values()).stream()
                .filter(intersecting -> intersecting.getMaximumPoint().equals(region.getMaximumPoint()))
                .filter(intersecting -> intersecting.getMinimumPoint().equals(region.getMinimumPoint())).findAny();
        if (!renamed.isPresent()) {
            return null;
        }

        ProtectedRegion intersecting = renamed.get();
        if (!intersecting.getFlags().equals(region.getFlags())) {
            return null;
        }

        if (intersecting.isTransient() != region.isTransient()) {
            return null;
        }

        if (intersecting.getParent() != null && !intersecting.getParent().equals(region.getParent())) {
            return null;
        }

        if (!intersecting.getOwners().getUniqueIds().equals(region.getOwners().getUniqueIds())) {
            return null;
        }

        if (!intersecting.getMembers().getUniqueIds().equals(region.getMembers().getUniqueIds())) {
            return null;
        }

        if (intersecting.isDirty() != region.isDirty()) {
            return null;
        }

        return intersecting;
    }
}