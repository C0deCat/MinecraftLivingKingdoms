package livingkingdoms.settlements.resident;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Supplier;
import livingkingdoms.core.CoreSaveData;
import livingkingdoms.core.StableId;
import livingkingdoms.settlements.LivingKingdomsSettlements;
import livingkingdoms.settlements.work.HarvestOrder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * The residents of a world: who is one, and what our mod has recorded about them. A resident is a
 * {@link ResidentRecord}; its body carries only the record's ID as a data attachment (ADR-0030).
 *
 * <p>Server thread only. After a change that affects behaviour the caller rebuilds the body's
 * brain through the MCA adapter.
 */
public final class Residents {
    public static final String ID_KIND = "resident";

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, LivingKingdomsSettlements.MOD_ID);

    /** Present on the body of a resident and saved with it. There is no default: read it with {@link #idOf}. */
    private static final Supplier<AttachmentType<StableId>> RESIDENT_ID = ATTACHMENT_TYPES.register("resident_id",
            () -> AttachmentType.<StableId>builder(() -> {
                throw new IllegalStateException("resident_id has no default value; use Residents.idOf");
            }).serialize(StableId.CODEC).build());

    private Residents() {}

    public static Optional<StableId> idOf(Entity entity) {
        return entity.getExistingData(RESIDENT_ID);
    }

    /** Cheap check that needs no save data, so it is safe while an entity is still being loaded. */
    public static boolean isResident(Entity entity) {
        return entity.hasData(RESIDENT_ID);
    }

    public static Optional<ResidentRecord> recordOf(Entity entity) {
        MinecraftServer server = entity.getServer();
        if (server == null) return Optional.empty();
        return idOf(entity).flatMap(id -> SettlementSaveData.get(server).find(id));
    }

    public static Collection<ResidentRecord> all(MinecraftServer server) {
        return SettlementSaveData.get(server).all();
    }

    /** Makes the villager a resident, or returns its record if it already is one. */
    public static ResidentRecord enlist(Villager body) {
        MinecraftServer server = requireServer(body);
        Optional<ResidentRecord> existing = recordOf(body);
        if (existing.isPresent()) {
            existing.get().setName(body.getName().getString());
            SettlementSaveData.get(server).setDirty();
            return existing.get();
        }
        StableId id = CoreSaveData.get(server).next(ID_KIND);
        ResidentRecord record = new ResidentRecord(id, body.getUUID(), body.getName().getString(), Optional.empty(), 0);
        SettlementSaveData.get(server).add(record);
        body.setData(RESIDENT_ID, id);
        return record;
    }

    /** Removes the record and the link; the villager goes back to being an ordinary MCA villager. */
    public static boolean dismiss(Entity body) {
        Optional<StableId> id = idOf(body);
        if (id.isEmpty()) return false;
        MinecraftServer server = requireServer(body);
        SettlementSaveData.get(server).remove(id.get());
        body.removeData(RESIDENT_ID);
        return true;
    }

    public static void setOrder(Villager body, Optional<HarvestOrder> order) {
        ResidentRecord record = enlist(body);
        record.setOrder(order);
        SettlementSaveData.get(requireServer(body)).setDirty();
    }

    public static void recordDelivery(Entity body, int count) {
        recordOf(body).ifPresent(record -> {
            record.addDelivered(count);
            SettlementSaveData.get(requireServer(body)).setDirty();
        });
    }

    /** A dead resident's record is dropped; M4-S4 will turn this into a death in the settlement. */
    public static void onDeath(LivingDeathEvent event) {
        Entity entity = event.getEntity();
        if (!entity.level().isClientSide() && isResident(entity)) {
            idOf(entity).ifPresent(id -> SettlementSaveData.get(requireServer(entity)).remove(id));
        }
    }

    private static MinecraftServer requireServer(Entity entity) {
        MinecraftServer server = entity.getServer();
        if (server == null) throw new IllegalStateException("Residents are changed on the server only");
        return server;
    }
}
