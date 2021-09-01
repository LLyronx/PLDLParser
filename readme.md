# LYRON

LYRON是一个编译器框架程序，它能够允许用户定义编程语言，并且根据这种定义来解析对应的程序代码。



### 使用文档

该使用文档面向的用户是定义特定编程语言的用户。对于这些用户来说，你只需要按照下面的方式定义你的语言，然后启动本程序解析你用来定义语言的文件，最后解析使用你定义的语言编写的程序代码。本程序最终会输出四元式。四元式是通用的源代码的中间表示，它包含了这个源代码的所有信息，你可以编写自己的后端处理四元式以生成特定平台的可执行程序。

PLDL语言语法：

1. PLDL语言使用XML编写。所以需要遵守XML的规则，需要注意的是**在XML中，大于号、小于号、与符号、单引号和双引号可能需要转义才能使用**。
2. 定义根元素 `<pldl>`   `</pldl>` 
3. 在根元素中定义文法产生式。所有的文法产生式在同一个根元素 `<cfgproductions>`  `</cfgproductions>` 下，每一个产生式写入一个 `<item>` 元素，其中定义一个production元素写入产生式本身。产生式的左部和右部用->隔开，左部只有一个非终结符，右部有若干个终结符或非终结符，**它们之间必须用空格隔开**。产生式可以推出空串，在这种情况下，产生式右部有且只有一个null。整个文法的开始符号是Program。
4. 在根元素中定义终结符的正则表达式。目前我们支持的正则表达式功能有或运算（|）、连接运算（直接连接）、克林闭包运算（*）、正闭包运算（+）、优先运算（左右小括号）、定义字符范围（左右中括号，字符间用短横线表示字符范围，或者取反运算^表示排除这些字符范围）。暂不支持其他运算符。有关这些运算符更详细的信息请查阅其他资料。像lex一样，正则表达式匹配过程是贪婪的，并且不可更改。但是你可以规定一个正则串后面不可以跟某些符号或者必须跟某些符号中的一个。综上典型的终结符正则表达式定义过程如下：

   1. 在根元素中添加 `<terminals>`  `</terminals>` 根元素，其中包含若干个item，每一个终结符是一个item。

   2. 在item中添加name元素定义这个终结符的名字。

   3. 在item中添加regex元素定义这个终结符的正则表达式。

   4. 在item中添加ban元素定义这个终结符后面不能跟哪些符号，allow元素定义这个终结符后面**只能**跟哪些符号，ban和allow元素**不能同时出现在一个item中**。两者可以都不存在，如果都不存在，默认这个终结符后面允许出现任何符号。

   5. 如果这个终结符是**平凡**的，也就是这个终结符唯一匹配它的名称的字符（以c语言为例，关键字，运算符等都属于平凡终结符），那么**这个终结符的正则表达式可以省略不写**。
5. 在根元素中定义注释的正则表达式，注释会被以与上述相同的方式识别，并且直接被词法分析器丢弃。因而注释的名称不会对注释的识别产生任何影响。

   1. 在根元素中添加 `<comments>`  `</comments>` 根元素，其中包含若干个item，每一种注释是一个item。

   2. 在item中添加name元素定义这种注释的名字。
   3. 在item中添加regex元素定义这种注释的正则表达式。
