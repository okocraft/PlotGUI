package net.okocraft.plotgui.event;

import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

/**
 * 定期タスクで削除を監視し、削除されたら発火される。性質上、削除されたそのtickよりも後に発火されることに注意。
 * キャンセルすると削除した保護を再び追加するが、他のプラグインの実装によっては不具合が生じる可能性がある。
 */
public class ProtectionRemoveEvent extends ProtectionEvent {

    private ProtectedRegion region;
    private World world;

    ProtectionRemoveEvent(ProtectedRegion removedRegion, World world) {
        this.region = removedRegion;
        this.world = world;
    }

    public ProtectedRegion getRegion() {
        return region;
    }

    public World getFromWorld() {
        return world;
    }
}