import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
public class TestClass {
    public static void main(String[] args) {
        Object x = GameRuleRegistry.register("test", null, null);
    }
}
