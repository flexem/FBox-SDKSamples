package fbox.models;

public class HdataDef {

    public long uid;

    /**
     * 名称
     */
    public String name;

    /**
     * 采样时间
     */
    public int period;

    /**
     * 关联的盒子ID
     */
    public long boxId;

    /**
     * 是否使用使能设置
     */
    public boolean hasCtrl;

    /**
     * 使能控制详细配置
     */
    public HdataControlOptions ctrl;

    public HdataChannelDef[] channels;
}
