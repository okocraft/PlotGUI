package net.okocraft.plotgui.event;

import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

/**
* 定期タスクで追加を監視し、追加されたら発火される。性質上、追加されたそのtickよりも後に発火されることに注意。
* キャンセルすると追加した保護を無理やり削除するため、他のプラグインの実装によっては不具合が生じる可能性がある。
*/
public class ProtectionAddEvent extends ProtectionEvent {

    private ProtectedRegion region;
    private World world;

    ProtectionAddEvent(ProtectedRegion addedRegion, World world) {
        this.region = addedRegion;
        this.world = world;
    }

    public ProtectedRegion getRegion() {
        return region;
    }

    public World getWorld() {
        return world;
    }
}