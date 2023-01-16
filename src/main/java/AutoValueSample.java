
public class AutoValueSample {
    public static void main(String[] args) {
        ValueType object1 = new ValueType(5,10);
        ValueType object2 = new ValueType(5,10);
        System.out.println("Object1 == Object2 " + (object1.equals(object2)));
        System.out.println("Object1 Hash = " + object1.hashCode());
        System.out.println("Object2 Hash = " + object2.hashCode());
    }
}


