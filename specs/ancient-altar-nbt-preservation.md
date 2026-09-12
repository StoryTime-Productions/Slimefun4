# Spec: Ancient Altar/Pedestal loses item Lore (NBT) on place/remove round-trip

## Symptom (reported)

Placing an item on an `AncientPedestal` (part of the `AncientAltar` multiblock)
and then removing it (via block break or right-click pickup) returns an item
that is missing its original custom NBT data.

## Location

- `src/main/java/io/github/thebusybiscuit/slimefun4/implementation/items/altar/AncientPedestal.java`
  - `placeItem(Player, Block)` — spawns the item entity that sits on the pedestal.
  - `getOriginalItemStack(Item)` — reconstructs the original stack when the item
    is picked back up (used by both `onBreak()` here and `usePedestal()` in the listener).
- `src/main/java/io/github/thebusybiscuit/slimefun4/implementation/listeners/AncientAltarListener.java`
  - `usePedestal(Block, Player)` — right-click pickup path, calls `getOriginalItemStack`.
  - `startRitual(...)` — reads pedestal contents via `getOriginalItemStack` as recipe input.

## Root cause (confirmed via bytecode inspection of vendored `dough-api-cb22e71335.jar`)

`placeItem()` builds the entity's displayed stack with:

```java
ItemStack displayItem = CustomItemStack.create(hand, displayName);
```

This resolves to the dough-api overload
`CustomItemStack.create(ItemStack item, String name, String... lore)`.
Because no lore varargs are passed, Java supplies an **empty `String[0]`** array
for `lore`. Inside `ItemStackEditor.setLore(String...)` this becomes
`Arrays.asList(new String[0])` → an empty `List`, which is forwarded to
`ItemStackUtil.editLore(List)`.

Decompiled `editLore`'s lambda:

```java
if (list.isEmpty()) {
    meta.setLore(Collections.emptyList());   // <-- unconditionally wipes lore
    return;
}
```

So **any item with existing lore has its lore forcibly cleared** the moment it
is placed on a pedestal — not just left alone. Since the pedestal entity's
item stack (with lore now empty) is exactly what `getOriginalItemStack()`
clones and returns on pickup, the lore never comes back. Lore is stored in
item NBT (`display.Lore`), so from the player's perspective this reads as
"the item lost its NBT data" — this is especially visible for Slimefun items,
which almost universally carry a multi-line lore description of their
stats/tier.

Everything else `getOriginalItemStack()` does (clone, clear/restore display
name) is correct and NOT part of the bug — enchantments, PersistentDataContainer,
custom model data, attribute modifiers, etc. all survive a plain `clone()`
fine. Only lore is destroyed, and only because of the accidental empty-varargs
call into a lore-wiping helper.

## Fix

In `AncientPedestal.placeItem()`, stop routing the display-name change through
`CustomItemStack.create(ItemStack, String, String...)` (which mutates lore as
a side effect). Instead, clone `hand` and set only the display name via
`ItemMeta`, exactly like `getOriginalItemStack()` already does correctly for
the reverse direction. This preserves lore (and everything else) untouched.

## Non-goals

- Not touching `getOriginalItemStack()`'s display-name restoration logic — it's correct.
- Not touching `AncientAltarListener` — it doesn't do the stripping itself, it just
  consumes the already-broken stack produced by `placeItem()`.
- Not adding new tests infra beyond what the existing test suite supports; a
  targeted unit/regression test should assert lore survives place→remove.

## Verification plan

1. Unit/regression test (if a JUnit harness for `AncientPedestal` exists or is
   feasible with MockBukkit) that places an item with lore + display name onto
   a pedestal and asserts the picked-up item's lore matches the original.
2. `mvn package` / relevant test module passes.
3. Manual reasoning check: display name restoration logic in
   `getOriginalItemStack()` is unaffected by the fix.
