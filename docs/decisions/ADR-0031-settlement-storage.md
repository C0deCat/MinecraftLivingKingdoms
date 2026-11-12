---
status: stable
last_reviewed: 2026-11-12
---

# ADR-0031: The stock of a settlement lives in chests

**Status:** Accepted · **Date:** 2026-11-12

## Context

Slice S1 of [M4](../milestones/m4-settlements.md) needs a shared storage, and every later slice builds on it: production chains (S5, S3), the builder and its materials (S2), the growth of a village (S6), trade and theft (S7). [M5](../milestones/m5-simulation.md) computes settlements whose chunks are not loaded, so it must know what a settlement has without reading blocks.

[ADR-0030](ADR-0030-resident-integration.md) made our save data the source of truth for what our mod owns about a resident. For the stock, the user chose the other way on 2026-11-12: it is physical, in chests, as in Millénaire, so that a resident who produces something works at its own building and walks to the warehouse only when it must.

## Decision

1. **The stock is the content of chests in the world.** A settlement has storage points, each a container block with a role:
   - a **workplace store** belongs to a production building or a workplace (a field, a smithy, a sawmill, a winery) and holds what that production needs and makes;
   - a **warehouse** only stores.

2. **Own building first, warehouse for the rest.** A resident puts what it makes into the store of its workplace; what exceeds the amount that workplace keeps goes to a warehouse. It takes inputs from its workplace store first and from a warehouse only when they are missing there.

3. **A table of balances beside the chests.** The settlement record keeps, per storage point, how much of each item is in it. It is the current balance, not a log of transactions, so it does not grow with time.
   - While the settlement is loaded, the chests are the truth; a resident that puts in or takes out also changes the balance.
   - When the settlement loads, the balances are compared with the chests once and corrected, because a player or a hopper may have changed them.
   - While it is unloaded, the simulation of M5 works on the balances.
   - When it loads again, the difference is put into the chests, so their content matches the balances.

4. **Growth by capacity.** When the free space of the warehouses of a settlement falls below a reserve, the settlement builds another warehouse.

5. **Adopted villages** (D-53) have neither warehouses nor production buildings, and the chests in their houses are loot for the player. When a village is adopted, our mod places a warehouse of a few chests at its bell and a workplace store next to the workplace of each worker. Our own templates replace this in S2.

Where each part is built:

| Part | Slice |
| --- | --- |
| Storage points with roles, rule 2 for the farmer and the lumberjack, the table of balances, the placed chests of adopted villages | S1 |
| Chests with roles inside our building templates; "the warehouse is almost full, build another" as the first rule of the development plan | S2 |
| Production chains through the workplace stores of their buildings | S5, S3 |
| Capacity as a driver of the whole building tree | S6 |
| A player taking from the chests of a village: trade or theft | S7 |
| Balances changed by the simulation and put back into chests on load | M5 |

## Options considered

- **The stock in the settlement record, opened through a storage block of ours** — always readable, one owner, nothing to reconcile. Not chosen: the stock would be less physical, residents would have no reason to walk between their building and the warehouse, and a player could not see or touch what a village has except through our block.
- **Chests only, without balances** — not chosen: M5 could not compute an unloaded settlement, and a settlement would have to scan its chests to know its needs.
- **A log of transactions** — not chosen: it grows without bound and answers a question nobody asks; what the simulation needs is the current amount. The history of a settlement belongs to the event log of Core.

## Consequences

- The stock has two representations, and the rules of point 3 decide which one is the truth at each moment. Reconciliation on load is code that must be tested with a player changing chests in between.
- This differs from ADR-0030 on purpose: a resident record is the truth for a resident, chests are the truth for the stock while loaded.
- Every task that moves items goes through one storage service, never straight into a container, so the balances stay right.
- Step S1-4 of the [track plan](../exec-plans/active/2026-11-10-m4-settlements.md) changes from a storage block of ours to chests with roles.
