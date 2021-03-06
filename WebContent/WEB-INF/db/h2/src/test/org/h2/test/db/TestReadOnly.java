/*
 * Copyright 2004-2011 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.test.db;

import java.io.File;
import java.io.RandomAccessFile;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import org.h2.constant.ErrorCode;
import org.h2.store.FileLister;
import org.h2.store.fs.FileSystem;
import org.h2.test.TestBase;

/**
 * Test for the read-only database feature.
 */
public class TestReadOnly extends TestBase {

    /**
     * Run just this test.
     *
     * @param a ignored
     */
    public static void main(String... a) throws Exception {
        TestBase.createCaller().init().test();
    }

    public void test() throws Exception {
        if (config.memory) {
            return;
        }
        testReadOnlyTempTableResult();
        testReadOnlyConnect();
        testReadOnlyDbCreate();
        if (!config.googleAppEngine) {
            testReadOnlyFiles(true);
        }
        testReadOnlyFiles(false);
        deleteDb("readonly");
    }

    private void testReadOnlyTempTableResult() throws SQLException {
        deleteDb("readonly");
        Connection conn = getConnection("readonly");
        Statement stat = conn.createStatement();
        stat.execute("CREATE TABLE TEST(ID INT) AS SELECT X FROM SYSTEM_RANGE(1, 20)");
        conn.close();
        conn = getConnection("readonly;ACCESS_MODE_DATA=r;MAX_MEMORY_ROWS_DISTINCT=10");
        stat = conn.createStatement();
        stat.execute("SELECT DISTINCT ID FROM TEST");
        conn.close();
    }

    private void testReadOnlyDbCreate() throws SQLException {
        deleteDb("readonly");
        Connection conn = getConnection("readonly");
        conn.close();
        conn = getConnection("readonly;ACCESS_MODE_DATA=r");
        Statement stat = conn.createStatement();
        assertThrows(ErrorCode.DATABASE_IS_READ_ONLY, stat).
                execute("CREATE TABLE TEST(ID INT)");
        assertThrows(ErrorCode.TABLE_OR_VIEW_NOT_FOUND_1, stat).
                execute("SELECT * FROM TEST");
        stat.execute("create local temporary linked table test(" +
                "null, 'jdbc:h2:mem:test3', 'sa', 'sa', 'INFORMATION_SCHEMA.TABLES')");
        ResultSet rs = stat.executeQuery("select * from test");
        assertTrue(rs.next());
        conn.close();
    }

    private void testReadOnlyFiles(boolean setReadOnly) throws Exception {
        new File(System.getProperty("java.io.tmpdir")).mkdirs();
        File f = File.createTempFile("test", "temp");
        assertTrue(f.canWrite());
        f.setReadOnly();
        assertTrue(!f.canWrite());
        f.delete();

        f = File.createTempFile("test", "temp");
        RandomAccessFile r = new RandomAccessFile(f, "rw");
        r.write(1);
        f.setReadOnly();
        r.close();
        assertTrue(!f.canWrite());
        f.delete();

        deleteDb("readonly");
        Connection conn = getConnection("readonly");
        Statement stat = conn.createStatement();
        stat.execute("CREATE TABLE TEST(ID INT PRIMARY KEY, NAME VARCHAR)");
        stat.execute("INSERT INTO TEST VALUES(1, 'Hello')");
        stat.execute("INSERT INTO TEST VALUES(2, 'World')");
        assertTrue(!conn.isReadOnly());
        conn.close();

        if (setReadOnly) {
            setReadOnly();
            conn = getConnection("readonly");
        } else {
            conn = getConnection("readonly;ACCESS_MODE_DATA=r");
        }
        assertTrue(conn.isReadOnly());
        stat = conn.createStatement();
        stat.execute("SELECT * FROM TEST");
        assertThrows(ErrorCode.DATABASE_IS_READ_ONLY, stat).
                execute("DELETE FROM TEST");
        conn.close();

        if (setReadOnly) {
            conn = getConnection("readonly;DB_CLOSE_DELAY=1");
        } else {
            conn = getConnection("readonly;DB_CLOSE_DELAY=1;ACCESS_MODE_DATA=r");
        }
        stat = conn.createStatement();
        stat.execute("SELECT * FROM TEST");
        assertThrows(ErrorCode.DATABASE_IS_READ_ONLY, stat).
                execute("DELETE FROM TEST");
        stat.execute("SET DB_CLOSE_DELAY=0");
        conn.close();
    }

    private void setReadOnly() {
        FileSystem fs = FileSystem.getInstance(getBaseDir());
        ArrayList<String> list = FileLister.getDatabaseFiles(getBaseDir(), "readonly", true);
        for (String fileName : list) {
            fs.setReadOnly(fileName);
        }
    }

    private void testReadOnlyConnect() throws SQLException {
        deleteDb("readonly");
        Connection conn = getConnection("readonly;OPEN_NEW=TRUE");
        Statement stat = conn.createStatement();
        stat.execute("create table test(id identity)");
        stat.execute("insert into test select x from system_range(1, 11)");
        assertThrows(ErrorCode.DATABASE_ALREADY_OPEN_1, this).
                getConnection("readonly;ACCESS_MODE_DATA=r;OPEN_NEW=TRUE");
        conn.close();
        deleteDb("readonly");
    }

}
