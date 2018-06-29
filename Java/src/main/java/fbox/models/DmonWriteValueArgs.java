package fbox.models;

public class DmonWriteValueArgs {
    public DmonWriteValueArgs(String groupName, String name, String value) {
        this.name = name;
        this.value = value;
        this.groupName = groupName;
    }

    public String name;
    public int type = 0;
    public String value;
    public String groupName;
}
