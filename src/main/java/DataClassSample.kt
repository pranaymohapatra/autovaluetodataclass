data class ValueTypeKotlin (val mA: Int, val mB: Int)


fun main() {
    val object1 = ValueTypeKotlin(5, 10)
    val object2 = ValueTypeKotlin(5, 10)
    println("Object1 == Object2 " + (object1 == object2))
    println("Object1 Hash = " + object1.hashCode())
    println("Object2 Hash = " + object2.hashCode())
}