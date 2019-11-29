# PlotGUI
区画に新規プレイヤーが簡単に入居できる &amp; アクティブでない区画を自動でパージできるようにするプラグイン

## 前提
* ワールドガードを必要とする。
* プレイヤーが入居した状態は、ワールドガードの仕様によるオーナーになっているのではない
* ただこのプラグインにオーナーとして記録されているだけでワールドガード上ではオーナーもメンバーになる
* コマンドは1つもない
* 区画として提供する領域を最初から保護しておく必要がある

## 使い方
* 保護の中に看板を立てる。または保護の外側一ブロックに、保護に背を向けた看板を立てる
* 看板の1行目に`[PlotGUI]`と書いて完了
* 看板を2回右クリックするとオーナーになれる。オーナーになった状態でさらに看板を右クリックするとGUIが開く
* すでにオーナーがー居る保護は、オーナーまたは`plotgui.mod`の権限があるプレイヤーのみGUIを開ける
* オーナー以外で権限のないプレイヤーが看板を右クリックするとオーナーの名前が表示される
* オーナーがGUIからできる操作は
  * 保護メンバー追加
  * 保護メンバー削除
  * オーナー状態の譲渡
  * 保護内の再生成
  * 保護の放棄（再生成を伴う）
* `config.yml`で指定した日数以上ログインしていないプレイヤーの区画はサーバー起動時に強制放棄・再生成される（WIP）

## 権限
* `plotgui.*`
  * 説明: 全権
  * デフォルト権限: `op`
  * 子権限
    * `plotgui.mod` (`true`)
    * `plotgui.sign.remove` (`true`)
    * `plotgui.sign.place` (`true`)
* `plotgui.mod`
  * 説明: 他の人の区画でも管理GUIを開ける権限
  * デフォルト権限: `op`
* `plotgui.sign.remove`
  * 説明: 看板を破壊して区画を削除する権限
  * デフォルト権限: `op`
* `plotgui.sign.place`
  * 説明: 看板を設置して区画を作成する権限
  * デフォルト権限: `op`