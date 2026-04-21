# peach-gateway

Peach API 网关（Spring Cloud Gateway），依赖 `peach-dependencies` BOM。

> 文档创作日期：2026-04-20，作者：leiyangjun

---

## Git 远程（一次推送，双端同步）

本仓库 **`master`** 为默认分支（与 Gitee 一致；GitHub 若新建仓库默认为 `main`，请在 GitHub 仓库 **Settings → Default branch** 改为 **`master`**，或首次推送后改默认分支）。

| 用途 | 地址 |
|------|------|
| **fetch** | `git@github.com:leiyangjun/peach-gateway.git` |
| **push** | 同上 + `git@gitee.com:leiyangjun/peach-gateway.git` |

```bash
git add .
git commit -m "your message"
git push -u origin master
```

之后可直接 `git push`（会依次推送到 GitHub 与 Gitee）。

核对：`git remote -v`、`git config --get-all remote.origin.pushurl`。
