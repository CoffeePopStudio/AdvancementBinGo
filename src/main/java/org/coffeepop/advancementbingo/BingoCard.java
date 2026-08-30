package org.coffeepop.advancementbingo;

import io.papermc.paper.advancement.AdvancementDisplay.Frame;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public final class BingoCard {

    public static final int SIZE = 5;
    public static final int CELLS = SIZE * SIZE;

    private static final Random RANDOM = new Random();

    // 公认极难/几乎不可能单局完成的成就：不排除，但权重降到很低
    private static final Set<NamespacedKey> EXTREME_HARD_KEYS = Set.of(
            NamespacedKey.minecraft("nether/all_effects"),            // How Did We Get Here?
            NamespacedKey.minecraft("nether/all_potions"),            // A Furious Cocktail
            NamespacedKey.minecraft("adventure/arbalistic"),          // Arbalistic
            NamespacedKey.minecraft("adventure/very_very_frightening"), // Very, Very Frightening
            NamespacedKey.minecraft("adventure/sniper_duel"),         // Sniper Duel
            NamespacedKey.minecraft("adventure/two_birds_one_arrow"), // Two Birds, One Arrow
            NamespacedKey.minecraft("nether/obtain_ancient_debris"),  // Cover Me in Debris
            NamespacedKey.minecraft("end/levitate"),                  // Great View From Everywhere
            NamespacedKey.minecraft("adventure/kill_all_mobs"),       // Monsters Hunted
            NamespacedKey.minecraft("husbandry/complete_catalogue"),  // A Complete Catalogue
            NamespacedKey.minecraft("husbandry/balanced_diet")       // A Balanced Diet
    );

    private final List<NamespacedKey> advancements;
    private final List<Component> names;
    private final List<ItemStack> icons;

    private BingoCard(List<NamespacedKey> advancements, List<Component> names, List<ItemStack> icons) {
        this.advancements = List.copyOf(advancements);
        this.names = List.copyOf(names);
        this.icons = List.copyOf(icons);
    }

    public static BingoCard createRandom(int easyCount, int mediumCount, int hardCount) {
        List<Candidate> all = new ArrayList<>();
        List<Candidate> easy = new ArrayList<>();
        List<Candidate> medium = new ArrayList<>();
        List<Candidate> hard = new ArrayList<>();

        var iterator = Bukkit.advancementIterator();
        while (iterator.hasNext()) {
            Advancement advancement = iterator.next();
            advancement.getKey();
            if (advancement.getDisplay() == null) {
                continue;
            }
            String path = advancement.getKey().getKey().toLowerCase(Locale.ROOT);
            if (path.startsWith("recipes/")) {
                continue;
            }

            Candidate candidate = new Candidate(advancement, difficulty(advancement));
            all.add(candidate);
            switch (candidate.tier) {
                case EASY -> easy.add(candidate);
                case MEDIUM -> medium.add(candidate);
                case HARD -> hard.add(candidate);
            }
        }

        // 保证数量不超过 25
        int easyTarget = Math.max(0, easyCount);
        int mediumTarget = Math.max(0, mediumCount);
        int hardTarget = Math.max(0, hardCount);
        int overflow = easyTarget + mediumTarget + hardTarget - CELLS;
        if (overflow > 0) {
            int reduceHard = Math.min(hardTarget, overflow);
            hardTarget -= reduceHard;
            overflow -= reduceHard;

            int reduceMedium = Math.min(mediumTarget, overflow);
            mediumTarget -= reduceMedium;
            overflow -= reduceMedium;

            int reduceEasy = Math.min(easyTarget, overflow);
            easyTarget -= reduceEasy;
        }

        List<Advancement> selected = new ArrayList<>();
        Set<NamespacedKey> usedKeys = new HashSet<>();

        addPicked(selected, usedKeys, pickWeighted(easy, easyTarget));
        addPicked(selected, usedKeys, pickWeighted(medium, mediumTarget));
        addPicked(selected, usedKeys, pickWeighted(hard, hardTarget));

        // 如果某一难度池不够，从剩余成就里随机补满
        if (selected.size() < CELLS) {
            List<Advancement> rest = new ArrayList<>();
            for (Candidate candidate : all) {
                if (!usedKeys.contains(candidate.advancement.getKey())) {
                    rest.add(candidate.advancement);
                }
            }
            Collections.shuffle(rest, RANDOM);
            for (Advancement advancement : rest) {
                if (selected.size() >= CELLS) {
                    break;
                }
                selected.add(advancement);
                usedKeys.add(advancement.getKey());
            }
        }

        Collections.shuffle(selected, RANDOM);

        List<NamespacedKey> keys = new ArrayList<>();
        List<Component> names = new ArrayList<>();
        List<ItemStack> icons = new ArrayList<>();

        int count = Math.min(CELLS, selected.size());
        for (int i = 0; i < count; i++) {
            Advancement advancement = selected.get(i);
            keys.add(advancement.getKey());
            names.add(displayName(advancement));
            icons.add(iconOf(advancement));
        }

        // 如果服务器成就少于25个，用空位占位（正常情况下不会发生）
        while (keys.size() < CELLS) {
            keys.add(null);
            names.add(Component.text("???"));
            icons.add(new ItemStack(Material.PAPER));
        }

        return new BingoCard(keys, names, icons);
    }

    private static void addPicked(List<Advancement> target, Set<NamespacedKey> usedKeys, List<Advancement> picked) {
        for (Advancement advancement : picked) {
            if (target.size() >= BingoCard.CELLS) {
                break;
            }
            if (usedKeys.add(advancement.getKey())) {
                target.add(advancement);
            }
        }
    }

    private static List<Advancement> pickWeighted(List<Candidate> pool, int count) {
        List<Candidate> remaining = new ArrayList<>(pool);
        List<Advancement> picked = new ArrayList<>();
        while (picked.size() < count && !remaining.isEmpty()) {
            int totalWeight = 0;
            for (Candidate candidate : remaining) {
                totalWeight += candidate.weight;
            }
            if (totalWeight <= 0) {
                break;
            }
            int roll = RANDOM.nextInt(totalWeight);
            int cumulative = 0;
            Candidate chosen = null;
            for (Candidate candidate : remaining) {
                cumulative += candidate.weight;
                if (roll < cumulative) {
                    chosen = candidate;
                    break;
                }
            }
            if (chosen == null) {
                chosen = remaining.getLast();
            }
            picked.add(chosen.advancement);
            remaining.remove(chosen);
        }
        return picked;
    }

    private static int difficulty(Advancement advancement) {
        int base = 0;
        Frame frame = Objects.requireNonNull(advancement.getDisplay()).frame();
        if (frame == Frame.CHALLENGE) {
            base = 4;
        } else if (frame == Frame.GOAL) {
            base = 2;
        }

        int depth = 0;
        Advancement current = advancement.getParent();
        Set<NamespacedKey> seen = new HashSet<>();
        while (current != null && seen.add(current.getKey())) {
            depth++;
            current = current.getParent();
        }
        return base + depth;
    }

    private static Component displayName(Advancement advancement) {
        if (advancement.getDisplay() != null) {
            return advancement.getDisplay().title();
        }
        return Component.text(advancement.getKey().getKey());
    }

    private static ItemStack iconOf(Advancement advancement) {
        try {
            if (advancement.getDisplay() != null) {
                advancement.getDisplay().icon();
                return advancement.getDisplay().icon().clone();
            }
        } catch (Exception ignored) {
            // fall through
        }
        return new ItemStack(Material.PAPER);
    }

    public int indexOf(NamespacedKey key) {
        if (key == null) {
            return -1;
        }
        for (int i = 0; i < advancements.size(); i++) {
            if (key.equals(advancements.get(i))) {
                return i;
            }
        }
        return -1;
    }

    public NamespacedKey keyAt(int index) {
        return advancements.get(index);
    }

    public Component nameAt(int index) {
        return names.get(index);
    }

    public ItemStack iconAt(int index) {
        return icons.get(index).clone();
    }

    public boolean hasIndex(int index) {
        return index >= 0 && index < CELLS;
    }

    public List<NamespacedKey> keys() {
        return advancements;
    }

    private enum Tier {
        EASY,
        MEDIUM,
        HARD
    }

    private static final class Candidate {
        private final Advancement advancement;
        private final Tier tier;
        private final int weight;

        private Candidate(Advancement advancement, int difficulty) {
            this.advancement = advancement;
            if (difficulty <= 2) {
                this.tier = Tier.EASY;
            } else if (difficulty <= 4) {
                this.tier = Tier.MEDIUM;
            } else {
                this.tier = Tier.HARD;
            }

            int baseWeight = switch (tier) {
                case EASY -> 100;
                case MEDIUM -> 60;
                case HARD -> 30;
            };
            if (EXTREME_HARD_KEYS.contains(advancement.getKey())) {
                baseWeight = 5;
            }
            this.weight = baseWeight;
        }
    }
}
