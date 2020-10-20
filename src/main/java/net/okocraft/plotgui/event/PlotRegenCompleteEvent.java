package net.okocraft.plotgui.event;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import lombok.Getter;
import net.okocraft.plotgui.Plot;

public class PlotRegenCompleteEvent extends Event {

    @Getter
    private static HandlerList handlerList = new HandlerList();

    @Getter
    private final Plot regeneratedPlot;

    @Getter
    private final long elapsedTime;

    @Getter
    private final CommandSender executor;

    @Getter
    private final UUID previousOwner;

    public PlotRegenCompleteEvent(Plot regeneratedPlot, long elapsedTime, CommandSender executor, UUID previousOwner) {
        super(!Bukkit.isPrimaryThread());
        this.regeneratedPlot = regeneratedPlot;
        this.elapsedTime = elapsedTime;
        this.executor = executor;
        this.previousOwner = previousOwner;
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }

}
