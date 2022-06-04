package com.winthier.kit.legacy;

import java.util.Map;
import java.util.UUID;
import lombok.Data;

@Data
public final class LegacyUsers {
    protected Map<UUID, Long> cooldowns;
}
