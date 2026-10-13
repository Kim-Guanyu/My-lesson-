# JB1-3 面向对象 · 第3单元 OOP继承机制

> 课程：JB1-3-面向对象（讲师：刘能）｜单元：OOP继承机制（共 2 课时）
> 本单元讲继承：为什么要继承、继承的规则限制、以及 super 关键字的三种用法。

---

## 3.1 继承基本原则

**本课时对应知识点**：继承的意义、继承的原则。

**继承是什么**。用 `extends` 关键字让一个类（子类 / 派生类）获得另一个类（父类 / 基类 / 超类）的属性和方法。

```java
// 父类
public class Animal {
    protected String name;
    public void eat() { System.out.println(name + "在吃东西"); }
}

// 子类：自动拥有 name 属性和 eat() 方法
public class Dog extends Animal {
    public void bark() { System.out.println("汪汪"); }
}

Dog dog = new Dog();
dog.name = "豆豆";
dog.eat();          // ✅ 继承来的方法
dog.bark();         // ✅ 自己的方法
```

**继承的意义（三点）**：

**① 代码复用** —— 共同的属性和方法只写一次：

```java
// ❌ 不用继承：大量重复
class Student { String name; int age; void eat(){} void sleep(){} void study(){} }
class Teacher { String name; int age; void eat(){} void sleep(){} void teach(){} }

// ✅ 用继承：共性抽到父类
class Person { String name; int age; void eat(){} void sleep(){} }
class Student extends Person { void study(){} }
class Teacher extends Person { void teach(){} }
```

**② 建立层次关系** —— 表达「is-a」（是一个）的语义。学生**是一个**人，狗**是一个**动物。

**③ 支持多态** —— 继承是多态的前提（见第 4 单元）。父类引用可以指向子类对象，这是框架设计的基石。

**继承的核心原则（本课时重点）**：

**① Java 只支持单继承** —— 一个类**只能有一个直接父类**：

```java
class A { }
class B { }
class C extends A, B { }        // ❌ 编译错误：Java 不支持多继承
```

**为什么禁止多继承**：避免「菱形继承问题」（钻石问题）—— 如果 B 和 C 都继承自 A 并各自重写了 `method()`，那么同时继承 B 和 C 的 D 该用哪个版本？C++ 允许多继承因此有这个歧义，Java 通过「单继承 + 多实现接口」的方案规避了它。

**② 支持多层继承（继承链）**：

```java
class A { }
class B extends A { }
class C extends B { }           // ✅ C 间接拥有 A 的成员
```

**继承层次建议不超过 3 层**。层次太深会导致「继承地狱」—— 改父类一个方法，不知道会影响多少个子类。阿里巴巴开发手册也建议谨慎使用深层继承。

**③ 一个父类可以有多个子类**：

```java
class Animal { }
class Dog extends Animal { }
class Cat extends Animal { }
class Bird extends Animal { }   // ✅ 完全可以
```

**④ 所有类都隐式继承 `Object`**：

```java
class A { }
// 等价于 class A extends Object { }
```

所以任何对象都有这些方法：`toString()`、`equals()`、`hashCode()`、`getClass()`、`clone()`、`wait()`、`notify()`、`finalize()`（已废弃）。

**⑤ 子类能继承什么、不能继承什么（重要）**：

| 成员 | 是否继承 |
|---|---|
| `public` 属性 / 方法 | ✅ |
| `protected` 属性 / 方法 | ✅ |
| 友元（默认）成员 | ⚠️ **仅同包时可继承** |
| `private` 属性 / 方法 | ❌ **不可继承**（子类中不可见） |
| 静态成员 | ⚠️ 可以访问，但**不是「继承」而是「共享」**，且不能被重写 |
| **构造器** | ❌ **不被继承**（但可以通过 `super()` 调用） |

**关于 private 成员的准确理解**：private 属性**实际存在于子类对象的内存中**（因为子类对象包含完整的父类部分），但子类代码**无法直接访问**它，只能通过父类提供的 public getter/setter 间接访问：

