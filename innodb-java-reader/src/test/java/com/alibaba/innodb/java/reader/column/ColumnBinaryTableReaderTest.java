package com.alibaba.innodb.java.reader.column;

import com.alibaba.innodb.java.reader.AbstractTest;
import com.alibaba.innodb.java.reader.TableReader;
import com.alibaba.innodb.java.reader.page.index.GenericRecord;
import com.alibaba.innodb.java.reader.schema.Column;
import com.alibaba.innodb.java.reader.schema.Schema;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author xu.zx
 */
public class ColumnBinaryTableReaderTest extends AbstractTest {

  public Schema getSchema() {
    return new Schema()
        .addColumn(new Column().setName("id").setType("int(11)").setNullable(false).setPrimaryKey(true))
        .addColumn(new Column().setName("a").setType("varbinary(32)").setNullable(false))
        .addColumn(new Column().setName("b").setType("varbinary(255)").setNullable(false))
        .addColumn(new Column().setName("c").setType("varbinary(512)").setNullable(false))
        .addColumn(new Column().setName("d").setType("binary(32)").setNullable(false))
        .addColumn(new Column().setName("e").setType("binary(255)").setNullable(false));
  }

  @Test
  public void testBinaryColumnMysql56() {
    testBinaryColumn(IBD_FILE_BASE_PATH_MYSQL56 + "column/binary/tb07.ibd");
  }

  @Test
  public void testBinaryColumnMysql57() {
    testBinaryColumn(IBD_FILE_BASE_PATH_MYSQL57 + "column/binary/tb07.ibd");
  }

  @Test
  public void testBinaryColumnMysql80() {
    testBinaryColumn(IBD_FILE_BASE_PATH_MYSQL80 + "column/binary/tb07.ibd");
  }

  public void testBinaryColumn(String path) {
    try (TableReader reader = new TableReader(path, getSchema())) {
      reader.open();

      // check queryByPageNumber
      List<GenericRecord> recordList = reader.queryByPageNumber(3);

      assertThat(recordList.size(), is(10));

      int index = 0;
      for (int i = 1; i <= 10; i++) {
        GenericRecord record = recordList.get(index++);
        Object[] values = record.getValues();
        System.out.println(Arrays.asList(values));

        assertThat(values[0], is(i));
        assertThat(record.get("a"), is(getContent((byte) (97 + i), (byte) 0x0a, 8)));
        // if len > 127 && max len <= 255，覆盖这个分支条件
        if ((i % 2) == 0) {
          assertThat(((byte[]) values[2]).length, is(255));
          assertThat(values[2], is(getContent((byte) (97 + i), (byte) 0x0b, 254)));
        } else {
          assertThat(((byte[]) values[2]).length, is(11));
          assertThat(values[2], is(getContent((byte) (97 + i), (byte) 0x0b, 10)));
        }

        assertThat(((byte[]) record.get("c")).length, is(401));
        assertThat(record.get("c"), is(getContent((byte) (97 + i), (byte) 0x0c, 400)));

        // a BINARY(255) column can exceed 768 bytes if the maximum byte length of the character set is greater than 3, as it is with utf8mb4.
        assertThat(record.get("d"), is(getContent((byte) (97 + i), (byte) 0x0a, 8, 32)));
        if ((i % 2) == 0) {
          assertThat(((byte[]) record.get("e")).length, is(255));
          assertThat(record.get("e"), is(getContent((byte) (97 + i), (byte) 0x0b, 254, 255)));
        } else {
          assertThat(((byte[]) record.get("e")).length, is(255));
          assertThat(record.get("e"), is(getContent((byte) (97 + i), (byte) 0x0b, 10, 255)));
        }
      }
    }
  }

}
