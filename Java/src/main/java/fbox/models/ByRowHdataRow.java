package fbox.models;

import java.util.Date;

public class ByRowHdataRow {
    /**
     * 时间戳（1970-1-1以来的毫秒数）
     */
    public long t;

    public Date getTime (){
        return new Date(t);
    }

    /**
     * 一行中每个单元格的值
     */
    public Object[] c;
}
