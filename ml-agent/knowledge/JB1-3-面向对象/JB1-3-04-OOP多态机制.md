# JB1-3 面向对象 · 第4单元 OOP多态机制

> 课程：JB1-3-面向对象（讲师：刘能）｜单元：OOP多态机制（共 2 课时）
> 本单元讲多态：重载与重写的区别、动态绑定原理、向上向下转型、instanceof。

---

## 4.1 多态具体实现

**本课时对应知识点**：方法重载 overload、方法重写 override。

**多态是什么**。多态（Polymorphism）= 多种形态，指**同一个行为在不同情况下表现出不同的结果**。Java 中多态通过两种机制实现：

| | **重载 Overload** | **重写 Override** |
|---|---|---|
| 别名 | 编译时多态、静态多态 | **运行时多态、动态多态** |
| 发生位置 | **同一个类**中 | **父子类**之间 |
| 方法名 | 相同 | 相同 |
| 参数列表 | **必须不同** | **必须相同** |
| 返回值 | 可以不同 | 相同或是其**子类型**（协变返回） |
| 访问权限 | 无限制 | **不能比父类更严格** |
| 抛出异常 | 无限制 | **不能比父类更宽泛** |
| 绑定时机 | **编译期**（看引用类型） | **运行期**（看实际对象） |

### 方法重载（Overload）

**同一个类中，方法名相同、参数列表不同**：

```java
public class Calculator {
    public int add(int a, int b) { return a + b; }
    public double add(double a, double b) { return a + b; }      // 类型不同
    public int add(int a, int b, int c) { return a + b + c; }    // 个数不同
    public void print(String s, int i) { }
    public void print(int i, String s) { }                       // 顺序不同
}
```

**判定标准只看参数列表（类型、个数、顺序）**。以下都不构成重载：

```java
public int add(int a, int b) { return 0; }
public double add(int a, int b) { return 0; }     // ❌ 仅返回值不同，编译错误
private int add(int a, int b) { return 0; }       // ❌ 仅权限不同，编译错误
public int add(int x, int y) { return 0; }        // ❌ 仅参数名不同，编译错误
```

**重载是「编译时多态」**：编译器根据**实参的类型**在编译期就决定调用哪个版本，字节码里写死了目标方法。

**重载的匹配优先级（面试题常考）**。编译器按以下顺序寻找最匹配的方法：

```
① 精确匹配（类型完全相同）
② 自动类型提升（int → long → float → double）
③ 自动装箱（int → Integer）
④ 装箱后向上转型（int → Integer → Object）
⑤ 可变参数（int... ）
```

```java
public void test(int a)       { System.out.println("int"); }
public void test(long a)      { System.out.println("long"); }
public void test(Integer a)   { System.out.println("Integer"); }
public void test(Object a)    { System.out.println("Object"); }
public void test(int... a)    { System.out.println("varargs"); }

test(1);      // 输出 "int"      ← 精确匹配优先
// 如果注释掉 test(int)，输出 "long"（自动提升优先于装箱）
// 再注释掉 test(long)，输出 "Integer"（装箱优先于 Object）
// 可变参数优先级最低
```

**重载的实际应用**：JDK 里到处都是。`System.out.println()` 有十几个重载版本（int、double、char、String、Object……），构造器重载也是最常见的场景。

### 方法重写（Override）

**子类重新定义父类已有的方法**，方法签名必须完全相同：

```java
class Animal {
    public void speak() { System.out.println("动物在叫"); }
}

class Dog extends Animal {
    @Override                        // 建议总是加这个注解
    public void speak() { System.out.println("汪汪汪"); }
}

class Cat extends Animal {
    @Override
    public void speak() { System.out.println("喵喵喵"); }
}
```

**多态调用的效果**：

```java
Animal a1 = new Dog();      // 父类引用指向子类对象
Animal a2 = new Cat();

a1.speak();                 // 汪汪汪  ← 调用的是 Dog 的实现
a2.speak();                 // 喵喵喵  ← 调用的是 Cat 的实现
```

**同一行代码 `a.speak()`，执行结果取决于 `a` 实际指向什么对象** —— 这就是运行时多态。

**重写的「两同两小一大」规则**：

- **两同**：方法名相同、参数列表相同
- **两小**：子类返回值类型**相同或更小**（协变返回，可以是父类返回类型的子类）；子类抛出的异常**相同或更小**（不能抛出比父类更宽泛的受检异常）
- **一大**：子类的访问权限**相同或更大**（不能比父类更严格）

