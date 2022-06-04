package com.winthier.kit.sql;

import com.winthier.sql.SQLRow.*;
import com.winthier.sql.SQLRow;
import java.util.Date;
import java.util.UUID;
import lombok.Data;

@Data @NotNull @Name("claimed")
@UniqueKey({"kitId", "member"})
public final class SQLClaimed implements SQLRow {
    @Id
    private Integer id;

    private int kitId;

    @Keyed
    private UUID member;

    @Default("NOW()")
    private Date claimedTime;

    public SQLClaimed() { }

    public SQLClaimed(final SQLKit kit, final UUID member) {
        this.kitId = kit.getId();
        this.member = member;
        this.claimedTime = new Date();
    }
}
