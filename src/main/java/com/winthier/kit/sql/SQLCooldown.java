package com.winthier.kit.sql;

import com.winthier.sql.SQLRow;
import com.winthier.sql.SQLRow.Key;
import com.winthier.sql.SQLRow.Name;
import com.winthier.sql.SQLRow.NotNull;
import com.winthier.sql.SQLRow.UniqueKey;
import java.util.Date;
import java.util.UUID;
import lombok.Data;

@Data @NotNull @Name("cooldowns")
@UniqueKey({"kitId", "member"})
@Key({"expiryTime", "member"}) // Sidebar/find/update query
public final class SQLCooldown implements SQLRow {
    @Id
    private Integer id;

    private int kitId;

    @Keyed
    private UUID member;

    @Default("NOW()")
    private Date claimedTime;

    private Date expiryTime;

    public SQLCooldown() { }

    public SQLCooldown(final SQLKit kit, final UUID member, final Date expiryTime) {
        this.kitId = kit.getId();
        this.member = member;
        this.claimedTime = new Date();
        this.expiryTime = expiryTime;
    }
}
