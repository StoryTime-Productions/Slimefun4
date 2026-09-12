# History

Archived narratives of fully-completed IMPLEMENTATION_PLAN.md phases. Append-only.

---

## Ancient Altar/Pedestal lore (NBT) preservation — done 2026-09-11

Reference spec: `specs/ancient-altar-nbt-preservation.md`

### Bug

Placing an item into an Ancient Pedestal (part of the Ancient Altar multiblock)
and then removing it (block break or right-click pickup) returned an item
that had lost its original lore/NBT.

### Root cause

`AncientPedestal.placeItem()` built the pedestal's displayed item entity via:

```java
ItemStack displayItem = CustomItemStack.create(hand, displayName);
```

This resolves to dough-api's `CustomItemStack.create(ItemStack, String, String...)`
overload. With no lore varargs supplied, Java passes an empty `String[0]` for
`lore`, which becomes an empty `List` inside `ItemStackEditor.setLore`. Decompiling
the vendored `dough-api-cb22e71335.jar` (via `javap`) showed
`ItemStackUtil.editLore`'s lambda unconditionally does:

```java
if (list.isEmpty()) {
    meta.setLore(Collections.emptyList());
    return;
}
```

So any item with lore had it wiped the instant it was placed on a pedestal.
Since `getOriginalItemStack()` (used by both `AncientPedestal.onBreak()` and
`AncientAltarListener.usePedestal()`) just clones the entity's already-stripped
item stack on pickup, the lore never came back. `getOriginalItemStack()`'s own
display-name clone/restore logic was correct and not part of the bug.

### Fix

`src/main/java/io/github/thebusybiscuit/slimefun4/implementation/items/altar/AncientPedestal.java`
— `placeItem()` now builds the display item via `hand.clone()` +
`ItemMeta.setDisplayName()` only (mirroring the pattern already used correctly
in `getOriginalItemStack()`), leaving lore, enchantments, PersistentDataContainer,
and all other meta untouched. Removed the now-unused `CustomItemStack` import.

### Verification

- `mvn compile` clean (JDK 26 required — the shell's default `JAVA_HOME` is JDK 15,
  which fails against the vendored Paper 26.1.2 API; no checkstyle/spotless/PMD
  plugin exists in this pom, so compile is the only static check).
- Added `src/test/java/io/github/thebusybiscuit/slimefun4/implementation/items/altar/TestAncientPedestal.java`,
  a regression test asserting `getOriginalItemStack()` restores lore and display
  name intact after a simulated place → pickup round trip. Passes
  (`mvn test -Dtest=TestAncientPedestal` → 1 run, 0 failures).
- **Known gap**: a full `placeItem()` → block → `getOriginalItemStack()`
  integration test was attempted but blocked by this project's vendored
  MockBukkit version (`mockbukkit-v26.1.2-dev-6d384d3`) throwing
  `UnimplementedOperationException` on `LivingEntity#setRemoveWhenFarAway`,
  called from `ArmorStandUtils.spawnArmorStand()`. The test instead constructs
  an `ItemEntityMock` whose backing stack matches exactly what the fixed
  `placeItem()` produces. Revisit with a true end-to-end test once MockBukkit
  implements `setRemoveWhenFarAway`.

---

## Kitchen Auto Crafter — done 2026-09-12

Reference spec: `specs/kitchen-auto-crafter.md`

Adds a `KitchenAutoCrafter` block to **ExoticGarden** (not this repo — Slimefun4
is only a read-only compile-time dependency) that automates ExoticGarden Kitchen
multiblock recipes: electric, shift-click cycles recipes, deposits output into
a chest below.

### What was done

1. Explored ExoticGarden's package structure; no `machines/` subpackage existed,
   so `KitchenAutoCrafter.java` was created directly under `items/`.
2. Added the `KITCHEN_AUTO_CRAFTER` item constant inline in
   `ExoticGarden.registerItems()`, matching that project's existing pattern.
3. Created `KitchenAutoCrafter.java` extending `SlimefunAutoCrafter` (the spec
   originally called for `AbstractAutoCrafter` directly, but the actual
   `EnhancedAutoCrafter` pattern in the codebase extends `SlimefunAutoCrafter`,
   so implementation followed that instead). Targets
   `ExoticGardenRecipeTypes.KITCHEN`, capacity 512, consumption 48.
4. Registered it in `ExoticGarden.registerItems()` with a Cauldron + Smoker +
   Crafting/Electric Motors recipe. Bumped `pom.xml`'s compiler target from
   21 → 25 to match the Paper API class file version (though the `<compiler>`
   plugin's own explicit `<source>21</source>/<target>21</target>` still
   overrides that — harmless, javac just warns, left as a known loose end for
   a future pass).
5. Build and test: `mvn -o clean compile` and `mvn -o package` (JDK 26 required
   — same class-file-version mismatch as Slimefun4) both succeeded; the shaded
   jar contains `KitchenAutoCrafter.class`. In-game verification (item appears
   in guide, shift-click cycles recipes) was out of scope — no running server
   with a player was exercised for this item; only a clean build was verified.

