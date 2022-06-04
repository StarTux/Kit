package com.winthier.kit.legacy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Data;

/**
 * (De)serializable with Json.
 */
@Data
public final class LegacyKit {
    protected String name = "";
    protected String displayName; // Gson-serialized component
    protected List<String> messages = new ArrayList<>();
    protected String permission = "";
    protected List<LegacyKitItem> items = new ArrayList<>();
    protected long cooldown = -1;
    protected boolean hidden = false;
    protected List<String> commands = new ArrayList<>();
    protected List<String> titles = List.of();
    protected Map<UUID, String> members = new HashMap<>();
    protected List<String> description = new ArrayList<>();
    protected int friendship = 0;
    protected long date = 0L;
}
