package org.greatgamesonly.shared.opensource.utils.reflectionutils;

public class TestModelClass {
    public static final String CONSTANT_TEST = "test_constant_value";
    private String name;
    private String description;
    private TestSubObjectClass sub;

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public TestSubObjectClass getSub() { return this.sub; }
    public void setSub(TestSubObjectClass sub) { this.sub = sub; }
}