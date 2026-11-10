package livingkingdoms.settlements.resident;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.UUID;
import livingkingdoms.core.StableId;
import livingkingdoms.settlements.work.HarvestOrder;
import net.minecraft.core.UUIDUtil;

/**
 * Everything our mod knows about one resident. The record is the resident; the MCA entity is its
 * body and may be unloaded (ADR-0030). Changed only through {@link Residents}, on the server thread.
 */
public final class ResidentRecord {
    static final Codec<ResidentRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            StableId.CODEC.fieldOf("id").forGetter(ResidentRecord::id),
            UUIDUtil.CODEC.fieldOf("entity").forGetter(ResidentRecord::entity),
            Codec.STRING.fieldOf("name").forGetter(ResidentRecord::name),
            HarvestOrder.CODEC.optionalFieldOf("order").forGetter(ResidentRecord::order),
            Codec.INT.optionalFieldOf("delivered", 0).forGetter(ResidentRecord::delivered)
    ).apply(instance, ResidentRecord::new));

    private final StableId id;
    private final UUID entity;
    private String name;
    private Optional<HarvestOrder> order;
    private int delivered;

    ResidentRecord(StableId id, UUID entity, String name, Optional<HarvestOrder> order, int delivered) {
        this.id = id;
        this.entity = entity;
        this.name = name;
        this.order = order;
        this.delivered = delivered;
    }

    public StableId id() {
        return id;
    }

    /** UUID of the MCA entity, the key into MCA's own data. */
    public UUID entity() {
        return entity;
    }

    /** The resident's name when the body was last seen. */
    public String name() {
        return name;
    }

    public Optional<HarvestOrder> order() {
        return order;
    }

    /** Items delivered to storage so far. */
    public int delivered() {
        return delivered;
    }

    void setName(String name) {
        this.name = name;
    }

    void setOrder(Optional<HarvestOrder> order) {
        this.order = order;
    }

    void addDelivered(int count) {
        this.delivered += count;
    }
}
