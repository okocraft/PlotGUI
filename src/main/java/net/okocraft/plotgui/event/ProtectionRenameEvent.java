package net.okocraft.plotgui.event;

import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

/**
 * 保護の削除・名称変更は定期タスクで保護すべてを検査してそのタイミングを特定しているから、名称変更されたtickに即座にイベントが発火するとは限らない。
 * 現在のWorldGuardの実装からして、どのプラグインでも名前の変更は確実に別名の保護を作って内容を複製している。
 * なので、ProtectionRenameEventという名前ではあるが、コンストラクタの引数は二つの保護を取る。
 * 名称変更前と変更後はそれぞれの保護の {@link ProtectedRegion#getId()} メソッドを利用して取得する。
 * キャンセルした場合は新しい名前の保護を削除し、古い保護を復活する。
 */
public class ProtectionRenameEvent extends ProtectionEvent {


    private ProtectedRegion fromRegion;
    private ProtectedRegion toRegion;
    private World world;

    ProtectionRenameEvent(ProtectedRegion fromRegion, ProtectedRegion toRegion, World world) {
        this.fromRegion = fromRegion;
        this.toRegion = toRegion;
        this.world = world;
    }
    
    public ProtectedRegion getFromRegion() {
        return fromRegion;
    }

    public ProtectedRegion getToRegion() {
        return toRegion;
    }

    public World getWorld() {
        return world;
    }
}