```java
class Parent {
    private String secret = "私密";
    public String getSecret() { return secret; }     // 提供访问通道
}

class Child extends Parent {
    public void show() {
        // System.out.println(secret);         // ❌ 不可见
        System.out.println(getSecret());       // ✅ 通过继承来的 public 方法访问
    }
}
```

**⑥ `final` 类不能被继承**：

```java
public final class String { }
class MyString extends String { }     // ❌ 编译错误
```

JDK 中 `String`、`Integer`、`Math`、`System` 等都是 final 类，目的是保证行为的确定性和安全性（如果能继承 String 并重写方法，字符串常量池和安全校验都会失效）。

**继承的初始化顺序**（见第 2 单元 2.6 课时）：

```java
class Parent {
    static { System.out.println("① 父类静态块"); }
    { System.out.println("③ 父类实例块"); }
    Parent() { System.out.println("④ 父类构造器"); }
}

class Child extends Parent {
    static { System.out.println("② 子类静态块"); }
    { System.out.println("⑤ 子类实例块"); }
    Child() { System.out.println("⑥ 子类构造器"); }
}

new Child();
// 输出：① ② ③ ④ ⑤ ⑥
```

**记忆口诀**：**静态先于实例，父类先于子类**。

**继承 vs 组合（重要的设计取舍）**：

```java
// 继承：is-a（是一个）
class Dog extends Animal { }              // 狗是一个动物 ✅

// 组合：has-a（有一个）
class Car {
    private Engine engine;                 // 汽车有一个引擎 ✅
}
```

**「组合优于继承」是业界公认的设计原则**。原因：

| | 继承 | 组合 |
|---|---|---|
| 耦合度 | **高** —— 父类改动直接影响所有子类 | 低 —— 只依赖接口 |
| 灵活性 | 编译期确定，不可变 | **运行时可替换** |
| 破坏封装 | 子类依赖父类实现细节 | 不破坏 |
| 数量限制 | 只能单继承 | 可以组合任意多个 |

**判断标准**：**只有真正的「是一个」关系才用继承**。经典反例：让 `Stack extends ArrayList` —— 栈只允许在一端操作，但继承 ArrayList 后暴露了 `add(index, e)` 这类破坏栈语义的方法。正确做法是让 Stack **组合**一个 ArrayList。

Java 集合框架本身就有这个历史包袱：`java.util.Stack extends Vector` 是公认的设计失误，现在推荐用 `Deque` 代替。

---

## 3.2 父子类间调用

**本课时对应知识点**：`super.属性`、`super.方法()`、`super()`。

**`super` 是什么**。`super` 代表**父类的引用**，用于在子类中访问父类的成员。它有三种用法。

### 用法一：`super.属性` —— 访问父类属性

主要用于**解决属性同名遮蔽（shadowing）**：

```java
class Parent {
    protected String name = "父类的name";
}

class Child extends Parent {
    private String name = "子类的name";       // 与父类同名，遮蔽了父类的

    public void show() {
        System.out.println(name);              // 子类的name
        System.out.println(this.name);         // 子类的name
        System.out.println(super.name);        // 父类的name  ← 用 super 访问
    }
}
```

**实践建议：不要让子类属性与父类同名**。属性遮蔽极易造成混淆，且不像方法重写那样有明确的语义。这个特性主要是为了应对面试题和读懂遗留代码。

### 用法二：`super.方法()` —— 调用父类方法

**最主要的用途是在重写方法时「扩展」而不是「替换」父类行为**：

```java
class Parent {
    public void save() {
        System.out.println("父类：基础保存逻辑");
    }
}

class Child extends Parent {
    @Override
    public void save() {
        super.save();                    // ← 先执行父类逻辑
        System.out.println("子类：额外的日志记录");   // 再加自己的
    }
}
```

**这是模板扩展的常见模式**。如果不调 `super.save()`，父类逻辑就完全被替换掉了。

