// Inventory and container helpers. Slots are menu indexes:
//   player inventory (minecraft:inventory): 0 craft result, 1–4 craft grid, 5–8 armor, 9–35 main, 36–44 hotbar, 45 offhand;
//   crafting table (minecraft:crafting): 0 result, 1–9 grid, 10–36 main, 37–45 hotbar;
//   single chest (minecraft:generic_9x3): 0–26 chest, 27–53 main, 54–62 hotbar.
import type { GameClient } from "../client.ts";
import type { ClickType, ContainerState } from "../protocol.ts";
import { waitFor } from "./wait.ts";

/** How many of an item the player holds, from the server. */
export async function inventoryCount(mc: GameClient, item: string): Promise<number> {
  const { slots } = await mc.call("query.inventory", {});
  return slots.filter((s) => s.item === item).reduce((sum, s) => sum + s.count, 0);
}

export function readContainer(mc: GameClient): Promise<ContainerState> {
  return mc.call("container.read", {});
}

/** Waits until a menu of the given type is open on the client and returns it. */
export function waitForContainer(mc: GameClient, type: string, ticks = 20): Promise<ContainerState> {
  return waitFor(mc, async () => {
    const menu = await readContainer(mc);
    return menu.type === type ? menu : undefined;
  }, { ticks, what: `a ${type} menu to open` });
}

/** One click on a menu slot; button 0 is left, 1 is right. */
export async function clickSlot(mc: GameClient, slot: number, options: { button?: number; type?: ClickType } = {}): Promise<void> {
  await mc.call("container.click", { slot, button: options.button ?? 0, type: options.type ?? "pickup" });
}

export async function closeContainer(mc: GameClient): Promise<void> {
  await mc.call("container.close", {});
}
