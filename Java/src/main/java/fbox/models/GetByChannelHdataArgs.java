package fbox.models;

import java.util.List;


public class GetByChannelHdataArgs extends GetHdataArgs{
    public GetByChannelHdataArgs(List<Long> ids, long begin, long end, int limit, int tr) {
        super(1, ids, begin, end, limit, tr);
    }
}

