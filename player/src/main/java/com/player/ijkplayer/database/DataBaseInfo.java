package com.player.ijkplayer.database;

/**
 *数据库表名，字段名，字段类型
 * Created by Administrator on 2015/12/29.
 */
public class DataBaseInfo {
    public static final String DATABASE_NAME = "db_block_data.db";
    public static final int DATABASE_VERSION = 1;

    private static String[][] FieldNames;
    private static String[][] FieldTypes;
    private static String[] TableNames;

    //表，字段名，字段类型
    static {

        TableNames = new String[]{
                "block",
        };

        FieldNames = new String[][] {
                {"_id", "text"},
        };

        FieldTypes = new String[][] {
                {"INTEGER PRIMARY KEY AUTOINCREMENT","VARCHAR(255) NOT NULL"},
        };
    }

    public static String[][] getFieldNames() {
        return FieldNames;
    }

    public static String[][] getFieldTypes() {
        return FieldTypes;
    }

    public static String[] getTableNames() {
        return TableNames;
    }
}
