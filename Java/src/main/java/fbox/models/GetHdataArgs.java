package fbox.models;

import java.util.List;

public class GetHdataArgs {
    /**
     * 通道ID列表
     */
    public List<Long> ids;
    /**
     * 开始时间（1970-1-1以来的毫秒数）
     */
    public long begin;
    /**
     * 结束时间（1970-1-1以来的毫秒数）
     */
    public long end;
    /**
     * 取值最大个数
     * 正数表示从开始时间向结束时间取最多limit个，负值表示从结束时间向开始时间最多取limit个
     */
    public int limit;
    /**
     * 时间范围边界类型
     * 0:全开区间, 1:左开右闭, 2: 左闭右开, 3:全闭区间
     */
    public int tr;

    public int type;

    public GetHdataArgs(int type, List<Long> ids, long begin, long end, int limit, int tr) {
        this.ids = ids;
        this.begin = begin;
        this.end = end;
        this.limit = limit;
        this.tr = tr;
        this.type = type;
    }
}
