# Jumper
A simple fragment-router tool for TV client.

Jumper 是一个基于单 Activity 多 Fragments 架构的、适用于 Tv 客户端的页面导航工具，具体来说，它提供了下述能力：

1）以一个 Activity 作为容器，将 Fragment 作为单页面，提供了常规的页面跳转与传参功能；
2）妥善地处理了按键事件的传递，因此能够像使用 Activity 一样，在 Fragment 中处理 KeyEvent；
3）Fragment 层层叠加或者逐层回退时，能够妥善地处理焦点记忆与传递问题；
4）提供了类似 Activity 启动模式的调用形式，能够达到清除 Fragment 栈或者多级回退的效果；
5）提供了栈顶 Fragment 变化时的监听。

主要的功能在 Jumper 类中实现，在使用时还需要辅以 MainActivity 和 BaseFragment 来使用。

大致的实现原理如下：
- 在单例类 Jumper 中，始终维护着一些基本信息，包括 fragment 栈、记录的焦点列表等；
- 在进行添加页面或移除页面的操作时维护好栈和焦点等信息，以便在将来返回到某个页面的时候恢复焦点状态。

下面对其基本的使用方式和几个特殊的接口进行简要介绍。

Jumper 提供了链式调用的语法，比如，若要在 MainActivity 中打开一个新的 FirstFragment 页面，只用如下几行代码即可实现：
```Java
Jumper.init(activity)
    .target(FirstFragment())
    .hidePreviousActivity(rootView)
    .giveBackFocus(focus)
    .addToWindowRoot(null)
```
其中，``hidePreviousActivity`` 的作用是打开新的 Fragment 页面后将根 Activity 的布局隐藏起来；``giveBackFocus`` 的作用是记录下前一个页面最后的焦点，待将来返回该页面时，焦点将自动落回这里指定的 focus.

再比如，我们要从一级 Fragment 页面，跳转到一个二级 Fragment，类比如上写法，可通过如下代码实现：
```Java
Jumper.init(activity)
    .target(SecondFragment())
    .hidePreviousFragment(rootView)
    .giveBackFocus(focus)
    .addBundleData("param" to "hello, this is a message from first page")
    .addToWindowRoot(null)
```
这里使用的 ``hidePreviousFragment`` 方法，与 ``hidePreviousActivity`` 类似，其作用是记录下前一个 fragment，以便将来页面回退时进行恢复；``addBundleData`` 的作用是传递参数.

上述两个例子是最简单的页面跳转，项目中我们往往会面临更为复杂的页面导航情景。

比如，有这样的页面前进路径：A -> B -> C, 但是回退时，需要从 C 中直接返回到页面 A。Jumper 亦提供了相应的接口来实现这样的情形。在页面 B 跳转到 C 时，使用下面的代码即可达到如上需求：
```Java
Jumper.init(activity)
    .target(C())
    .transfer(b)
    .addToWindowRoot(null)
```
这里 ``transfer`` 接口的作用是将 b 页面记录的焦点、页面路径等信息，“传递”给将要跳转的页面 C，并且将 b 自身从 Jumper 记录的 Fragment 栈中移除。这一系列调用结束后，看上去就好像是 C 取代了 b 的位置一样，因此在从 C 返回时，便会直接返回到页面 A，并且能够妥善地处理好焦点记忆和页面恢复的问题，达到了上面的需求。

此外，Jumper 还提供了 ``backStack(level: Int)`` 这样一个特殊的方法，其作用是从当前页面回退一定的页面的层级；而 ``clearTop2Main`` 则是其一个特殊调用，作用是清空所有打开的 Fragment 页面，直接回到主页面。有了上述这些接口，便可基本实现常见的页面跳转。

总的来说，Jumper 的实现原理较为简单，目前提供的接口还比较有限，但基本能够实现常见的页面跳转，若是将来有新的需求，“见招拆招”就是。

> - contributor：handsomeyang, mao, corey
> - writer：corey

