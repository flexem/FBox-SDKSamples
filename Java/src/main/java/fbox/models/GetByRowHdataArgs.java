package fbox.models;

import java.util.List;

public class GetByRowHdataArgs extends GetHdataArgs{
    public GetByRowHdataArgs(List<Long> ids, long begin, long end, int limit, int tr) {
        super(0, ids, begin, end, limit, tr);
    }
}
