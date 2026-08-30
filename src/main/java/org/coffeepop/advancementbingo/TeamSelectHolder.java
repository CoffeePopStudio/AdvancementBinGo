package org.coffeepop.advancementbingo;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class TeamSelectHolder implements InventoryHolder {

    private final Player player;
    private Inventory inventory;

    public TeamSelectHolder(Player player) {
        this.player = player;
    }

    public Player player() {
        return player;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