```java
class Parent {
    protected Animal getAnimal() throws IOException { return null; }
}

class Child extends Parent {
    @Override
    public Dog getAnimal() throws FileNotFoundException { return null; }
    // ✅ 权限变大 protected → public
    // ✅ 返回值变小 Animal → Dog（协变返回，JDK 5+ 支持）
    // ✅ 异常变小 IOException → FileNotFoundException
}
```

**不能被重写的方法**：

| 情况 | 说明 |
|---|---|
| `private` 方法 | 子类根本看不见，写同名方法只是**新方法**，不是重写 |
| `static` 方法 | 属于类而非对象，同名只是**隐藏（hide）**，不是重写 |
| `final` 方法 | 语法上禁止重写 |
| 构造器 | 不参与继承，只能重载 |

**`@Override` 注解的价值**。它不是必需的，但**强烈建议加上**：

```java
class Dog extends Animal {
    @Override
    public void speek() { }        // ❌ 编译报错！拼写错了，编译器帮你发现
}
```

不加注解的话，拼错方法名会被当作「新增方法」而静默通过，多态调用时永远走父类实现，这种 bug 极难排查。

**静态方法「隐藏」而非「重写」的验证（重要考点）**：

```java
class Parent {
    public static void staticMethod() { System.out.println("父类静态"); }
    public void instanceMethod() { System.out.println("父类实例"); }
}

class Child extends Parent {
    public static void staticMethod() { System.out.println("子类静态"); }
    @Override
    public void instanceMethod() { System.out.println("子类实例"); }
}

Parent p = new Child();
p.staticMethod();       // 父类静态  ❗ 看的是引用类型（编译期绑定）
p.instanceMethod();     // 子类实例  ✅ 看的是实际对象（运行期绑定）
```

**结论：静态方法没有多态**。它在编译期按引用类型确定，与实际对象无关。**属性也是同样的规则**（属性没有多态）：

```java
class Parent { String name = "父"; }
class Child extends Parent { String name = "子"; }

Parent p = new Child();
System.out.println(p.name);     // "父"  ❗ 属性按引用类型访问
```

**记忆结论：只有「实例方法」才有多态；静态方法和属性都是编译期按引用类型确定的**。

---

## 4.2 动态绑定机制

**本课时对应知识点**：`instanceof`、子类的实例可以用父类类型接收。

**多态的三个必备条件**：

1. **有继承（或实现接口）关系**
2. **子类重写了父类方法**
3. **父类引用指向子类对象**（向上转型）

三者缺一不可。

### 向上转型（Upcasting）

**子类对象赋值给父类引用，自动完成，绝对安全**：

```java
Animal a = new Dog();          // 向上转型，隐式完成
Animal[] animals = { new Dog(), new Cat(), new Bird() };
List<Animal> list = new ArrayList<>();
list.add(new Dog());
```

**向上转型后的限制**：只能调用**父类中声明过的**方法：

```java
Animal a = new Dog();
a.speak();          // ✅ Animal 中有 speak()，实际执行 Dog 的实现
a.bark();           // ❌ 编译错误：Animal 类型中没有 bark() 方法
```

**编译器只认引用类型（编译期看左边），JVM 只认实际对象（运行期看右边）**。这句话是理解多态的钥匙。

**向上转型的价值 —— 面向抽象编程**：

```java
// ❌ 不用多态：每加一种动物就要加一个方法
public void feedDog(Dog dog) { dog.eat(); }
public void feedCat(Cat cat) { cat.eat(); }

// ✅ 用多态：一个方法处理所有动物，新增动物无需改代码
public void feed(Animal animal) { animal.eat(); }

feed(new Dog());
feed(new Cat());
feed(new Bird());      // 新增类型，这个方法完全不用改
```

**这就是「开闭原则」（对扩展开放，对修改关闭）的实现方式**，也是所有框架设计的核心。JDBC 的 `Connection`、Spring 的 `BeanFactory`、集合的 `List` 都是这个思路 —— 你面向接口写代码，具体实现可以随时替换。

```java
// 面向接口编程的日常写法
List<String> list = new ArrayList<>();       // 想换成 LinkedList 只改这一处
Map<String, Object> map = new HashMap<>();
```

### 向下转型（Downcasting）

**父类引用转回子类类型，必须显式强转，且有风险**：

```java
Animal a = new Dog();
Dog d = (Dog) a;               // ✅ 向下转型，实际对象确实是 Dog
d.bark();                      // 现在可以调用子类特有方法了

Animal a2 = new Cat();
Dog d2 = (Dog) a2;             // ❌ 运行时抛 ClassCastException
```

**编译能通过，运行才报错** —— 这是向下转型的危险之处。

### instanceof 关键字

**用于在向下转型前判断实际类型，避免 ClassCastException**：

