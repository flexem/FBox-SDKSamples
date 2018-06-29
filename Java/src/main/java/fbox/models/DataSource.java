package fbox.models;

public class DataSource {
    /**
     * PLC别名
     */
    public String devAlias;

    /**
     * 站号
     */
    public int stationNo;

    /**
     * 数据类型
     */
    public int dataType;

    /**
     * 寄存器ID（与寄存器位宽联合使用）
     */
    public int regId;

    /**
     * 寄存器位宽 (与寄存器ID联合使用)
     */
    public int ioWidth;
    /// <summary>
    ///     寄存器名称
    /// </summary>
    public String regName;
    /// <summary>
    ///     主地址
    /// </summary>
    public int addr;
    /// <summary>
    ///     子地址
    /// </summary>
    public int subAddr;
    /// <summary>
    ///     数据块(DB块)
    /// </summary>
    public int addrBlk;
    /// <summary>
    /// 是否启用按位索引
    /// </summary>
    public boolean bitIndexEnabled;
    /// <summary>
    /// 按位索引号
    /// </summary>
    public int bitIndex;

}