**另一个用途：重写后仍需访问父类版本**：

```java
class Child extends Parent {
    @Override
    public String toString() {
        return "Child{" + super.toString() + ", extra=" + extra + "}";
    }
}
```

### 用法三：`super()` —— 调用父类构造器

**这是 `super` 最重要、最容易考的用法**。

**核心规则：子类构造器必须（直接或间接）调用父类构造器**。因为子类对象包含完整的父类部分，父类的字段必须先被初始化。

```java
class Parent {
    private String name;
    public Parent(String name) {
        this.name = name;
        System.out.println("父类构造器");
    }
}

class Child extends Parent {
    private int age;
    public Child(String name, int age) {
        super(name);              // ← 必须显式调用，且必须在第一行
        this.age = age;
        System.out.println("子类构造器");
    }
}
```

**四条必须记住的规则**：

**① 如果不写 `super()`，编译器会自动插入无参的 `super()`**：

```java
class Child extends Parent {
    public Child() {
        // 编译器在这里自动插入 super();
    }
}
```

**② 父类没有无参构造器时，子类必须显式调用带参的 `super(...)`**：

```java
class Parent {
    public Parent(String name) { }        // 只有带参构造器
}

class Child extends Parent {
    public Child() { }                    // ❌ 编译错误：找不到 Parent() 无参构造器
}

class Child2 extends Parent {
    public Child2() { super("默认名"); }   // ✅ 显式调用带参构造器
}
```

**这是初学者极其高频的报错**。报错信息类似 `There is no default constructor available in 'Parent'`。解决办法二选一：在父类补一个无参构造器，或在子类显式调用 `super(...)`。

**③ `super()` 必须是构造器的第一条语句**：

```java
public Child(String name) {
    System.out.println("先做点别的");     // ❌ 编译错误
    super(name);
}
```

原因：父类必须先完成初始化，否则子类代码可能访问到未初始化的父类字段。

**④ `super()` 和 `this()` 不能同时出现**（两者都要求在第一行，无法共存）：

```java
public Child(String name) {
    this();               // 调用本类其他构造器
    super(name);          // ❌ 编译错误
}
```

**间接调用的正确写法**：

```java
class Child extends Parent {
    public Child() {
        this("默认名");            // 调用本类另一个构造器
    }
    public Child(String name) {
        super(name);              // 由这个构造器负责调用父类
    }
}
```

这样 `super()` 是被**间接**调用的，符合规则。

**`this` 与 `super` 的完整对比**：

| | `this` | `super` |
|---|---|---|
| 指代 | **当前对象** | **父类部分** |
| `.属性` | 访问本类属性 | 访问父类属性 |
| `.方法()` | 调用本类方法 | 调用父类方法 |
| `()` 构造器 | 调用**本类**其他构造器 | 调用**父类**构造器 |
| 位置要求 | `this()` 必须在第一行 | `super()` 必须在第一行 |
| 静态方法中 | ❌ 不能用 | ❌ 不能用 |

**`this` 的其他常见用途**：

```java
class User {
    private String name;

    // ① 区分同名的成员变量和形参（最常见）
    public void setName(String name) {
        this.name = name;         // 不写 this 就变成 name = name，自赋值无效
    }

    // ② 返回当前对象，支持链式调用（Builder 模式）
    public User name(String name) {
        this.name = name;
        return this;
    }

    // ③ 把自己作为参数传出去
    public void register() {
        UserManager.add(this);
    }
}
```

**继承中的访问权限约束（重写时的规则，预告第 4 单元）**：子类重写方法的访问权限**不能比父类更严格**：

```java
class Parent {
    public void method() { }
}

class Child extends Parent {
    protected void method() { }    // ❌ 编译错误：不能降低可见性
    public void method() { }       // ✅ 相同
}
```

**原因**：多态调用时用父类引用调用该方法，如果子类把它变成了 private，调用就会失败 —— 这违反了「子类必须能替代父类」的里氏替换原则。