6. 定义注释分析树生成规则。这些规则在分析树生成以后被执行，用于对分析树中的每一个节点的属性赋值。赋值时可以想象一个指针首先指向整个分析树的根节点，然后通过定义的操作向下遍历或者进行属性赋值。开始时，只有终结符存在属性名称为val的属性值，这个值是词法分析得到的词法值，非终结符不存在任何属性。因而**遍历子节点和赋值的顺序至关重要**，如果顺序定义错误，某些属性可能会被赋空值，这时程序将抛出异常，用户可以根据异常检查并改正动作定义的错误。定义动作的方式如下。
   1. 首先在 `<cfgproductions>` 根元素中寻找每一个 `<item>` 元素，在item中添加元素 `<movements>` ，由于一个节点的动作可能有多个，因而在 `<movements>` 元素内定义多个 `<item>` ，每个 `<item>` 表示一个动作。
   2. 使用$$表示节点本身，使用`$$(属性名)`的方式取得本身的某一个属性。例如`$$(val)`表示当前指针指向的节点的val属性。在任何时候指针总会指向一个产生式左部表示的节点，并且执行这个产生式定义 `<movements>` 元素内定义的`<item>` ，因而$$表示这个产生式左部的节点。
   3. 使用`$num`表示节点的某个孩子节点，num是节点的孩子的索引值，**索引值从1开始，索引这条产生式右部的所有符号**。例如产生式A -> B C中用`$2(type)`表示第二个孩子也就是产生式右部的C的type属性。使用$num(属性名)的方式取得这个孩子的某一个属性。**孩子节点不能嵌套**。如果要访问孙子节点，请在孩子节点中定义动作，如果要访问父节点，请在父节点本身的产生式中定义动作。同一个产生式只能执行相同的动作。
   4. 使用赋值符号=表示将某个属性的值复制给另一个属性。后者如果不存在，则创建一个。例如`$$(val)=$1(name)`表示将第一个孩子的name属性赋值给当前节点本身的val属性，如果val属性不存在，则在当前节点中创建一个名为val的属性并赋值。
   5. 使用`go`表示指针移动到某一个孩子节点并开始遍历。go(某个孩子节点)表示遍历某个孩子节点，例如go($1)表示遍历第一个孩子节点。**遍历完成之前不会执行下一条语句**，终结符由于不出现在产生式的左部，因而不用遍历。一般认为非终结符需要遍历，否则该非终结符以及其所有的非终结符孩子节点将不会获得属性而失效。
   6. 使用`newTemp(某字符串)`获取一个包含某字符串的，**全局唯一**的字符串。你可以把它当做一个属性值，将它赋值给节点本身的一个属性或者孩子节点的某个属性。例如`$1(val)=newTemp(val)`表示将一个全局不重复且包含"val"的字符串赋值给第一个孩子节点的val属性。
   7. 使用`print(属性值)`进行调试，调试信息输出到控制台中。当你发布文法文件时，请确认所有的print已经被删除。例如`print($$(val))`会在控制台中打印当前节点的val属性，所有print的打印顺序和赋值顺序（用户定义的遍历顺序）一致。
7. 定义四元式生成规则，这些规则在语法树节点属性赋值完成后执行。也就是说整个分析树总共会被从根节点遍历两遍，第一遍遍历时根据movements为节点赋值，第二遍遍历时生成四元式。定义四元式的方式如下：

   1. 首先在 `<cfgproductions>` 根元素中寻找每一个 `<item>` 元素，在item中添加元素 `<before-generations>` 和 `<after-generations>` ，由于一个节点生成的四元式可能有多个，因而在 `<before-generations>` 、 `<after-generations>` 元素内定义多个 `<item>` ，每个 `<item>` 表示生成一个四元式。所有 `<before-generations>` 中生成四元式的动作将在遍历子节点之前执行，**与节点属性赋值不同，用户不能显式地指定节点遍历顺序**，执行 `<before-generations>` 中生成四元式的动作后，遍历所有的子节点并生成所有子节点中 `<before-generations>` 和 `<after-generations>` 中生成四元式的动作，最后执行该节点本身的 `<after-generations>` 中生成四元式的动作。
   
   2. 每一个四元式生成语句的形式都是`gen(xxx, xxx, xxx, xxx)`，括号内第一个元素一般表示语法动作，**只能是以字母开头的字母数字串**，后面三个元素可以是节点或者节点变量的属性值，取值方式与movements中一致，或者是常量字符串，或者一般用下划线_表示空，四元式具体的含义与用户具体的后端实现有关。
   
   3. 程序将边遍历边生成四元式，生成的四元式按顺序在屏幕上输出。

