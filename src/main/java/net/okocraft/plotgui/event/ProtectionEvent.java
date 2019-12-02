package net.okocraft.plotgui.event;

import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

abstract class ProtectionEvent extends Event implements Cancellable {

    private static HandlerList handlers = new HandlerList();
    private boolean cancel = false;

    ProtectionEvent() {
        super(!Bukkit.isPrimaryThread());
    }

    @Override
    public boolean isCancelled() {
        return cancel;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancel = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return getHandlerList();
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}