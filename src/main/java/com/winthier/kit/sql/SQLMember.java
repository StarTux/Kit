package com.winthier.kit.sql;

import com.winthier.sql.SQLRow.*;
import com.winthier.sql.SQLRow;
import java.util.Date;
import java.util.UUID;
import lombok.Data;

@Data @NotNull @Name("members")
@UniqueKey(value = {"kitId", "member"}, name = "kit_member")
@Key({"enabled", "claimed", "member"}) // Sidebar/player query
public final class SQLMember implements SQLRow {
    @Id
    private Integer id;

    private int kitId;

    private UUID member;

    @Default("0")
    private boolean enabled;

    @Default("0")
    private boolean claimed;

    @Default("NOW()")
    private Date createdTime;

    @Nullable
    private Date claimedTime;

    public SQLMember() { }

    public SQLMember(final SQLKit kit, final UUID member) {
        this.kitId = kit.getId();
        this.member = member;
        this.createdTime = new Date();
        this.enabled = kit.isEnabled();
    }
}
