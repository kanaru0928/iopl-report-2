# 【課題B】LRパーザジェネレータの実装

## LR構文解析の形式化 [[大堀 '14]](https://doi.org/10.11309/jssst.31.1_30)

* 解説記事 [[大堀 '14]](https://doi.org/10.11309/jssst.31.1_30) の中で，後述の実装の説明に必要な部分を説明．

### アイディア

LR 構文解析法を用いて、与えられた入力文字列が LR(0) 文法を満たすかを確認するパーザジェネレータを実装する。

文脈自由文法 $G = (N, T, P, S)$ を考える。ただし、$N$ を非終端文字の集合、$T$ を終端文字の集合、$P$ を生成規則の集合、$S \in N$ を開始記号とする。

この文法に対して、入力文字列 $w \in T^*$ がどのように還元されるかを計算したい。特に、入力文字列を左から順に読んでいき、部分的に還元可能な部分文字列を見つけたら、それを非終端記号に置き換えていくことで、最終的に開始記号 $S$ に還元できるかどうかを判定したい。これは、以下のような還元系列を構成する。

$$
w_1w_2\cdots w_n \Leftarrow \beta_1A_1w_2w_3\cdots w_n \Leftarrow \beta_1\beta_2A_2w_3\cdots w_n \Leftarrow \cdots \Leftarrow S
$$

ここで、$\beta_i$ は終端記号の列、$A_i$ は非終端記号である。これは、最右導出の逆順である。

ここで、次の定理（LR 構文解析の基本原理 (Knuth)）が成り立つ。ただし、$\stackrel{\Rightarrow}{\tiny{rm}}$ は最右導出を表す。

> 分文脈自由文法 $G$ に対して、$C_G = \{\alpha \beta \mid S \stackrel{*}{\stackrel{\Rightarrow}{\tiny{rm}}} \alpha A w \stackrel{\Rightarrow}{\tiny{rm}} \alpha \beta w\}$ は正規言語である。
>
> $C_G$ を受理する FA $N_G = (Q, \Sigma, \delta, q_0, F)$ は次のように構成される。
>
> $Q = \{s\} \cup \{[A \to \alpha \cdot \beta] \mid A \to \alpha \beta \in P\},$
>
> $\Sigma = T \cup N,$
>
> $F = \{[A \to \alpha \cdot] \mid A \to \alpha \in P\},$
>
> $\delta : Q \times (\Sigma \cup \{\varepsilon\}) \to 2^Q,$ は関数であり、以下で定義される。
>
> $\delta(s, \varepsilon) = \{[S \to \cdot \alpha] \mid S \to \alpha \in P\},$
>
> $\delta([A \to \alpha \cdot v \beta], a) = \{[A \to \alpha v \cdot \beta]\},$
>
> $\delta([A \to \alpha \cdot B \beta], \varepsilon) = \{[B \to \cdot \gamma] \mid B \to \gamma \in P\}.$
>
> ただし、$Q$ は状態集合、$\Sigma$ は入力アルファベット、$\delta$ は状態遷移関数、$q_0$ は初期状態、$F$ は受理状態の集合、$\alpha, \beta, \gamma$ は終端記号の列、$A, B$ は非終端記号、$v$ は終端記号または非終端記号である。

よって、$C_G$ を受理する有限オートマトン（以下 FA）を構成し、文字列を入力して受理状態に到達した段階で停止することで、最右導出の逆順で 1 ステップ還元できる。

### アルゴリズムの効率化

アイディアに基づくと、定理に従って構成された FA を何度も入力文字列に適用することになるが、1 回の還元で変化する入力文字列はごく一部である。よって、似たような状態遷移を何度も計算することになる。そこで、状態遷移をスタックで管理し、還元時にその文字数だけスタックを巻き戻すことで、効率的に入力文字列の還元を行うことができる。

### 文法の拡張

文法 $G$ に対して、$G' = (N', T', P', S)$ を次のように定義する。

$$
N' = N \cup \{S'\}, \quad T' = T \cup \{\$\}, \quad P' = P \cup \{S' \to S\$\}
$$

ここで、$S'$ は生成規則の右辺に現れない新しい開始記号であり、$\$$ は生成規則の左辺に現れない新しい終端記号である。入力文字列の末尾に $\$$ を付加することで、入力文字列の終端を明示的に表すことができる。

### ACTION 表

定理に従って構成された FA は Subset Construction によって DFA に変換することができる。構成された DFA を $D_G$ とする。$D_G$ の状態は、$N_G$ の状態の集合、すなわち $[A \to \alpha \cdot \beta]$ の集合である。ここで、文法 $G$ が LR(0) 文法であるとき、$D_G$ の状態は、還元項（$[B \to \cdot \gamma]$）を高々 1 つしか含まない。

よって、各状態においてやるべきこと（アクション）は以下の 2 つに分類できる。

1. 還元項を含まない場合は入力文字列の先頭の終端記号を読み取り状態遷移する
2. 還元項を含む場合は還元して状態遷移を元に戻す

これらのアクションを 1 をシフト（**shift**）、2 を還元（**reduce**）と呼び分ける。シフトは次に遷移する状態、還元はどの規則で還元するかも保持する。そして、状態 $[S' \to S \cdot \$]$ にたどり着いた時は、入力文字列が文法 $G$ に従っていることを意味する。これを **accept** と設定する。

これらを状態と入力文字列に対するアクションを表でまとめたものを ACTION 表と呼ぶ。

### 上記アイディアを踏まえた構文解析アルゴリズム

1. スタックに初期状態 $q_0$ をプッシュする
2. 入力の先頭を現在の記号 $x$ とする
3. スタックから現在の状態 $q$ をポップする
4. ACTION 表に従って、状態 $q$ と記号 $x$ に対するアクションを決定する
    - **shift** の場合は、
        1. $x$ をスタックにプッシュする
        2. 遷移先の状態 $q'$ をスタックにプッシュする
        3. 入力の先頭を次の記号に更新する
    - **reduce** の場合は、
        1. 還元する規則 $A \to \beta$ の右辺の長さ $|\beta| - 1$ だけスタックをポップする
        2. 還元する規則の左辺 $A$ をスタックにプッシュする
    - **accept** の場合は、構文解析の成功を報告し、処理を終了する
    - 対応するアクションがない場合は、構文解析の失敗を報告し、処理を終了する
5. ステップ 3 に戻る


## 解説記事と実装との対応

* 解説記事の説明（形式化）と実装との対応を具体的に説明．

Scala 言語で実装した。

### FA

FA は [NFA.scala](src/main/scala/lr0pg/NFA.scala) と [DFA.scala](src/main/scala/lr0pg/DFA.scala) に実装されている。[fromNFA](src/main/scala/lr0pg/DFA.scala#L35) メソッドでは Subset Construction により ε-NFA を DFA に変換することができる。

まず、[purgeEpsilonTransition](src/main/scala/lr0pg/NFA.scala#L49) メソッドで ε 遷移を除去する。その後、幅優先探索で状態を走査する形で Subset Construction を行う（[DFA.scala](src/main/scala/lr0pg/DFA.scala#L44-L60)）。

### LR(0) 文法

LR(0) 文法は [LR0.scala](src/main/scala/lr0pg/LR0.scala) 内で `LR0` クラスとして実装されている。

`LR0` クラスのインスタンスが作成された時点で、上記定理に従い DFA を構成している（[mkFA](src/main/scala/lr0pg/LR0.scala#L40) メソッド）。また、その DFA の状態に還元項が複数含まれないことを確認し、LR(0) 文法であることを検証している（[LR0.scala](src/main/scala/lr0pg/LR0.scala#L200-L213)）。

上記で説明した文法の拡張は [normalize](src/main/scala/lr0pg/LR0.scala#L95) メソッドで実装した。

ACTION 表の構成は [actionTable](src/main/scala/lr0pg/LR0.scala#L154) プロパティで実装した。

### 構文解析アルゴリズム

構文解析アルゴリズムは [Parser.scala](src/main/scala/lr0pg/Parser.scala) 内で `Parser` クラスとして実装されている。[parse](src/main/scala/lr0pg/Parser.scala#L9) メソッドでは上記アルゴリズムを愚直に実装している。