一个典型的语言定义文件如下所示，该文法定义了一个支持加法和乘法的计算器怎样通过一个其对应的代码文件（一个算式）生成四元式：
```xml
<pldl>
    <!--文法声明部分-->
    <cfgproductions>
        <item>
            <!--开始符号是Program-->
            <production>Program -> E</production>
            <movements>
                <!--必须先通过遍历其孩子节点获得属性-->
                <item>go($1)</item>
            </movements>
            <!--不生成任何四元式，所以不需要写generations-->
        </item>
        <item>
            <production>E -> E + T</production>
            <movements>
                <!--必须先通过遍历其两个孩子节点E和T获得属性-->
                <item>go($1)</item>
                <item>go($3)</item>
                <!--节点本身需要一个属性，值是一个新生成的临时变量-->
                <item>$$(val) = newTemp(val)</item>
            </movements>
            <!--所有子节点的四元式都生成完毕，再追加下面的四元式，所以用after-generations而不是before-generations-->
            <after-generations>
                <!--节点本身的val属性是之前的newTemp出的值-->
                <item>gen(add, $1(val), $3(val), $$(val))</item>
            </after-generations>
        </item>
        <item>
            <production>E -> T</production>
            <movements>
                <!--必须先通过遍历其孩子节点获得属性-->
                <item>go($1)</item>
                <!--将第一个子节点的val属性传递给节点本身-->
                <item>$$(val) = $1(val)</item>
            </movements>
        </item>
        <item>
            <production>T -> T * F</production>
            <movements>
                <!--必须先通过遍历其两个孩子节点获得属性-->
                <item>go($1)</item>
                <item>go($3)</item>
                <!--生成另一个临时变量，与上面不同-->
                <item>$$(val) = newTemp(val)</item>
            </movements>
            <!--所有子节点的四元式都生成完毕，再追加下面的四元式，所以用after-generations而不是before-generations-->
            <after-generations>
                <!--节点本身的val属性是之前的newTemp出的值-->
                <item>gen(multi, $1(val), $3(val), $$(val))</item>
            </after-generations>
        </item>
        <item>
            <production>T -> F</production>
            <movements>
                <item>go($1)</item>
                <item>$$(val) = $1(val)</item>
            </movements>
        </item>
        <item>
            <production>F -> num</production>
            <movements>
                <!--由于产生式右部没有非终结符，不需要写go语句-->
                <!--将第一个节点（终结符num）的词法值赋给节点本身-->
                <item>$$(val) = $1(val)</item>
            </movements>
        </item>
    </cfgproductions>
    <!--终结符词法部分-->
    <terminals>
        <!--除了num以外所有的终结符（*、+等）都是平凡的，因此只需要写num的正则表达式-->
        <item>
            <name>num</name>
            <!--终结符num的正则表达式-->
            <regex>[1-9][0-9]*|0</regex>
        </item>
    </terminals>
    <!--注释部分-->
    <comments>
        <item>
            <name>comment</name>
            <regex>/\*([^\*]|(\*)*[^\*/])*(\*)*\*/</regex>
        </item>
    </comments>
</pldl>
```

下面假设你已经定义了用于描述自己的语言的XML文件，请按照如下方式使用本程序。

1. 安装JRE，最低版本是JRE8，并将Java的可执行程序添加到环境变量。

2. 在[Releases](https://gitee.com/llyronx/LYRON/releases)中下载最新版本的包含可执行jar包的zip文件，解压文件。

3. 编写自己定义的语言的程序代码。

4. 在命令行中输入`java -jar xxxx.jar`，`xxxx.jar`表示解压的根目录下jar包的路径。

5. 第一次运行时输入xml文件路径，保存模型文件(`model`)，然后再次运行，直接输入模型文件路径和代码文件路径，模型文件即对应这个语言的定义，这样可以在一定程度上加速程序的运行。

6. 解析成功会在屏幕上输出四元式，如果出现错误，请查看错误提示并修改。

以下是本程序根据上述示例中定义的语言解析代码 `3 + 4 * 5 + 6` 的运行过程和结果：其中`test.code`文件的内容是`3 + 4 * 5 + 6`

第一次运行：
```shell
欢迎使用LYRON！
请选择程序执行方式
1.输入xml文件，生成程序定义模型文件（model）
2.输入model文件和对应的代码文件，生成四元式文件（4tu）
1
请输入xml文件路径
test.xml
请输入模型文件保存路径
test.model
XML文件解析中...
XML文件解析成功。
正在构建词法分析器...
词法分析器构建成功。
正在构建语法分析器...
表项共9*10项
基于LR（1）分析的语法分析器构建成功。
特定语言类型的内部编译器架构形成。
保存模型中……
保存模型成功
```

第二次运行：
```shell
欢迎使用LYRON！
请选择程序执行方式
1.输入xml文件，生成程序定义模型文件（model）
2.输入model文件和对应的代码文件，生成四元式文件（4tu）
2
请输入模型文件路径
test.model
请输入代码文件路径
test.code
请输入四元式保存路径
test.4tu
正在读取代码文件...
正在对代码进行词法分析...
正在对代码进行语法分析构建分析树...
正在对分析树进行语义赋值生成注释分析树...
正在根据注释分析树生成四元式...
生成四元式成功
生成完毕。
```

生成的`test.4tu`的内容如下：

```shell
multi, 4, 5, t_val0
add, 3, t_val0, t_val1
add, t_val1, 6, t_val2
```

### 开发文档

该文档面向需要修改编译器框架本身的用户，这部分用户可以修改本工程代码，修复bug或提供不同功能的其他实现。例如，用户可以通过继承lexer.RE类实现自己的正则表达式引擎。

1. 首先下载并安装JetBrains Intellij IDEA。通过Intellij导入本项目为maven项目，等待加载maven包完毕。
   
2. src文件夹中可以查看和修改源代码。
   
3. 使用maven的package功能生成jar包，使用install功能安装jar包。target文件夹中将生成编译好的文件。

4. 如果需要查看与本项目相关联的c后端项目相关代码，请使用git submodule功能，然后你将在sample-c-backend看到与sample-xml/c.xml定义的文法相关的后端的java项目。
   