```java
Animal a = new Cat();

if (a instanceof Dog) {
    Dog d = (Dog) a;
    d.bark();
} else if (a instanceof Cat) {
    Cat c = (Cat) a;
    c.climb();
}
```

**instanceof 的规则**：

```java
Dog dog = new Dog();
System.out.println(dog instanceof Dog);       // true
System.out.println(dog instanceof Animal);    // true  ← 父类也返回 true
System.out.println(dog instanceof Object);    // true  ← 万物皆 Object

Animal a = null;
System.out.println(a instanceof Animal);      // false ← null 永远返回 false（不会抛异常）
```

**`null instanceof X` 恒为 false**，这个特性使 instanceof 天然空安全，可以用它同时完成判空和判类型。

**模式匹配（Pattern Matching for instanceof，JDK 16+ 正式特性）**。可以在判断的同时完成转型和变量绑定：

```java
// 老写法：判断 + 强转 + 声明变量，三步
if (a instanceof Dog) {
    Dog d = (Dog) a;
    d.bark();
}

// 新写法（JDK 16+）：一步完成
if (a instanceof Dog d) {
    d.bark();                  // d 直接可用，编译器自动完成转型
}

// 还能配合逻辑运算符
if (a instanceof Dog d && d.getAge() > 3) { ... }
```

**JDK 17 / 21 / 25 项目应优先使用这种写法**，更简洁也更安全。本平台（知堂课堂）用的是 Java 17，完全支持。

**`instanceof` 与 `getClass()` 的区别**：

```java
Dog dog = new Dog();
System.out.println(dog instanceof Animal);              // true  （包含子类）
System.out.println(dog.getClass() == Animal.class);     // false （必须精确相同）
System.out.println(dog.getClass() == Dog.class);        // true
```

`equals()` 方法里通常用 `getClass() != o.getClass()` 而不是 `instanceof`，因为 equals 要求严格的类型对称性。

### 动态绑定的原理

**静态绑定 vs 动态绑定**：

| | 静态绑定（早绑定） | 动态绑定（晚绑定） |
|---|---|---|
| 时机 | **编译期** | **运行期** |
| 依据 | **引用类型** | **实际对象类型** |
| 适用 | 静态方法、私有方法、final 方法、构造器、**属性** | **普通实例方法** |

**底层实现：虚方法表（vtable）**。JVM 为每个类维护一张方法表，记录该类所有可调用方法的实际入口地址。子类重写父类方法时，表中对应槽位被替换为子类的实现。调用 `a.speak()` 时，JVM 执行 `invokevirtual` 指令：

1. 从 `a` 指向的对象头中取出它的实际类型信息
2. 在该类型的虚方法表中查找 `speak()` 的入口地址
3. 跳转执行

**因为查表发生在运行时，所以能"认出"实际对象是 Dog 还是 Cat**。这就是多态的实现机制。

可以用 jclasslib 插件（见 JB1-1 第 3 单元）查看字节码，观察 `invokevirtual`（虚方法调用，动态绑定）与 `invokestatic`（静态方法，静态绑定）、`invokespecial`（构造器、私有方法、super 调用，静态绑定）的区别。

**动态绑定的经典陷阱：构造器中调用可重写方法**：

```java
class Parent {
    public Parent() {
        init();               // ❗ 危险：调用了可被重写的方法
    }
    public void init() { System.out.println("父类 init"); }
}

class Child extends Parent {
    private String name = "初始值";

    @Override
    public void init() {
        System.out.println("子类 init, name = " + name);
    }
}

new Child();
// 输出：子类 init, name = null    ❗ name 还没初始化！
```

**原因**：父类构造器先执行（见第 3 单元初始化顺序），此时子类的字段还没初始化完成，但动态绑定已经把调用派发到了子类的 `init()`。

**结论：绝对不要在构造器中调用可被重写的方法**。要么把方法声明为 `final` 或 `private`，要么把初始化逻辑移出构造器。

**多态的实际应用示例**：

```java
// 支付方式的多态设计
interface Payment {
    void pay(BigDecimal amount);
}

class AlipayPayment implements Payment {
    public void pay(BigDecimal amount) { /* 支付宝逻辑 */ }
}
class WechatPayment implements Payment {
    public void pay(BigDecimal amount) { /* 微信逻辑 */ }
}

// 业务代码完全不关心是哪种支付方式
public class OrderService {
    public void checkout(Order order, Payment payment) {
        payment.pay(order.getTotal());       // 多态调用
    }
}
```

新增一种支付方式（如银联），只需新增一个实现类，`OrderService` 一行都不用改。这正是多态在企业开发中最主要的价值 —— **用统一的调用方式应对不断变化的具体实现**。
