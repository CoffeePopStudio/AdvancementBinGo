package org.coffeepop.advancementbingo;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class BingoCardHolder implements InventoryHolder {

    private final BingoGame game;
    private final Player viewer;
    private Inventory inventory;

    public BingoCardHolder(BingoGame game, Player viewer) {
        this.game = game;
        this.viewer = viewer;
    }

    public BingoGame game() {
        return game;
    }

    public Player viewer() {
        return viewer;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
