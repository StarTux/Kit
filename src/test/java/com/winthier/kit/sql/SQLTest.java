package com.winthier.kit.sql;

import com.winthier.sql.SQLDatabase;
import org.junit.Test;

public final class SQLTest {
    @Test
    public void main() {
        System.out.println(SQLDatabase.testTableCreation(SQLKit.class));
        System.out.println(SQLDatabase.testTableCreation(SQLMember.class));
        System.out.println(SQLDatabase.testTableCreation(SQLCooldown.class));
        System.out.println(SQLDatabase.testTableCreation(SQLClaimed.class));
    }
}
